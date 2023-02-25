package com.asoul.ava.nodes;

import akka.actor.*;
import akka.japi.pf.DeciderBuilder;
import akka.remote.RemoteScope;

import com.asoul.ava.messages.create.*;
import com.asoul.ava.workers.*;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//管理worker的节点，负责worker的创建
public class Master extends AbstractActor {

    //变成map
    private HashMap<Integer,ActorRef> stage = new HashMap<>();
    private HashMap<Integer,ActorRef> oldStage = new HashMap<>();

    private boolean needShuffle = false;
    private int numMachines = 1;


    public Master(int numMachines) {
        this.numMachines = numMachines;
    }
    //master受到相应的消息，就创建相应的算子
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ChangeStageMsg.class, this::onChangeStage) //
                .match(SourceMsg.class, this::onReceiveSource) //
                .match(SinkMsg.class, this::onReceiveSink) //
                .match(CreateMapMsg.class, this::onCreateMapMsg) //
                .match(CreateAggMsg.class, this::onCreateAggMsg) //
                .match(CreateFilterMsg.class, this::onCreateFilterMsg) //
                .build();
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(//
                -1, //
                Duration.Inf(), //
                DeciderBuilder //
                        .match(RuntimeException.class, ex -> SupervisorStrategy.restart()) //
                        .build());
    }

    private void onChangeStage(ChangeStageMsg changeStageMsg) {
        oldStage = stage;
        stage = new HashMap<>();
    }
    //starter初始化的时候首先初始化sink，把sinkActor添加进stage，再change一次，sinckActor变成oldStage，stage又清空
    private void onReceiveSink(SinkMsg sinkMsg) {
        stage = new HashMap<>();
        //TODO 先写死看看，到时候要不要变成常量
        stage.put(1,sinkMsg.getSinkRef());
    }
    private void onReceiveSource(SourceMsg sourceMsg) {
        //给source发一条new SourceMsg(oldStage)
        sender().tell(new SourceMsg(oldStage), self());
    }





    //创建算子worker的actor
    private void onCreateMapMsg(CreateMapMsg  mapMsg) {
        ActorRef mapWorker;
        if (mapMsg.isLocal()) {
            mapWorker = getContext().actorOf(MapWorker.props(
                    mapMsg.getPosStage(),
                    //下面是downstream
                    stageDeepCopy(oldStage),
                    mapMsg.getFun(),mapMsg.getShuffleFlag()).withMailbox("recover-mailbox"), mapMsg.getName());
        } else {
            //创建actor
            mapWorker = getContext().actorOf(MapWorker.props(
                    mapMsg.getPosStage(),
                    stageDeepCopy(oldStage),
                    mapMsg.getFun(),mapMsg.getShuffleFlag()).withMailbox("recover-mailbox")
                    //在别的机器上创建节点
                    .withDeploy(new Deploy(new RemoteScope(mapMsg.getAddress()))), mapMsg.getName());
        }
        //把worker丢进stage里面
        updateStage(mapWorker, mapMsg.getMachineNumber());
    }

    private void onCreateAggMsg(CreateAggMsg aggMsg) {
        ActorRef aggWorker;
        needShuffle = true;
        if (aggMsg.isLocal()) {

            aggWorker = getContext().actorOf(AggregateWorker.props(
                    aggMsg.getPosStage(),
                    stageDeepCopy(oldStage),
                    aggMsg.getFun()
                ).withMailbox("recover-mailbox"), aggMsg.getName());
        } else {
            aggWorker = getContext().actorOf(AggregateWorker.props(
                    aggMsg.getPosStage(),
                    stageDeepCopy(oldStage),
                    aggMsg.getFun())
                    .withMailbox("recover-mailbox")
                    .withDeploy(new Deploy(new RemoteScope(aggMsg.getAddress()))), aggMsg.getName());

        }
        updateStage(aggWorker,aggMsg.getMachineNumber());
    }

    private void onCreateFilterMsg(CreateFilterMsg filterMsg) {
        ActorRef filterWorker;


        if (filterMsg.isLocal()) {
            filterWorker = getContext().actorOf(FilterWorker.props(
                    filterMsg.getPosStage(),
                    stageDeepCopy(oldStage),
                    filterMsg.getFun(),filterMsg.getMachineNumber(),filterMsg.getShuffleFlag()).withMailbox("recover-mailbox"), filterMsg.getName());
        } else {
            filterWorker = getContext().actorOf(FilterWorker.props(
                    filterMsg.getPosStage(),
                    stageDeepCopy(oldStage),
                    filterMsg.getFun(),filterMsg.getMachineNumber(),filterMsg.getShuffleFlag()).withMailbox("recover-mailbox")
                    .withDeploy(new Deploy(new RemoteScope(filterMsg.getAddress()))), filterMsg.getName());
        }

        updateStage(filterWorker,filterMsg.getMachineNumber());
    }



    private void updateStage(ActorRef actorRef,int machineNumber) {
            stage.put(machineNumber,actorRef);
    }

    private Map<Integer,ActorRef> stageDeepCopy(Map<Integer,ActorRef> stage) {
        Map<Integer,ActorRef> newStage = new HashMap<>();
        for (Integer key : stage.keySet()) {
            newStage.put(key,stage.get(key));
        }
        return newStage;
    }

    private void setNeedShuffle(boolean need){
        needShuffle = need;
    }

    public static Props props(int numMachines) {
        return Props.create(Master.class, numMachines);
    }
}
