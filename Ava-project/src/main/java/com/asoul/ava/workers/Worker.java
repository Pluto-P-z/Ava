package com.asoul.ava.workers;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import com.asoul.ava.messages.BatchMessage;
import com.asoul.ava.messages.Message;

import java.util.ArrayList;
import java.util.List;

//算子工作的actor基类
public abstract class Worker extends AbstractActor {
    protected int stagePos;
    protected List<ActorRef> downstream = new ArrayList<>();

    protected List<Message> batchQueue = new ArrayList<>();

    public Worker() {}

    public Worker(int stagePos, List<ActorRef> downstream) {

        this.stagePos = stagePos;
        this.downstream = downstream;

    }


    @Override
    public Receive createReceive() {
        return receiveBuilder() //
                .match(BatchMessage.class, this::onBatchMessage) //
                .build();
    }

    protected abstract void onBatchMessage(BatchMessage batchMessage);
}
