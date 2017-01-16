package com.sapient.Attendant.kafkaConnectors

import java.util
import java.util.Properties

import kafka.consumer.{ConsumerConfig, KafkaStream}


/**
  * Created by agu225 on 12/1/2017.
  */
object MessageReceiver {
  private def createConsumerConfig(zookeeper: String, groupId: String): ConsumerConfig = {
    val props = new Properties()
    props.put("zookeeper.connect", zookeeper)
    props.put("group.id", groupId)
    props.put("auto.offset.reset","smallest")
    props.put("zookeeper.session.timeout.ms", "500")
    props.put("zookeeper.sync.time.ms", "250")
    props.put("auto.commit.interval.ms", "1000")
    new ConsumerConfig(props)
  }
  /**/
}
class MessageReceiver(zookeeper: String, groupId: String, private val topic: String){
  private val consumer =
    kafka.consumer.Consumer.createJavaConsumerConnector(MessageReceiver.createConsumerConfig(zookeeper,groupId))
  def startConsumer : util.List[KafkaStream[Array[Byte],Array[Byte]]]= {
    val topicMap = new util.HashMap[String, Integer]()
    topicMap.put(topic, 1)

    val consumerStreamsMap = consumer.createMessageStreams(topicMap)
    val streamList = consumerStreamsMap.get(topic)
    streamList
    /*if (consumer != null) {
      consumer.shutdown()
    }*/
  }
}
