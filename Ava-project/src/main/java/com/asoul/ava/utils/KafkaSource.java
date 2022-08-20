package com.asoul.ava.utils;

import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Collections;
import java.util.Properties;

//kafka消息源（配置写死）
public class KafkaSource {
    Properties props = new Properties();
    String topic;
    KafkaConsumer<String, String> consumer;
    //获取消费者，循环消费数据
    public KafkaConsumer<String, String> getConsumer() {
        return consumer;
    }

    public void setUp(KafkaConsumer<String, String> consumer) {
        props.put("bootstrap.servers", "192.168.19.4:9092");
        props.put("group.id", "groupIdName");
        props.put("enable.auto.commit", "false");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));
        this.consumer = consumer;
    }


    public KafkaSource(String topic){
        this.topic=topic;
        setUp(consumer);
    }
}
