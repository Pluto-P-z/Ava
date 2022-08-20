package com.asoul.ava.messages.source;

import java.io.Serializable;
//不带触发时延的kafkaSource通知信息
public class KafkaSourceMsg implements Serializable {
    private final String topic;
    private final long size;

    public KafkaSourceMsg(String topic,long size) {
        this.topic=topic;
        this.size=size;
    }

    public String getTopic(){
        return this.topic;
    }
    public long getSize() {return this.size;}
}
