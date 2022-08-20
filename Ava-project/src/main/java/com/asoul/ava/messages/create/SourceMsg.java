package com.asoul.ava.messages.create;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.List;
//创建Source的通知信息
public class SourceMsg implements Serializable {
    private ActorRef sourceRef;
    private List<ActorRef> downstream;

    public SourceMsg(ActorRef sinkRef) {
        this.sourceRef = sinkRef;
    }

    public SourceMsg(List<ActorRef> downstream) { this.downstream = downstream;}

    public ActorRef getSourceRef() {
        return sourceRef;
    }

    public List<ActorRef> getDownstream() { return downstream; }
}
