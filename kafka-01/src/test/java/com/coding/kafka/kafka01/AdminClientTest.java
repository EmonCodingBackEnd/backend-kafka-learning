package com.coding.kafka.kafka01;

import java.util.*;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminClientTest {

    // kafka-topics.sh --bootstrap-server emon:9092 --create --partitions 2 --replication-factor 1 --topic admin-topic
    // kafka-topics.sh --bootstrap-server emon:9092 --create --partitions 2 --replication-factor 1 --topic test
    public static final String TOPIC_NAME = "admin-topic";

    private static AdminClient adminClient;

    @BeforeAll
    static void beforeAll() {
        Properties properties = new Properties();
        properties.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "emon:9092");
        adminClient = AdminClient.create(properties);
    }

    @AfterAll
    static void afterAll() {
        if (adminClient != null) {
            adminClient.close();
        }
    }

    @BeforeEach
    public void beforeEach() {}

    @AfterEach
    public void afterEach() {}

    @Order(1)
    @Test
    public void testAdminClient() {
        System.out.println(adminClient);
    }

    @Order(2)
    @Test
    public void testCreateTopic() throws Exception {
        NewTopic newTopic = new NewTopic(TOPIC_NAME, 1, (short)1);
        CreateTopicsResult createTopicsResult = adminClient.createTopics(Collections.singletonList(newTopic));
        createTopicsResult.all().get();
    }

    @Order(3)
    @Test
    public void testTopicLists() throws Exception {
        ListTopicsOptions options = new ListTopicsOptions();
        options.listInternal(true);
        ListTopicsResult listTopicsResult = adminClient.listTopics();
        Set<String> names = listTopicsResult.names().get();
        names.forEach(System.out::println);
    }

    @Order(3)
    @Test
    public void testTopicLists2() throws Exception {
        ListTopicsOptions options = new ListTopicsOptions();
        options.listInternal(true);
        ListTopicsResult listTopicsResult = adminClient.listTopics(options);
        Set<String> names = listTopicsResult.names().get();
        names.forEach(System.out::println);
    }

    @Order(3)
    @Test
    public void testTopicLists3() throws Exception {
        ListTopicsOptions options = new ListTopicsOptions();
        options.listInternal(true);
        ListTopicsResult listTopicsResult = adminClient.listTopics(options);
        Collection<TopicListing> topicListings = listTopicsResult.listings().get();
        topicListings.forEach(System.out::println);
    }

    @Order(3)
    @Test
    public void testTopicLists4() throws Exception {
        ListTopicsOptions options = new ListTopicsOptions();
        options.listInternal(true);
        ListTopicsResult listTopicsResult = adminClient.listTopics(options);
        Map<String, TopicListing> stringTopicListingMap = listTopicsResult.namesToListings().get();
        stringTopicListingMap.entrySet().forEach(System.out::println);
    }

    @Order(4)
    @Test
    public void testDescribeTopic() throws Exception {
        DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(Collections.singletonList(TOPIC_NAME));
        Map<String, TopicDescription> stringTopicDescriptionMap = describeTopicsResult.all().get();
        stringTopicDescriptionMap.entrySet().forEach(System.out::println);
    }

    @Order(4)
    @Test
    public void testDescribeTopic2() throws Exception {
        ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, TOPIC_NAME);
        DescribeConfigsResult describeConfigsResult =
            adminClient.describeConfigs(Collections.singletonList(configResource));
        Map<ConfigResource, Config> configResourceConfigMap = describeConfigsResult.all().get();
        configResourceConfigMap.entrySet().forEach(System.out::println);
    }

    @Order(4)
    @Test
    public void testDescribeTopic3() throws Exception {
        Map<ConfigResource, Config> configMaps = new HashMap<>();
        ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, TOPIC_NAME);
        Config config = new Config(Collections.singletonList(new ConfigEntry("preallocate", "true")));
        configMaps.put(configResource, config);
        AlterConfigsResult alterConfigsResult = adminClient.alterConfigs(configMaps);
        alterConfigsResult.all().get();

        DescribeConfigsResult describeConfigsResult =
            adminClient.describeConfigs(Collections.singletonList(configResource));
        Map<ConfigResource, Config> configResourceConfigMap = describeConfigsResult.all().get();
        configResourceConfigMap.entrySet().forEach(System.out::println);
    }

    @Order(4)
    @Test
    public void testDescribeTopic4() throws Exception {
        Map<ConfigResource, Collection<AlterConfigOp>> configMaps = new HashMap<>();
        ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, TOPIC_NAME);
        AlterConfigOp alterConfigOp =
            new AlterConfigOp(new ConfigEntry("preallocate", "false"), AlterConfigOp.OpType.SET);
        configMaps.put(configResource, Collections.singletonList(alterConfigOp));
        AlterConfigsResult alterConfigsResult = adminClient.incrementalAlterConfigs(configMaps);
        alterConfigsResult.all().get();

        DescribeConfigsResult describeConfigsResult =
            adminClient.describeConfigs(Collections.singletonList(configResource));
        Map<ConfigResource, Config> configResourceConfigMap = describeConfigsResult.all().get();
        configResourceConfigMap.entrySet().forEach(System.out::println);
    }

    @Order(4)
    @Test
    public void testIncrPartitions() throws Exception {
        Map<String, NewPartitions> partitionsMap = new HashMap<>();
        NewPartitions newPartitions = NewPartitions.increaseTo(2);
        partitionsMap.put(TOPIC_NAME, newPartitions);
        CreatePartitionsResult createPartitionsResult = adminClient.createPartitions(partitionsMap);
        createPartitionsResult.all().get();

        DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(Collections.singletonList(TOPIC_NAME));
        Map<String, TopicDescription> stringTopicDescriptionMap = describeTopicsResult.all().get();
        stringTopicDescriptionMap.entrySet().forEach(System.out::println);
    }

    @Order(100)
    @Test
    public void testDeleteTopic() throws Exception {
        DeleteTopicsResult deleteTopicsResult = adminClient.deleteTopics(Collections.singletonList(TOPIC_NAME));
        deleteTopicsResult.all().get();

        ListTopicsOptions options = new ListTopicsOptions();
        options.listInternal(true);
        ListTopicsResult listTopicsResult = adminClient.listTopics(options);
        Set<String> names = listTopicsResult.names().get();
        names.forEach(System.out::println);
    }

}
