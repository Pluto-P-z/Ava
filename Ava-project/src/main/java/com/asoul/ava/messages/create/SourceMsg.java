package com.asoul.ava.messages.create;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

//创建Source的通知信息
public class SourceMsg implements Serializable {
    private ActorRef sourceRef;
    private Map<Integer,ActorRef> downstream;

    public SourceMsg(ActorRef sinkRef) {
        this.sourceRef = sinkRef;
    }

    public SourceMsg(Map<Integer,ActorRef> downstream) { this.downstream = downstream;}

    public ActorRef getSourceRef() {
        return sourceRef;
    }

    public Map<Integer,ActorRef> getDownstream() { return downstream; }
}
