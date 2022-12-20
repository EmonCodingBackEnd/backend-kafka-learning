package com.coding.kafka.kafka01.producer;

import java.util.Map;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExamplePartition implements Partitioner {
    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        /*
         * key-1 key-2 key-3
         */
        String keyStr = key + "";
        if (keyStr.startsWith("key-")) {
            String keyInt = keyStr.substring(4);
            int keyPartition = Integer.parseInt(keyInt) % 2;
            // 注意：针对同一个key，有可能会输出两次；如果producer按批次发送时，产生了新的批次，会重新计算分区，导致调用两次；否则，就是一次。
            log.info("keyStr={},keyInt={},keyPartition={}", keyStr, keyInt, keyPartition);
            return keyPartition;
        }
        return 0;
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> configs) {

    }
}
