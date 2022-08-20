package com.asoul.ava.messages.source;

import java.io.Serializable;
//带触发时延的kafkaSource通知信息
public class KafkaSourceMsgWithDelay implements Serializable {
    private final String topic;
    private final long size;
    private long delay;

    public KafkaSourceMsgWithDelay(String topic,long size,long delay) {
        this.topic=topic;
        this.size=size;
        this.delay=delay;
    }

    public String getTopic(){
        return this.topic;
    }
    public long getSize() {return this.size;}
    public long getDelay() {return this.delay;}
}