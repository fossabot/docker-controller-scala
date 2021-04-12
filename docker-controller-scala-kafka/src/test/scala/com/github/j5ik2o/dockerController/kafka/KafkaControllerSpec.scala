package com.github.j5ik2o.dockerController.kafka

import com.github.j5ik2o.dockerController.WaitPredicates.WaitPredicate
import com.github.j5ik2o.dockerController._
import org.apache.kafka.clients.consumer.{ ConsumerConfig, KafkaConsumer }
import org.apache.kafka.clients.producer.{ KafkaProducer, ProducerConfig, ProducerRecord }
import org.apache.kafka.common.TopicPartition
import org.scalatest.freespec.AnyFreeSpec

import java.time.{ LocalDateTime, Duration => JavaDuration }
import java.util.{ Collections, Properties }
import scala.concurrent.duration.Duration

class KafkaControllerSpec extends AnyFreeSpec with DockerControllerSpecSupport {

  val kafkaExternalHostPort: Int = RandomPortUtil.temporaryServerPort()

  val kafkaController = new KafkaController(dockerClient)(
    kafkaExternalHostName = dockerHost,
    kafkaExternalHostPort = kafkaExternalHostPort,
    createTopics = Seq("mytopic")
  )

  override protected val dockerControllers: Vector[DockerController] =
    Vector(kafkaController.zooKeeperController, kafkaController)

  val zooKeeperWaitPredicate: WaitPredicate =
    WaitPredicates.forLogMessageByRegex(ZooKeeperController.RegexForWaitPredicate)
  val zooKeeperWaitPredicateSetting: WaitPredicateSetting = WaitPredicateSetting(Duration.Inf, zooKeeperWaitPredicate)

  val kafkaWaitPredicate: WaitPredicate               = WaitPredicates.forLogMessageByRegex(KafkaController.RegexForWaitPredicate)
  val kafkaWaitPredicateSetting: WaitPredicateSetting = WaitPredicateSetting(Duration.Inf, kafkaWaitPredicate)

  override protected val waitPredicatesSettings: Map[DockerController, WaitPredicateSetting] = {
    Map(
      kafkaController.zooKeeperController -> zooKeeperWaitPredicateSetting,
      kafkaController                     -> kafkaWaitPredicateSetting
    )
  }

  "KafkaController" - {
    "produce&consume" in {
      val consumerRunnable = new Runnable {
        override def run(): Unit = {
          val consumerProperties = new Properties()
          consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, s"$dockerHost:$kafkaExternalHostPort")
          consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

          consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "myConsumerGroup")
          consumerProperties.put(
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer"
          )
          consumerProperties.put(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer"
          )
          val consumer = new KafkaConsumer[String, String](consumerProperties)
          consumer.subscribe(Collections.singletonList("mytopic"))
          try {
            while (true) {
              logger.debug("consumer:=============================")
              val records = consumer.poll(JavaDuration.ofMillis(1000))
              logger.debug("consumer:=============================")
              logger.debug("[record size] " + records.count());
              records.forEach { record =>
                logger.debug("consumer:=============================")
                logger.debug("consumer:" + LocalDateTime.now)
                logger.debug("consumer:topic: " + record.topic)
                logger.debug("consumer:partition: " + record.partition)
                logger.debug("consumer:key: " + record.key)
                logger.debug("consumer:value: " + record.value)
                logger.debug("consumer:offset: " + record.offset)
                val topicPartition    = new TopicPartition(record.topic, record.partition)
                val offsetAndMetadata = consumer.committed(topicPartition)
                if (offsetAndMetadata != null)
                  logger.debug("partition offset: " + offsetAndMetadata.offset)
              }
            }
          } catch {
            case ex: InterruptedException =>
              logger.warn("occurred error", ex)
          }
          consumer.close()
        }
      }
      val t = new Thread(consumerRunnable)
      t.start()
      val producerProperties = new Properties()
      producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, s"$dockerHost:$kafkaExternalHostPort")
      producerProperties.put(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringSerializer"
      )
      producerProperties.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringSerializer"
      )
      val producer = new KafkaProducer[String, String](producerProperties)
      (1 to 10).foreach { n =>
        val record         = new ProducerRecord[String, String]("mytopic", "my-value-" + n)
        val send           = producer.send(record)
        val recordMetadata = send.get
        logger.debug("producer:=============================")
        logger.debug("producer:" + LocalDateTime.now)
        logger.debug("producer:topic: " + recordMetadata.topic)
        logger.debug("producer:partition: " + recordMetadata.partition)
        logger.debug("producer:offset: " + recordMetadata.offset)
      }
      producer.close()
      Thread.sleep(1000 * 10)
      t.interrupt()
      t.join()
    }

  }

}