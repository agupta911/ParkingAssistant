package com.sapient.parkingAssistant.exceptions

import com.sapient.parkingAssistant.domain.VehicleStatus

/**
  * Created by agu225 on 11/1/2017.
  */
class SecurityFailException(vehicleStatus: VehicleStatus) extends
  RuntimeException(){
  def getVehicleStatus = vehicleStatus
}

object SecurityFailException {
  def defaultMessage(vehicleStatus: VehicleStatus, cause: Throwable) = {

  if (vehicleStatus.message != null) vehicleStatus.message
  else if (cause != null) cause.toString()
  else null
}
}