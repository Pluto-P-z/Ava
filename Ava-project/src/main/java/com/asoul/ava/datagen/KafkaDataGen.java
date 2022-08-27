package com.asoul.ava.datagen;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
//生成kv数据，用于测试系统时间普通wordcount
public class KafkaDataGen {

    ArrayList<String> words = new ArrayList<String>();

    public static void main(String[] args) throws InterruptedException {

        //9
        ArrayList<String> words = wordsInit();

        Properties props = new Properties();
        props.put("bootstrap.servers", "192.168.19.4:9092");
        props.put("group.id", "groupIdName");
        props.put("enable.auto.commit", "false");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        // 创建多个线程，并行执行
        genBatch(props,words);
    }

    private static void genBatch(Properties props,ArrayList<String> words) {
        for(int i=0;i<2;i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // 构造一个kafka生产者客户端
                    KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(props);
                    while (true) {
                        // 写入kafka的topic： "test-topic"

                        Random rand=new Random();
                        int index = rand.nextInt(words.size() - 1);
                        String s = words.get(index);
                        //count为10到14范围闭区间的整数
                        int count = rand.nextInt(5)+10;
                        s=s+','+count;
                        System.out.println(s);
                        ProducerRecord<String, String> record = new ProducerRecord<>("test-topic", s);
                        kafkaProducer.send(record);
                        try {
                            Thread.sleep(rand.nextInt(500)+500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }
    public static ArrayList<String> wordsInit() {
        ArrayList<String> words=new ArrayList<String>();
        words.add("hadoop");
        words.add("spark");
        words.add("flink");
        words.add("kafka");
        words.add("zookeeper");
        words.add("presto");
        words.add("clickhouse");
        words.add("hive");
        words.add("Hudi");
        words.add("Hadoop");
        words.add("Spark");
        words.add("Flink");
        words.add("Kafka");
        words.add("Zookeeper");
        words.add("Presto");
        words.add("Clickhouse");
        words.add("Hive");
        words.add("Hudi");
        return words;
    }
}
