package com.sapient.Attendant.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.sapient.Attendant.actors.Attendant.{RequestParking, ResponseParking}
import com.sapient.Attendant.actors.ParkingRequestSender.PostParkingRequest
import com.sapient.parkingAssistant.domain.{Vehicle, VehicleStatus}

/**
  * Created by agu225 on 12/1/2017.
  */
class Attendant(parkingRequestPoster: ActorRef) extends Actor with ActorLogging {
  override def receive = {
    case RequestParking(vehicle)=>
      log.info(s"${vehicle.regNumber} arrived for parking. Requesting parking.")
      parkingRequestPoster ! PostParkingRequest(vehicle)
    case ResponseParking(vehicleStatus)=>
      if (vehicleStatus.flag==true)
        log.info(vehicleStatus.vehicle.regNumber + " parked at " + vehicleStatus.message)
      else if(vehicleStatus.message.contains("Full"))
        log.info(vehicleStatus.vehicle.regNumber + " cannot enter because Parking is full.")
      else
        log.info(vehicleStatus.vehicle.regNumber + " not allowed to enter due to security reasons!" )
  }
}

object Attendant{
  case class RequestParking(vehicle:Vehicle)
  case class ResponseParking(vehicleStatus: VehicleStatus)
}