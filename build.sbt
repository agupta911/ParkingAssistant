name := "ParkingAssistant"

version := "1.0"

scalaVersion := "2.11.8"

val akkaVersion = "2.4.11"

libraryDependencies ++= Seq(
  "org.apache.kafka" %% "kafka" % "0.10.1.0"
)
libraryDependencies += "com.google.code.gson" % "gson" % "1.7.1"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion
)

