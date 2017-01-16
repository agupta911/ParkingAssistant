package com.sapient.parkingAssistant.actors

import akka.actor.{Actor, ActorLogging}
import com.sapient.parkingAssistant.actors.SecurityMonitor.SecurityCheck
import com.sapient.parkingAssistant.domain.{Vehicle, VehicleStatus}

/**
  * Created by agu225 on 9/1/2017.
  */
object SecurityMonitor {
  case class SecurityCheck(vehicle : Vehicle)
}

class SecurityMonitor extends Actor with ActorLogging  {
  val blacklistedVehicleList = List("Vehicle1")
  def receive = {
    case SecurityCheck(vehicle : Vehicle) => {
      //log info (s"Security Check request received for Vehicle: ${vehicle.regNumber}")
      println(vehicle.regNumber +" requested for security check!")
      if(blacklistedVehicleList.contains(vehicle.regNumber)){
        log.info("Securtiy of " + vehicle.regNumber +" failed!")
        val securityStatus:VehicleStatus = VehicleStatus(vehicle,false,"Security Check Failed")
        sender ! securityStatus
      }else{
        val securityStatus:VehicleStatus = VehicleStatus(vehicle,true,"Security OK")
        log.info("Securtiy of " + vehicle.regNumber +" passed!")
        sender ! securityStatus
      }
    }
  }
}

