package com.sapient.Attendant.kafkaConnectors

import java.util.Properties

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

/**
  * Created by agu225 on 9/1/2017.
  */

class MessageSender{
  private var producer: KafkaProducer[String,String] = _
  val props = new Properties()
  props.put("bootstrap.servers","localhost:9092") //broker addresses
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("request.required.acks", "1")
  producer = new KafkaProducer(props)

  def publishMessage(topic:String, message:String) ={
    producer.send(new ProducerRecord(topic,message))
  }
}
