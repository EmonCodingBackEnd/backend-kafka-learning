package com.coding.kafka.kafka01.stream;

import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;

public class StreamExample {

    /*
    创建Topic
    // kafka-topics.sh --bootstrap-server emon:9092 --create --partitions 2 --replication-factor 1 --topic stream-in
    // kafka-topics.sh --bootstrap-server emon:9092 --create --partitions 2 --replication-factor 1 --topic stream-out
    生产数据到stream-in：
    $ kafka-console-producer.sh --bootstrap-server emon:9092 --topic stream-in
    消费数据从stream-out：
    $ kafka-console-consumer.sh --bootstrap-server emon:9092 --topic stream-out \
    --property print.key=true \
    --property print.value=true \
    --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer \
    --from-beginning
     */
    private static final String INPUT_TOPIC = "stream-in";
    private static final String OUTPUT_TOPIC = "stream-out";

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "emon:9092");
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount-app");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // 如果构建流结果拓扑
        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        // 构建WordCount Processer
        // wordCountStream(streamsBuilder);
        foreachStream(streamsBuilder);

        // 为了避免 Informed to shut down ，这里不要使用 'try'-with-resources 并且不能使用@Test方式启动，只能使用main方法启动
        KafkaStreams streams = new KafkaStreams(streamsBuilder.build(), properties);
        streams.start();
        System.out.println(streams);
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private static void wordCountStream(final StreamsBuilder builder) {
        // 不断从INPUT_TOPIC上获取新数据，并且追加到流上的一个抽象对象
        KStream<String, String> source = builder.stream(INPUT_TOPIC);
        // Hello World emon
        // KTable是数据集合的抽象对象
        KTable<String, Long> count = source
            /*
            flatMapValues -> 将一行数据拆分为多行数据 hello world ==> (null,hello),(null,world)
            groupBy -> 对数据分组 (null,hello),(null,world) ==> (hello,1),(world,1)
             */
            .flatMapValues(value -> Arrays.asList(value.toLowerCase(Locale.getDefault()).split(" ")))
            .groupBy((key, value) -> value).count();
        count.toStream().to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.Long()));
    }

    private static void foreachStream(final StreamsBuilder builder) {
        KStream<String, String> source = builder.stream(INPUT_TOPIC);
        source.flatMapValues(value -> Arrays.asList(value.toLowerCase(Locale.getDefault()).split(" ")))
            .foreach((key, value) -> System.out.println(key + ":" + value));
    }
}
