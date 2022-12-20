## 5.4、Kafka安全

- Kafka的安全措施
  - Kafka提供了SSL或SASL机制（听说SSL降低Kafka20%的性能）
  - Kafka提供了Broker到ZooKeeper链接的安全机制
  - Kafka支持Client的读写验证

### 5.4.1、SSL

#### 1、切换到证书存储目录

```bash
$ mkdir /usr/local/kafka/ssl && cd /usr/local/kafka/ssl
```

#### 2、生成秘钥证书

##### 2.1、创建存储证书文件的服务端密钥仓库

```bash
$ keytool -keystore server.keystore.jks -alias emonkafka -validity 36500 -genkey
```

> 【命令解释】
>
> emonkafka	秘钥别名
>
> | 命令      | 解释            |
> | --------- | --------------- |
> | emonkafka | 秘钥别名        |
> | validity  | 秘钥有效期100年 |
>
> 【命令执行概述】
>
> ```bash
> 输入密钥库口令: 123456
> 再次输入新口令: 123456
> 您的名字与姓氏是什么?
>   [Unknown]:  ml    
> 您的组织单位名称是什么?
>   [Unknown]:  emon
> 您的组织名称是什么?
>   [Unknown]:  emon
> 您所在的城市或区域名称是什么?
>   [Unknown]:  hangzhou
> 您所在的省/市/自治区名称是什么?
>   [Unknown]:  zhejiangsheng
> 该单位的双字母国家/地区代码是什么?
>   [Unknown]:  cn
> CN=ml, OU=emon, O=emon, L=hangzhou, ST=zhejiangsheng, C=cn是否正确?
>   [否]:  y
> 
> 输入 <emonkafka> 的密钥口令
> 	(如果和密钥库口令相同, 按回车): [回车]
> ```
>
> 【命令执行输出】
>
> -rw-r--r-- 1 root root 1970 11月 30 09:45 server.keystore.jks
>
> 【验证证书】
>
> ```bash
> $ keytool -list -v -keystore server.keystore.jks
> ```

##### 2.2、创建CA并将CA添加到客户信任库

- 创建CA

```bash
$ openssl req -new -x509 -keyout ca-key -out ca-cert -days 36500
```

> 【命令执行概述】
>
> ```bash
> Generating a 2048 bit RSA private key
> .......................................................................................................................................+++
> .......................................+++
> writing new private key to 'ca-key'
> Enter PEM pass phrase: 123456
> Verifying - Enter PEM pass phrase: 123456
> -----
> You are about to be asked to enter information that will be incorporated
> into your certificate request.
> What you are about to enter is what is called a Distinguished Name or a DN.
> There are quite a few fields but you can leave some blank
> For some fields there will be a default value,
> If you enter '.', the field will be left blank.
> -----
> Country Name (2 letter code) [XX]:cn
> State or Province Name (full name) []:zhejiangsheng
> Locality Name (eg, city) [Default City]:hangzhou
> Organization Name (eg, company) [Default Company Ltd]:emon
> Organizational Unit Name (eg, section) []:emon
> Common Name (eg, your name or your server's hostname) []:ml
> Email Address []: [忽略]
> ```
>
> 【命令执行输出】
>
> -rw-r--r-- 1 root root 1834 11月 30 09:48 ca-key
> -rw-r--r-- 1 root root 1310 11月 30 09:48 ca-cert

- 将CA添加到客户信任库（truststore）

```bash
$ keytool -keystore server.truststore.jks -alias CARoot -import -file ca-cert
# 命令行输出：为broker提供信任库以及所有客户端签名了密钥的CA证书
-rw-r--r-- 1 root root  990 11月 30 09:51 server.truststore.jks

$ keytool -keystore client.truststore.jks -alias CARoot -import -file ca-cert
# 命令行输出：【重要】客户端通过SSL访问Kafka服务时，需要使用
-rw-r--r-- 1 root root  990 11月 30 09:51 client.truststore.jks
```

