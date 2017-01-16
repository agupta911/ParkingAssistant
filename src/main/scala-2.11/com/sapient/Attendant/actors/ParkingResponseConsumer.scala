package com.sapient.Attendant.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.google.gson.Gson
import com.sapient.Attendant.actors.Attendant.ResponseParking
import com.sapient.Attendant.actors.ParkingResponseConsumer.ReadParkingResponse
import com.sapient.Attendant.kafkaConnectors.MessageReceiver
import com.sapient.parkingAssistant.domain.VehicleStatus

import scala.collection.JavaConversions._


/**
  * Created by agu225 on 12/1/2017.
  */
object ParkingResponseConsumer{
  case class ReadParkingResponse()
}
class ParkingResponseConsumer(attendant: ActorRef) extends Actor with ActorLogging{
  val zooKeeper = "localhost:2181"
  val groupId ="Group1"
  val topic ="ParkingResponse"
  override def receive = {
    case ReadParkingResponse =>
      log.info("Starting Parking Response Listener.")
      val parkingResponseConsumer = new MessageReceiver(zooKeeper, groupId, topic)
      val streamList = parkingResponseConsumer.startConsumer
      for (stream <- streamList; aStream <- stream) {
        val jsonVehicle = new String(aStream.message())
        val vehicleStatus = (new Gson).fromJson(jsonVehicle, classOf[VehicleStatus])
        attendant ! ResponseParking(vehicleStatus)
      }
  }
}
