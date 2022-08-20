package com.asoul.ava.messages.create;

import akka.actor.ActorRef;

import java.io.Serializable;
//创建Sink的消息
public class SinkMsg implements Serializable {
    private ActorRef sinkRef;

    public SinkMsg(ActorRef sinkRef) {
        this.sinkRef = sinkRef;
    }

    public ActorRef getSinkRef() {
        return sinkRef;
    }
}
