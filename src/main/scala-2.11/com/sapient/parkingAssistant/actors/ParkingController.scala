package com.sapient.parkingAssistant.actors

import java.util
import java.util.Properties

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.routing.FromConfig
import akka.util.Timeout
import com.google.gson.Gson
import com.sapient.parkingAssistant.actors.ParkingController._
import com.sapient.parkingAssistant.actors.ResponsePoster.postResponse
import com.sapient.parkingAssistant.actors.SecurityMonitor.SecurityCheck
import com.sapient.parkingAssistant.actors.SlotMonitor.{ParkVehicle, RequestDeallocateSlot}
import com.sapient.parkingAssistant.domain.{Vehicle, VehicleStatus}
import kafka.consumer.ConsumerConfig

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationLong
import scala.io.Source
import scala.util.{Failure, Success}

/**
  * Created by agu225 on 9/1/2017.
  */
object ParkingController {
  implicit val timeout = Timeout(6000 seconds)
  val system = ActorSystem("ParkingAssistant")
  val router = system.actorOf(FromConfig.props(), "scatterGatherRouter")
  val responsePoster = system.actorOf(Props(new ResponsePoster),"ResponsePoster")
  val slotActor = system.actorOf(Props(new SlotMonitor(router)), "SlotMonitor")
  val securityActor = system.actorOf(Props[SecurityMonitor], "securityActor")

  private def createConsumerConfig(zookeeper: String, groupId: String): ConsumerConfig
  = {
    val props = new Properties()
    props.put("zookeeper.connect", zookeeper)
    props.put("group.id", groupId)
    props.put("auto.offset.reset", "smallest")
    props.put("zookeeper.session.timeout.ms", "500")
    props.put("zookeeper.sync.time.ms", "250")
    props.put("auto.commit.interval.ms", "1000")
    new ConsumerConfig(props)
  }

}

class ParkingController(zookeeper: String, groupId: String, val topic: String) {
  val slotResult: Option[VehicleStatus] = None
  val securityResult: Option[VehicleStatus] = None
  val consumer = kafka.consumer.Consumer.createJavaConsumerConnector(createConsumerConfig(zookeeper, groupId))

  def startVehicleConsumer(): Unit = {
    val topicMap = new util.HashMap[String, Integer]()
    topicMap.put(topic, 1)
    val consumerStreamsMap = consumer.createMessageStreams(topicMap)
    val streamList = consumerStreamsMap.get(topic)
    for (stream <- streamList; aStream <- stream) {
      val jsonVehicle = new String(aStream.message())
      val vehicle = (new Gson).fromJson(jsonVehicle, classOf[Vehicle])
      val futureSlotStatus = slotActor ? ParkVehicle(vehicle)
      val futureSecurityStatus = securityActor ? SecurityCheck(vehicle)
      val parkingStatusFuture = for {
        slotStatusResult <- futureSlotStatus
        securityStatusResult <- futureSecurityStatus
      } yield (slotStatusResult, securityStatusResult)
      parkingStatusFuture onComplete {
        case Success(parkingResult:Tuple2[VehicleStatus, VehicleStatus]) =>
          if (parkingResult._1.flag == true && parkingResult._2.flag == true) {
            responsePoster ! postResponse((new Gson()).toJson(parkingResult._1))
          }
          else {
            if (parkingResult._2.flag == false) {
              //security failed
              responsePoster ! postResponse((new Gson()).toJson(parkingResult._2))
              slotActor ! RequestDeallocateSlot(parkingResult._1)
            } else {      //parking full
              responsePoster ! postResponse((new Gson()).toJson(parkingResult._1))
            }
          }
        case Failure(ex:Exception) =>
          println("Failed due to --->" + ex)
      }
      parkingStatusFuture onFailure {
        case ex: RuntimeException =>
          println("Error for vehicle : " + new String((new Gson()).toJson(slotResult.get)))
        case _ => "Something Wrong!"
      }
    }
  }
}

object MyParkingApp {
  def main(args: Array[String]): Unit = {
    val properties = new Properties()
    val url = getClass.getResource("/parkingconfig.properties")
    if (url != null) {
      val source = Source.fromURL(url)
      properties.load(source.bufferedReader())
    }
    val zookeeperConsumer = properties.getProperty("zookeeper")
    val groupConsumer = properties.getProperty("ParkingRequestConsumerGroup")
    val topicConsumer = properties.getProperty("ParkingRequestTopic")
    val parkingController = new ParkingController(zookeeperConsumer, groupConsumer, topicConsumer)
    parkingController.startVehicleConsumer()
  }
}
