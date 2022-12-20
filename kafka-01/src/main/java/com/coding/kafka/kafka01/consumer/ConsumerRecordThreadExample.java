package com.coding.kafka.kafka01.consumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

public class ConsumerRecordThreadExample {
    private final static String TOPIC_NAME = "test";

    public static void main(String[] args) throws InterruptedException {
        String brokerList = "emon:9092";
        String groupId = "test";
        int workerNum = 5;

        CunsumerExecutor consumers = new CunsumerExecutor(brokerList, groupId, TOPIC_NAME);
        consumers.execute(workerNum);

        Thread.sleep(100000);

        consumers.shutdown();

    }

    // Consumer处理
    public static class CunsumerExecutor {
        private final KafkaConsumer<String, String> consumer;
        private ExecutorService executors;

        public CunsumerExecutor(String brokerList, String groupId, String topic) {
            Properties props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
            props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
            props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
            props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
            consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Collections.singletonList(topic));
        }

        public void execute(int workerNum) {

            executors = new ThreadPoolExecutor(workerNum, workerNum, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1000), new ThreadPoolExecutor.CallerRunsPolicy());

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));
                for (final ConsumerRecord<String, String> record : records) {
                    executors.submit(new ConsumerRecordWorker(record));
                }
            }
        }

        public void shutdown() {
            if (consumer != null) {
                consumer.close();
            }
            try {
                if (!executors.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.out.println("Timeout.... Ignore for this case");
                }
            } catch (InterruptedException ignored) {
                System.out.println("Other thread interrupted this shutdown, ignore for this case.");
                Thread.currentThread().interrupt();
            }
            if (executors != null) {
                executors.shutdown();
            }
        }

    }

    // 记录处理
    public static class ConsumerRecordWorker implements Runnable {

        private final ConsumerRecord<String, String> record;

        public ConsumerRecordWorker(ConsumerRecord<String, String> record) {
            this.record = record;
        }

        @Override
        public void run() {
            System.out.println("Thread - " + Thread.currentThread().getName() + "|"
                + String.format("patition = %d , offset = %d, key = %s, value = %s%n", record.partition(),
                    record.offset(), record.key(), record.value()));
        }

    }
}
