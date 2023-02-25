package com.asoul.ava.workers;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import com.asoul.ava.dto.WorkerMetaDataDto;
import com.asoul.ava.messages.BatchMessage;
import com.asoul.ava.messages.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//算子工作的actor基类
public abstract class Worker extends AbstractActor {
    protected int stagePos;
    protected int machineNumber;
    protected int shuffleFlag;
    protected Map<Integer,ActorRef> downstream = new HashMap<>();

    protected WorkerMetaDataDto workerMetaDataDto;
    protected List<Message> batchQueue = new ArrayList<>();

    public Worker() {}

    public Worker(int stagePos, Map<Integer,ActorRef> downstream,int machineNumber,int shuffleFlag) {
        this.stagePos = stagePos;
        this.downstream = downstream;
        this.machineNumber = machineNumber;
        this.shuffleFlag = shuffleFlag;
    }

    protected int getMachineNumber(){
        return this.machineNumber;
    }

    protected int getShuffleFlag(){
        return this.getShuffleFlag();
    }
    @Override
    public Receive createReceive() {
        return receiveBuilder() //
                .match(BatchMessage.class, this::onBatchMessage) //
                .build();
    }

    protected abstract void onBatchMessage(BatchMessage batchMessage);
}
