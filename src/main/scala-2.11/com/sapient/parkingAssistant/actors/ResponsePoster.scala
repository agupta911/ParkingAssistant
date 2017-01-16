package com.sapient.parkingAssistant.actors

import java.util.Properties

import akka.actor.{Actor, ActorLogging}
import com.sapient.parkingAssistant.actors.ResponsePoster.postResponse
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.io.Source

/**
  * Created by agu225 on 11/1/2017.
  */

object ResponsePoster {

  case class postResponse(message: String)

  private var producer: KafkaProducer[String, String] = _
  val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092") //broker addresses
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("request.required.acks", "1")
  producer = new KafkaProducer(props)

  val properties = new Properties()
  val url = getClass.getResource("/parkingconfig.properties")
  if (url != null) {
    val source = Source.fromURL(url)
    properties.load(source.bufferedReader())
  }
}

class ResponsePoster extends Actor with ActorLogging {

  def publishMessage(topic: String, message: String): Unit = {
    ResponsePoster.producer.send(new ProducerRecord(topic, message))
  }

  def sendResponseMessage(message: String) = {
    val topic = ResponsePoster.properties.getProperty("ParkingResponseTopic")
    publishMessage(topic, message)
  }

  override def receive = {
    case postResponse(message) =>
      sendResponseMessage(message)
      log.info(s"Posting response to Kafka -->$message")
  }
}
