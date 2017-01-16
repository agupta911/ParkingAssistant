package com.sapient.parkingAssistant.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.sapient.parkingAssistant.actors.FloorMonitor.{AllocateParkingSlot, CheckparkingFull, DeallocateSlot}
import com.sapient.parkingAssistant.actors.SlotMonitor.FreeSlot
import com.sapient.parkingAssistant.domain.{ParkingSlot, Vehicle, VehicleStatus}
class FloorMonitor(floorNumber: String) extends Actor with ActorLogging {

  val slotMap = scala.collection.mutable.Map[Int, Boolean](1 -> false, 2 -> false, 3 -> false, 4 -> false, 5 -> false)

  override def receive = {
    case AllocateParkingSlot(vehicle,originalSender) => {
      val emptySlots = slotMap.filter((p) => p._2 == false)
      if(emptySlots.size!=0){
        val (key, value) = emptySlots.head
        val parkingSlot = new ParkingSlot(floorNumber, key, value)
        sender ! FreeSlot(vehicle , parkingSlot , originalSender)
      }
      else{
        log.info(s"Parking full on floor- $floorNumber")
        Thread.sleep(1000)
        //val parkingSlot = new ParkingSlot(floorNumber, -1, false)
        //sender ! FreeSlot(vehicle , parkingSlot , originalSender)
      }
    }

    case FloorMonitor.ParkTheCar(vehicle, parkingSlot) => {
      val currentSlotStatus = slotMap.get(parkingSlot.slotNumber)
      if (currentSlotStatus.isDefined && !currentSlotStatus.get) {
          slotMap.put(parkingSlot.slotNumber, true)
          log.info(s"Slot No ${parkingSlot.slotNumber} on ${parkingSlot.floorNumber} for ${vehicle.regNumber}")
          sender ! VehicleStatus(vehicle, true, s"${parkingSlot.floorNumber},${parkingSlot.slotNumber}")
      } else {
        sender ! VehicleStatus(vehicle, false,"Slot is not empty" )
      }
    }

    case DeallocateSlot(slotNumber: Int) => {
      slotMap.put(slotNumber, false)
    }

    case CheckparkingFull =>{
      val emptySlots = slotMap.filter((p) => p._2 == false)
      sender ! emptySlots.size.equals(0)
    }
  }
}

object FloorMonitor {

  case class AllocateParkingSlot(vehicle:Vehicle , orignalSender : ActorRef)

  case class ParkTheCar(vehicle: Vehicle, parkingSlot: ParkingSlot)

  case class DeallocateSlot(slotNumber: Int)

  case class CheckparkingFull()
}