##### 2.3、从密钥仓库导出证书，并用CA来签名

- 从密钥仓库导出证书

```bash
$ keytool -keystore server.keystore.jks -alias emonkafka -certreq -file cert-file
```

> 【命令执行输出】
>
> -rw-r--r-- 1 root root 1563 11月 30 09:54 cert-file

- 用CA来签名生成的证书

```bash
$ openssl x509 -req -CA ca-cert -CAkey ca-key -in cert-file -out cert-signed -days 36500 -CAcreateserial -passin pass:123456
```

> 【命令执行概述】
>
> ```bash
> Signature ok
> subject=/C=cn/ST=zhejiangsheng/L=hangzhou/O=emon/OU=emon/CN=ml
> Getting CA Private Key
> ```
>
> 【命令执行输出】
>
> -rw-r--r-- 1 root root 1931 11月 30 09:56 cert-signed
> -rw-r--r-- 1 root root   17 11月 30 09:56 ca-cert.srl

##### 2.4、导入CA证书和已签名的证书到密钥仓库

```bash
$ keytool -keystore server.keystore.jks -alias CARoot -import -file ca-cert
$ keytool -keystore server.keystore.jks -alias emonkafka -import -file cert-signed
```

> 【命令执行输出】执行上述命令，会变更如下的文件
>
> -rw-r--r-- 1 root root 4027 11月 30 09:57 server.keystore.jks

#### 3、Kafka服务端配置

- 配置

```bash
$ vim /usr/local/kafka/config/server.properties 
```

```bash
# [修改]
listeners=PLAINTEXT://emon:9092,SSL://emon:8989
# [修改]
advertised.listeners=PLAINTEXT://emon:9092,SSL://emon:8989
# [新增]在advertised.listeners后面追加ssl配置
ssl.keystore.location=/usr/local/kafka/ssl/server.keystore.jks
ssl.keystore.password=123456
ssl.key.password=123456
ssl.truststore.location=/usr/local/kafka/ssl/server.truststore.jks
ssl.truststore.password=123456
# 设置空可以使得证书的主机名与kafka的主机名不用保持一致
ssl.endpoint.identification.algorithm=
# [修改]
log.dirs=/tmp/kafka-logs => log.dirs=/usr/local/kafka/logs
# [修改]
zookeeper.connect=localhost:2181=>zookeeper.connect=emon:2181
```

- 启动

- 测试

```bash
# 测试SSL是否成功
$ openssl s_client -debug -connect emon:8989 -tls1
```

#### 4、客户端配置及执行日志【访问不到】

- 将上面生成的客户信任库client.truststore.jks复制到到java项目`根目录`中，以便client可以信任这个CA

- 代码参考

```java
@Test
public void testAsyncSendWithSSL() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "emon:8989");
    properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
    properties.setProperty(ProducerConfig.RETRIES_CONFIG, "0");
    properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, "16348");
    properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "1");
    properties.setProperty(ProducerConfig.BUFFER_MEMORY_CONFIG, "33554432");
    properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                           "org.apache.kafka.common.serialization.StringSerializer");
    properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                           "org.apache.kafka.common.serialization.StringSerializer");

    properties.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
    properties.setProperty(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
    properties.setProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "client.truststore.jks");
    properties.setProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "123456");

    // Producer的主对象
    KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);

    // 消息对象 - ProducerRecord
    for (int i = 0; i < 10; i++) {
        String key = "key-" + i;
        String value = "value-" + i;
        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC_NAME, key, value);
        kafkaProducer.send(record);
    }

    // 所有的通道打开都需要关闭
    kafkaProducer.close();
}
```

执行结果日志：

