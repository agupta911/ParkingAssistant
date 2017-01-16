package com.sapient.parkingAssistant.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.sapient.parkingAssistant.actors.FloorMonitor.{AllocateParkingSlot, CheckparkingFull, DeallocateSlot, ParkTheCar}
import com.sapient.parkingAssistant.actors.SlotMonitor._
import com.sapient.parkingAssistant.domain.{ParkingSlot, Vehicle, VehicleStatus}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

/**
  * Created by Shiva on 9/1/2017.
  * */

class SlotMonitor(router: ActorRef) extends Actor with ActorLogging {
  implicit val timeout = Timeout(1200 seconds)
  val floor1: ActorRef = context.actorOf(Props(new FloorMonitor("floor1")), "floor1")
  val floor2: ActorRef = context.actorOf(Props(new FloorMonitor("floor2")), "floor2")

  override def receive = {

    case ParkVehicle(vehicle) => {
      log.info(s"Parking request arrived for vehicle ${vehicle.regNumber}")
      self ! CheckParkingFullOnFloors(vehicle, sender())
      router ! AllocateParkingSlot(vehicle, sender())
    }

    case FreeSlot(vehicle, parkingSlot, originalSender) => {
      log.info(s"Found the free slot -> $parkingSlot for vehicle -> ${vehicle.regNumber}" )
      val floorNumber = parkingSlot.floorNumber

      if (floorNumber.equals("floor2")) {
        val futureRes: Future[VehicleStatus] = (floor2 ? ParkTheCar(vehicle, parkingSlot)).mapTo[VehicleStatus].map{
          status =>
            if(!status.flag){
              throw new IllegalArgumentException
            }else{
              status
            }
        }

        futureRes onComplete {
          case Success(status) => {
            originalSender ! status
          }
          case Failure(status) => {
            self ! RetryParking(vehicle ,originalSender)
          }
        }
      } else {
        val futureRes: Future[VehicleStatus] = (floor1 ? ParkTheCar(vehicle, parkingSlot)).mapTo[VehicleStatus].map{
          status =>
            if(!status.flag){
              throw new IllegalArgumentException
            }else{
              status
            }
        }
        futureRes onComplete {
          case Success(status) => {
            originalSender ! status
          }
          case Failure(status) => {
           self ! RetryParking(vehicle ,originalSender)
          }
        }
      }
    }

    case RequestDeallocateSlot(vehicleStatus: VehicleStatus) => {
      val info = vehicleStatus.message.split(",")
      val floorNumber = info(0)
      val slotNumber = info(1)
      log.info(s"Deallocating Slot No $slotNumber on $floorNumber")
      if (floorNumber.equals("floor1")) {
        floor1 ! DeallocateSlot(slotNumber.toInt)
      } else {
        floor2 ! DeallocateSlot(slotNumber.toInt)
      }
    }

    case RetryParking(vehicle,originalSender) => {
      self ! CheckParkingFullOnFloors(vehicle, sender())
      router ! AllocateParkingSlot(vehicle, sender())
    }

    case CheckParkingFullOnFloors(vehicle , originalSender) => {
      val floorStatus = for {
        floor1status <- floor1 ? CheckparkingFull
        floor2status <- floor2 ? CheckparkingFull
      } yield (floor1status , floor2status )
      floorStatus onSuccess {
        case (floorStatus:Tuple2[Boolean,Boolean]) =>
          if(floorStatus._1 && floorStatus._2){
            log.info("No parking space left on any of the floors!")
            originalSender ! new VehicleStatus(vehicle,false,"Parking Full")
          }

      }
    }
  }
}

object SlotMonitor {

  case class ParkVehicle(vehicle: Vehicle)

  case class FreeSlot(vehicle: Vehicle, parkingSlot: ParkingSlot, sender: ActorRef)

  case class RequestDeallocateSlot(vehicleStatus: VehicleStatus)

  case class CheckParkingFullOnFloors(vehicle: Vehicle,sender: ActorRef)

  case class RetryParking(vehicle: Vehicle,sender: ActorRef)
}