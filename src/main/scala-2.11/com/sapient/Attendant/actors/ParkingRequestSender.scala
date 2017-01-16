package com.sapient.Attendant.actors

import akka.actor.{Actor, ActorLogging}
import com.google.gson.Gson
import com.sapient.Attendant.actors.ParkingRequestSender.PostParkingRequest
import com.sapient.parkingAssistant.domain.Vehicle
import com.sapient.parkingAssistant.kafkaConnectors.MessageSender

/**
  * Created by agu225 on 12/1/2017.
  */
object ParkingRequestSender{
  case class PostParkingRequest(vehicle: Vehicle)
}
class ParkingRequestSender extends Actor with ActorLogging{
  val messageSender = new MessageSender
  val parkingRequestTopic = "ParkingRequest"
  override def receive = {
    case PostParkingRequest(vehicle) =>{
      messageSender.publishMessage(parkingRequestTopic,(new Gson).toJson(vehicle))
    }
  }
}