```tex
C:\Applications\Java64\jdk1.8.0_91\bin\java.exe -ea -Didea.test.cyclic.buffer.size=10485760 "-javaagent:C:\Job\JobSoftware\JetBrains\IntelliJ IDEA 2022.1.3\lib\idea_rt.jar=11521:C:\Job\JobSoftware\JetBrains\IntelliJ IDEA 2022.1.3\bin" -Dfile.encoding=UTF-8 -classpath "C:\Users\liming\.m2\repository\org\junit\platform\junit-platform-launcher\1.8.2\junit-platform-launcher-1.8.2.jar;C:\Users\liming\.m2\repository\org\junit\platform\junit-platform-engine\1.8.2\junit-platform-engine-1.8.2.jar;C:\Users\liming\.m2\repository\org\opentest4j\opentest4j\1.2.0\opentest4j-1.2.0.jar;C:\Users\liming\.m2\repository\org\junit\platform\junit-platform-commons\1.8.2\junit-platform-commons-1.8.2.jar;C:\Users\liming\.m2\repository\org\apiguardian\apiguardian-api\1.1.2\apiguardian-api-1.1.2.jar;C:\Job\JobSoftware\JetBrains\IntelliJ IDEA 2022.1.3\lib\idea_rt.jar;C:\Job\JobSoftware\JetBrains\IntelliJ IDEA 2022.1.3\plugins\junit\lib\junit5-rt.jar;C:\Job\JobSoftware\JetBrains\IntelliJ IDEA 2022.1.3\plugins\junit\lib\junit-rt.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\charsets.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\deploy.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\ext\access-bridge-64.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\ext\cldrdata.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\ext\dnsns.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\ext\jaccess.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\ext\jfxrt.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\ext\localedata.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\ext\nashorn.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\ext\sunec.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\ext\sunjce_provider.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\ext\sunmscapi.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\ext\sunpkcs11.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\ext\zipfs.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\javaws.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\jce.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\jfr.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\jfxswt.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\jsse.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\management-agent.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\plugin.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\resources.jar;C:\Applications\Java64\jdk1.8.0_91\jre\lib\rt.jar;C:\Job\JobResource\IdeaProjects\Idea2020\backend-kafka-learning\kafka-01\target\test-classes;C:\Job\JobResource\IdeaProjects\Idea2020\backend-kafka-learning\kafka-01\target\classes;C:\Job\JobResource\local-repository\org\springframework\boot\spring-boot-starter-web\2.7.5\spring-boot-starter-web-2.7.5.jar;C:\Job\JobResource\local-repository\org\springframework\boot\spring-boot-starter\2.7.5\spring-boot-starter-2.7.5.jar;C:\Job\JobResource\local-repository\org\springframework\boot\spring-boot\2.7.5\spring-boot-2.7.5.jar;C:\Job\JobResource\local-repository\org\springframework\boot\spring-boot-autoconfigure\2.7.5\spring-boot-autoconfigure-2.7.5.jar;C:\Job\JobResource\local-repository\org\springframework\boot\spring-boot-starter-logging\2.7.5\spring-boot-starter-logging-2.7.5.jar;C:\Job\JobResource\local-repository\ch\qos\logback\logback-classic\1.2.11\logback-classic-1.2.11.jar;C:\Job\JobResource\local-repository\ch\qos\logback\logback-core\1.2.11\logback-core-1.2.11.jar;C:\Job\JobResource\local-repository\org\apache\logging\log4j\log4j-to-slf4j\2.17.2\log4j-to-slf4j-2.17.2.jar;C:\Job\JobResource\local-repository\org\apache\logging\log4j\log4j-api\2.17.2\log4j-api-2.17.2.jar;C:\Job\JobResource\local-repository\org\slf4j\jul-to-slf4j\1.7.36\jul-to-slf4j-1.7.36.jar;C:\Job\JobResource\local-repository\jakarta\annotation\jakarta.annotation-api\1.3.5\jakarta.annotation-api-1.3.5.jar;C:\Job\JobResource\local-repository\org\yaml\snakeyaml\1.30\snakeyaml-1.30.jar;C:\Job\JobResource\local-repository\org\springframework\boot\spring-boot-starter-json\2.7.5\spring-boot-starter-json-2.7.5.jar;C:\Job\JobResource\local-repository\com\fasterxml\jackson\core\jackson-databind\2.13.4.2\jackson-databind-2.13.4.2.jar;C:\Job\JobResource\local-repository\com\fasterxml\jackson\core\jackson-annotations\2.13.4\jackson-annotations-2.13.4.jar;C:\Job\JobResource\local-repository\com\fasterxml\jackson\core\jackson-core\2.13.4\jackson-core-2.13.4.jar;C:\Job\JobResource\local-repository\com\fasterxml\jackson\datatype\jackson-datatype-jdk8\2.13.4\jackson-datatype-jdk8-2.13.4.jar;C:\Job\JobResource\local-repository\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.13.4\jackson-datatype-jsr310-2.13.4.jar;C:\Job\JobResource\local-repository\com\fasterxml\jackson\module\jackson-module-parameter-names\2.13.4\jackson-module-parameter-names-2.13.4.jar;C:\Job\JobResource\local-repository\org\springframework\boot\spring-boot-starter-tomcat\2.7.5\spring-boot-starter-tomcat-2.7.5.jar;C:\Job\JobResource\local-repository\org\apache\tomcat\embed\tomcat-embed-core\9.0.68\tomcat-embed-core-9.0.68.jar;C:\Job\JobResource\local-repository\org\apache\tomcat\embed\tomcat-embed-el\9.0.68\tomcat-embed-el-9.0.68.jar;C:\Job\JobResource\local-repository\org\apache\tomcat\embed\tomcat-embed-websocket\9.0.68\tomcat-embed-websocket-9.0.68.jar;C:\Job\JobResource\local-repository\org\springframework\spring-web\5.3.23\spring-web-5.3.23.jar;C:\Job\JobResource\local-repository\org\springframework\spring-beans\5.3.23\spring-beans-5.3.23.jar;C:\Job\JobResource\local-repository\org\springframework\spring-webmvc\5.3.23\spring-webmvc-5.3.23.jar;C:\Job\JobResource\local-repository\org\springframework\spring-aop\5.3.23\spring-aop-5.3.23.jar;C:\Job\JobResource\local-repository\org\springframework\spring-context\5.3.23\spring-context-5.3.23.jar;C:\Job\JobResource\local-repository\org\springframework\spring-expression\5.3.23\spring-expression-5.3.23.jar;C:\Job\JobResource\local-repository\org\apache\kafka\kafka-clients\2.5.1\kafka-clients-2.5.1.jar;C:\Job\JobResource\local-repository\com\github\luben\zstd-jni\1.4.4-7\zstd-jni-1.4.4-7.jar;C:\Job\JobResource\local-repository\org\lz4\lz4-java\1.7.1\lz4-java-1.7.1.jar;C:\Job\JobResource\local-repository\org\xerial\snappy\snappy-java\1.1.7.3\snappy-java-1.1.7.3.jar;C:\Job\JobResource\local-repository\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar;C:\Job\JobResource\local-repository\org\apache\kafka\kafka-streams\2.5.1\kafka-streams-2.5.1.jar;C:\Job\JobResource\local-repository\org\apache\kafka\connect-json\3.1.2\connect-json-3.1.2.jar;C:\Job\JobResource\local-repository\org\apache\kafka\connect-api\3.1.2\connect-api-3.1.2.jar;C:\Job\JobResource\local-repository\org\rocksdb\rocksdbjni\5.18.3\rocksdbjni-5.18.3.jar;C:\Job\JobResource\local-repository\com\google\guava\guava\31.1-jre\guava-31.1-jre.jar;C:\Job\JobResource\local-repository\com\google\guava\failureaccess\1.0.1\failureaccess-1.0.1.jar;C:\Job\JobResource\local-repository\com\google\guava\listenablefuture\9999.0-empty-to-avoid-conflict-with-guava\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar;C:\Job\JobResource\local-repository\com\google\code\findbugs\jsr305\3.0.2\jsr305-3.0.2.jar;C:\Job\JobResource\local-repository\org\checkerframework\checker-qual\3.12.0\checker-qual-3.12.0.jar;C:\Job\JobResource\local-repository\com\google\errorprone\error_prone_annotations\2.11.0\error_prone_annotations-2.11.0.jar;C:\Job\JobResource\local-repository\com\google\j2objc\j2objc-annotations\1.3\j2objc-annotations-1.3.jar;C:\Job\JobResource\local-repository\com\alibaba\fastjson\1.2.83\fastjson-1.2.83.jar;C:\Job\JobResource\local-repository\org\springframework\boot\spring-boot-starter-test\2.7.5\spring-boot-starter-test-2.7.5.jar;C:\Job\JobResource\local-repository\org\springframework\boot\spring-boot-test\2.7.5\spring-boot-test-2.7.5.jar;C:\Job\JobResource\local-repository\org\springframework\boot\spring-boot-test-autoconfigure\2.7.5\spring-boot-test-autoconfigure-2.7.5.jar;C:\Job\JobResource\local-repository\com\jayway\jsonpath\json-path\2.7.0\json-path-2.7.0.jar;C:\Job\JobResource\local-repository\net\minidev\json-smart\2.4.8\json-smart-2.4.8.jar;C:\Job\JobResource\local-repository\net\minidev\accessors-smart\2.4.8\accessors-smart-2.4.8.jar;C:\Job\JobResource\local-repository\org\ow2\asm\asm\9.1\asm-9.1.jar;C:\Job\JobResource\local-repository\jakarta\xml\bind\jakarta.xml.bind-api\2.3.3\jakarta.xml.bind-api-2.3.3.jar;C:\Job\JobResource\local-repository\jakarta\activation\jakarta.activation-api\1.2.2\jakarta.activation-api-1.2.2.jar;C:\Job\JobResource\local-repository\org\assertj\assertj-core\3.22.0\assertj-core-3.22.0.jar;C:\Job\JobResource\local-repository\org\hamcrest\hamcrest\2.2\hamcrest-2.2.jar;C:\Job\JobResource\local-repository\org\junit\jupiter\junit-jupiter\5.8.2\junit-jupiter-5.8.2.jar;C:\Job\JobResource\local-repository\org\junit\jupiter\junit-jupiter-api\5.8.2\junit-jupiter-api-5.8.2.jar;C:\Job\JobResource\local-repository\org\opentest4j\opentest4j\1.2.0\opentest4j-1.2.0.jar;C:\Job\JobResource\local-repository\org\junit\platform\junit-platform-commons\1.8.2\junit-platform-commons-1.8.2.jar;C:\Job\JobResource\local-repository\org\apiguardian\apiguardian-api\1.1.2\apiguardian-api-1.1.2.jar;C:\Job\JobResource\local-repository\org\junit\jupiter\junit-jupiter-params\5.8.2\junit-jupiter-params-5.8.2.jar;C:\Job\JobResource\local-repository\org\junit\jupiter\junit-jupiter-engine\5.8.2\junit-jupiter-engine-5.8.2.jar;C:\Job\JobResource\local-repository\org\junit\platform\junit-platform-engine\1.8.2\junit-platform-engine-1.8.2.jar;C:\Job\JobResource\local-repository\org\mockito\mockito-core\4.5.1\mockito-core-4.5.1.jar;C:\Job\JobResource\local-repository\net\bytebuddy\byte-buddy\1.12.18\byte-buddy-1.12.18.jar;C:\Job\JobResource\local-repository\net\bytebuddy\byte-buddy-agent\1.12.18\byte-buddy-agent-1.12.18.jar;C:\Job\JobResource\local-repository\org\objenesis\objenesis\3.2\objenesis-3.2.jar;C:\Job\JobResource\local-repository\org\mockito\mockito-junit-jupiter\4.5.1\mockito-junit-jupiter-4.5.1.jar;C:\Job\JobResource\local-repository\org\skyscreamer\jsonassert\1.5.1\jsonassert-1.5.1.jar;C:\Job\JobResource\local-repository\com\vaadin\external\google\android-json\0.0.20131108.vaadin1\android-json-0.0.20131108.vaadin1.jar;C:\Job\JobResource\local-repository\org\springframework\spring-core\5.3.23\spring-core-5.3.23.jar;C:\Job\JobResource\local-repository\org\springframework\spring-jcl\5.3.23\spring-jcl-5.3.23.jar;C:\Job\JobResource\local-repository\org\springframework\spring-test\5.3.23\spring-test-5.3.23.jar;C:\Job\JobResource\local-repository\org\xmlunit\xmlunit-core\2.9.0\xmlunit-core-2.9.0.jar;C:\Job\JobResource\local-repository\org\projectlombok\lombok\1.18.24\lombok-1.18.24.jar;C:\Job\JobResource\local-repository\org\springframework\boot\spring-boot-configuration-processor\2.7.5\spring-boot-configuration-processor-2.7.5.jar" com.intellij.rt.junit.JUnitStarter -ideVersion5 -junit5 com.coding.kafka.kafka01.ProducerTest,testAsyncSendWithSSL
10:04:08,294 |-INFO in ch.qos.logback.classic.LoggerContext[default] - Could NOT find resource [logback-test.xml]
10:04:08,294 |-INFO in ch.qos.logback.classic.LoggerContext[default] - Found resource [logback.xml] at [file:/C:/Job/JobResource/IdeaProjects/Idea2020/backend-kafka-learning/kafka-01/target/test-classes/logback.xml]
10:04:08,298 |-WARN in ch.qos.logback.classic.LoggerContext[default] - Resource [logback.xml] occurs multiple times on the classpath.
10:04:08,298 |-WARN in ch.qos.logback.classic.LoggerContext[default] - Resource [logback.xml] occurs at [file:/C:/Job/JobResource/IdeaProjects/Idea2020/backend-kafka-learning/kafka-01/target/test-classes/logback.xml]
10:04:08,298 |-WARN in ch.qos.logback.classic.LoggerContext[default] - Resource [logback.xml] occurs at [file:/C:/Job/JobResource/IdeaProjects/Idea2020/backend-kafka-learning/kafka-01/target/classes/logback.xml]
10:04:08,376 |-INFO in ch.qos.logback.classic.joran.action.ConfigurationAction - debug attribute not set
10:04:08,376 |-INFO in ch.qos.logback.core.joran.action.AppenderAction - About to instantiate appender of type [ch.qos.logback.core.ConsoleAppender]
10:04:08,384 |-INFO in ch.qos.logback.core.joran.action.AppenderAction - Naming appender as [STDOUT]
10:04:08,388 |-INFO in ch.qos.logback.core.joran.action.NestedComplexPropertyIA - Assuming default type [ch.qos.logback.classic.encoder.PatternLayoutEncoder] for [encoder] property
10:04:08,423 |-INFO in ch.qos.logback.classic.joran.action.LoggerAction - Setting level of logger [org.apache.kafka.clients.consumer.internals] to INFO
10:04:08,423 |-INFO in ch.qos.logback.classic.joran.action.RootLoggerAction - Setting level of ROOT logger to INFO
10:04:08,423 |-INFO in ch.qos.logback.core.joran.action.AppenderRefAction - Attaching appender named [STDOUT] to Logger[ROOT]
10:04:08,423 |-INFO in ch.qos.logback.classic.joran.action.ConfigurationAction - End of configuration.
10:04:08,423 |-INFO in ch.qos.logback.classic.joran.JoranConfigurator@795cd85e - Registering current configuration as safe fallback point

10:04:08.439 [main] INFO  o.a.k.c.producer.ProducerConfig - ProducerConfig values: 
	acks = -1
	batch.size = 16348
	bootstrap.servers = [emon:8989]
	buffer.memory = 33554432
	client.dns.lookup = default
	client.id = producer-1
	compression.type = none
	connections.max.idle.ms = 540000
	delivery.timeout.ms = 120000
	enable.idempotence = false
	interceptor.classes = []
	key.serializer = class org.apache.kafka.common.serialization.StringSerializer
	linger.ms = 1
	max.block.ms = 60000
	max.in.flight.requests.per.connection = 5
	max.request.size = 1048576
	metadata.max.age.ms = 300000
	metadata.max.idle.ms = 300000
	metric.reporters = []
	metrics.num.samples = 2
	metrics.recording.level = INFO
	metrics.sample.window.ms = 30000
	partitioner.class = class org.apache.kafka.clients.producer.internals.DefaultPartitioner
	receive.buffer.bytes = 32768
	reconnect.backoff.max.ms = 1000
	reconnect.backoff.ms = 50
	request.timeout.ms = 30000
	retries = 0
	retry.backoff.ms = 100
	sasl.client.callback.handler.class = null
	sasl.jaas.config = null
	sasl.kerberos.kinit.cmd = /usr/bin/kinit
	sasl.kerberos.min.time.before.relogin = 60000
	sasl.kerberos.service.name = null
	sasl.kerberos.ticket.renew.jitter = 0.05
	sasl.kerberos.ticket.renew.window.factor = 0.8
	sasl.login.callback.handler.class = null
	sasl.login.class = null
	sasl.login.refresh.buffer.seconds = 300
	sasl.login.refresh.min.period.seconds = 60
	sasl.login.refresh.window.factor = 0.8
	sasl.login.refresh.window.jitter = 0.05
	sasl.mechanism = GSSAPI
	security.protocol = SSL
	security.providers = null
	send.buffer.bytes = 131072
	ssl.cipher.suites = null
	ssl.enabled.protocols = [TLSv1.2]
	ssl.endpoint.identification.algorithm = 
	ssl.key.password = null
	ssl.keymanager.algorithm = SunX509
	ssl.keystore.location = null
	ssl.keystore.password = null
	ssl.keystore.type = JKS
	ssl.protocol = TLSv1.2
	ssl.provider = null
	ssl.secure.random.implementation = null
	ssl.trustmanager.algorithm = PKIX
	ssl.truststore.location = client.truststore.jks
	ssl.truststore.password = [hidden]
	ssl.truststore.type = JKS
	transaction.timeout.ms = 60000
	transactional.id = null
	value.serializer = class org.apache.kafka.common.serialization.StringSerializer

10:04:09.770 [main] INFO  o.a.kafka.common.utils.AppInfoParser - Kafka version: 2.5.1
10:04:09.770 [main] INFO  o.a.kafka.common.utils.AppInfoParser - Kafka commitId: 0efa8fb0f4c73d92
10:04:09.770 [main] INFO  o.a.kafka.common.utils.AppInfoParser - Kafka startTimeMs: 1669773849766
10:13:39.791 [kafka-producer-network-thread | producer-1] WARN  o.apache.kafka.clients.NetworkClient - [Producer clientId=producer-1] Bootstrap broker emon:8989 (id: -1 rack: null) disconnected
10:14:09.797 [main] INFO  o.a.k.clients.producer.KafkaProducer - [Producer clientId=producer-1] Closing the Kafka producer with timeoutMillis = 9223372036854775807 ms.

Process finished with exit code 0

```

以上日志显示，尝试连接，卡住了接近10分钟，然后断开了。



#### 5、其他

以上 emon 是本地DNS解析，解析到自己的Linux虚拟机IP。



## 6、原因：

只因为生产的服务端密钥库时，未指定`-keyalg RSA`，因为默认是DSA。

```bash
$ keytool -keystore server.keystore.jks -alias emonkafka -validity 36500 -genkey -keyalg RSA
```

参考：

https://kafka.apache.org/25/documentation.html#security_ssl_key