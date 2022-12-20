package com.coding.kafka.kafka01;

import java.time.Duration;
import java.util.*;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConsumerTest {

    public static final String TOPIC_NAME = "test";

    @Order(1)
    @Test
    public void testHelloWorld() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "emon:9092");
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "test");
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");

        // Consumer的主对象
        try (KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties)) {
            // 消费者订阅的TOPIC（一个或多个）
            kafkaConsumer.subscribe(Collections.singletonList(TOPIC_NAME));
            while (true) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> record : records)
                    System.out.printf("partition = %d, offset = %d, key = %s, value = %s%n", record.partition(),
                        record.offset(), record.key(), record.value());
            }
        }
    }

    @Order(2)
    @Test
    public void testCommitedOffset() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "emon:9092");
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "test");
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");

        // Consumer的主对象
        try (KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties)) {
            // 消费者订阅的TOPIC（一个或多个）
            kafkaConsumer.subscribe(Collections.singletonList(TOPIC_NAME));
            while (true) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> record : records) {
                    // 想把数据保存到数据库，成功就成功，不成功就。。。
                    // TODO: 2022/11/14 record 2 db
                    System.out.printf("partition = %d, offset = %d, key = %s, value = %s%n", record.partition(),
                        record.offset(), record.key(), record.value());
                    // 如果失败，则回滚，不要提交offset
                }

                // 手动通知
                kafkaConsumer.commitAsync();
            }
        }
    }

    /**
     * 手动提交offset，并手动控制Partition
     */
    @Order(3)
    @Test
    public void testCommitedOffsetWithPartition() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "emon:9092");
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "test");
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000"); // 默认10秒
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "5"); // 默认500

        // Consumer的主对象
        try (KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties)) {
            // 消费者订阅的TOPIC（一个或多个）
            kafkaConsumer.subscribe(Collections.singletonList(TOPIC_NAME));
            while (true) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(1000));
                // 每个partiton单独处理
                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, String>> recordsOfPartitions = records.records(partition);
                    for (ConsumerRecord<String, String> record : recordsOfPartitions) {
                        System.out.printf("partition = %d, offset = %d, key = %s, value = %s%n", record.partition(),
                            record.offset(), record.key(), record.value());
                    }

                    long lastOffset = recordsOfPartitions.get(recordsOfPartitions.size() - 1).offset();
                    // 单个partition中的offset，并且进行提交
                    Map<TopicPartition, OffsetAndMetadata> offset = new HashMap<>();
                    offset.put(partition, new OffsetAndMetadata(lastOffset + 1)); // 表示下一次消费的开始位置，而不是本次消费的截止位置。
                    // 提交offset
                    kafkaConsumer.commitSync(offset);
                }
            }
        }
    }

    /**
     * 手动提交offset，并手动控制Partition
     */
    @Order(4)
    @Test
    public void testCommitedOffsetWithPartition2() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "emon:9092");
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "test");
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000"); // 默认10秒
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "5"); // 默认500

        // Consumer的主对象
        try (KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties)) {
            TopicPartition p0 = new TopicPartition(TOPIC_NAME, 0);
            TopicPartition p1 = new TopicPartition(TOPIC_NAME, 1);

            // 消费者订阅的TOPIC（一个或多个）
            // kafkaConsumer.subscribe(Collections.singletonList(TOPIC_NAME));

            // 消费订阅某个TOPIC的某个分区：托管的API，信马由缰了，脱离Kafka的group管控了，不再有rebalance了。
            kafkaConsumer.assign(Collections.singleton(p0));
            while (true) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(1000));
                // 每个partition单独处理
                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, String>> recordsOfPartitions = records.records(partition);
                    for (ConsumerRecord<String, String> record : recordsOfPartitions) {
                        System.out.printf("partition = %d, offset = %d, key = %s, value = %s%n", record.partition(),
                            record.offset(), record.key(), record.value());
                    }

                    long lastOffset = recordsOfPartitions.get(recordsOfPartitions.size() - 1).offset();
                    // 单个partition中的offset，并且进行提交
                    Map<TopicPartition, OffsetAndMetadata> offset = new HashMap<>();
                    offset.put(partition, new OffsetAndMetadata(lastOffset + 1)); // 表示下一次消费的开始位置，而不是本次消费的截止位置。
                    // 提交offset
                    kafkaConsumer.commitSync(offset);
                }
            }
        }
    }

    /**
     * 手动指定offset的起始位置，以及手动提交offset
     */
    @Order(5)
    @Test
    public void testControlOffset() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "emon:9092");
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "test");
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000"); // 默认10秒
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "5"); // 默认500

        // Consumer的主对象
        try (KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties)) {
            TopicPartition p0 = new TopicPartition(TOPIC_NAME, 0);

            // 消费订阅某个TOPIC的某个分区：托管的API，信马由缰了，脱离Kafka的group管控了，不再有rebalance了。
            kafkaConsumer.assign(Collections.singleton(p0));

            while (true) {
                /*
                 * 使用场景：
                 * 1、人为控制offset起始位置
                 * 2、如果出现程序错误，重新消费一遍
                 * 使用方法
                 * 1、第一次从0消费【一般情况】
                 * 2、比如一次消费了100条，offset置为101并且存入Redis
                 * 3、每次poll之前，读取Redis最新的offset位置
                 * 4、每次从这个位置开始消费
                 */

                // 手动指定offset起始位置
                kafkaConsumer.seek(p0, 100);

                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(1000));
                // 每个partition单独处理
                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, String>> recordsOfPartitions = records.records(partition);
                    for (ConsumerRecord<String, String> record : recordsOfPartitions) {
                        System.out.printf("partition = %d, offset = %d, key = %s, value = %s%n", record.partition(),
                            record.offset(), record.key(), record.value());
                    }

                    long lastOffset = recordsOfPartitions.get(recordsOfPartitions.size() - 1).offset();
                    // 单个partition中的offset，并且进行提交
                    Map<TopicPartition, OffsetAndMetadata> offset = new HashMap<>();
                    offset.put(partition, new OffsetAndMetadata(lastOffset + 1)); // 表示下一次消费的开始位置，而不是本次消费的截止位置。
                    // 提交offset
                    kafkaConsumer.commitSync(offset);
                }
            }
        }
    }

    /**
     * 流量控制 - 限流
     */
    @Order(4)
    @Test
    public void testControlPause() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "emon:9092");
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "test");
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000"); // 默认10秒
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "5"); // 默认500

        // Consumer的主对象
        try (KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties)) {
            TopicPartition p0 = new TopicPartition(TOPIC_NAME, 0);
            TopicPartition p1 = new TopicPartition(TOPIC_NAME, 1);

            // 消费者订阅的TOPIC（一个或多个）
            // kafkaConsumer.subscribe(Collections.singletonList(TOPIC_NAME));

            // 消费订阅某个TOPIC的某个分区
            kafkaConsumer.assign(Arrays.asList(p0));

            long totalNum = 3L;
            while (true) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(1000));
                // 每个partition单独处理
                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, String>> recordsOfPartitions = records.records(partition);
                    long num = 0;
                    for (ConsumerRecord<String, String> record : recordsOfPartitions) {
                        System.out.printf("partition = %d, offset = %d, key = %s, value = %s%n", record.partition(),
                            record.offset(), record.key(), record.value());
                        /*
                        1、接收到record信息以后，去令牌桶中拿取令牌
                        2、如果获取到令牌，则继续业务处理
                        3、如果获取不到令牌，则pause等待令牌
                        4、当令牌中的令牌足够，则将consumer置为resume状态
                         */
                        num++;
                        if (record.partition() == 0) {
                            if (num == totalNum) {
                                System.out.println("太多了，p0临时被暂停了");
                                kafkaConsumer.pause(Arrays.asList(p0));
                            }

                            if (records.count() == num) {
                                System.out.println("发现不太多，p0又被恢复了");
                                kafkaConsumer.resume(Arrays.asList(p0));
                            }
                        }
                    }

                    long lastOffset = recordsOfPartitions.get(recordsOfPartitions.size() - 1).offset();
                    // 单个partition中的offset，并且进行提交
                    Map<TopicPartition, OffsetAndMetadata> offset = new HashMap<>();
                    offset.put(partition, new OffsetAndMetadata(lastOffset + 1)); // 表示下一次消费的开始位置，而不是本次消费的截止位置。
                    // 提交offset
                    kafkaConsumer.commitSync(offset);
                }
            }
        }
    }

    @Order(6)
    @Test
    public void testHelloWorldWithSSL() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "emon:8989");
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "test");
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");

        /*properties.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        properties.setProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "client.truststore.jks");
        properties.setProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "123456");
        properties.setProperty(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");*/

        properties.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        properties.setProperty(SslConfigs.SSL_PROTOCOL_CONFIG, "SSL");
        properties.setProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, "client1.keystore.jks");
        properties.setProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "123890");
        properties.setProperty(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "123890");
        properties.setProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "client1.truststore.jks");
        properties.setProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "123890");
        properties.setProperty(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");

        // Consumer的主对象
        try (KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties)) {
            // 消费者订阅的TOPIC（一个或多个）
            kafkaConsumer.subscribe(Collections.singletonList(TOPIC_NAME));
            while (true) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> record : records)
                    System.out.printf("partition = %d, offset = %d, key = %s, value = %s%n", record.partition(),
                        record.offset(), record.key(), record.value());
            }
        }
    }

}
