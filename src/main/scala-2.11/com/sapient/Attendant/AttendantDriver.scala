package com.sapient.Attendant

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import com.sapient.Attendant.actors.{Attendant, ParkingRequestSender, ParkingResponseConsumer}
import com.sapient.Attendant.actors.Attendant.RequestParking
import com.sapient.Attendant.actors.ParkingResponseConsumer.ReadParkingResponse
import com.sapient.parkingAssistant.domain.Vehicle

import scala.concurrent.duration.DurationInt

/**
  * Created by agu225 on 12/1/2017.
  */

object AttendantDriver{
  implicit val timeout = Timeout(60 seconds)
  val system = ActorSystem("Attendant")
  val requestPoster = system.actorOf(Props(new ParkingRequestSender),"ParkingRequestPoster")
  val attendant = system.actorOf(Props(new Attendant(requestPoster)),"Attendant")
  val responseReader = system.actorOf(Props(new ParkingResponseConsumer(attendant)),"ParkingResponseReader")

  def main(args: Array[String]): Unit = {
    responseReader ! ReadParkingResponse
    val attendantDriver = new AttendantDriver
    attendantDriver.submitVehicleRequests(15)
  }
}
class AttendantDriver {
  def submitVehicleRequests(vehicleCount:Int): Unit ={
    for (count <- 0 until vehicleCount) {
      Thread.sleep(500)
      AttendantDriver.attendant ! RequestParking(new Vehicle("Vehicle"+count))
    }
  }
}
