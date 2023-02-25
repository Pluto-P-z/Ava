package com.asoul.ava.workers;

import java.util.List;
import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.Props;

import com.asoul.ava.functions.MapFunction;
import com.asoul.ava.messages.BatchMessage;
import com.asoul.ava.messages.Message;

//Map算子工作的actor
public class MapWorker extends Worker {
    private final MapFunction fun;

    public MapWorker( int stagePos, final Map<Integer,ActorRef> downstream,
                     final MapFunction fun,int machineNumber,int shuffleFlag) {
        super( stagePos, downstream,machineNumber,shuffleFlag);
        this.fun = fun;
    }



    @Override
    protected final void onBatchMessage(BatchMessage batchMessage) {

        System.out.println(self().path().name() + "(" + stagePos + ") received batch: " + batchMessage);



        for(Message message : batchMessage.getMessages()) {
            final Message result = fun.process(message.getKey(), message.getVal());
            batchQueue.add(result);
        }
        if (this.getShuffleFlag() == 0){
            //找到本机的下一个算子。
            downstream.get(this.getMachineNumber()).tell(new BatchMessage(batchQueue,batchMessage.getBatchInfo()), self());
        }else{
            //TODO
            //写入shuffle中间文件
            //通知Master
            //Master通知下游读取
            //
        }

        System.out.println("batchMessage.getBatchInfo():"+batchMessage.getBatchInfo());
        batchQueue.clear();
    }

    public static Props props(int stagePos, Map<Integer,ActorRef> downstream,
                               final MapFunction fun,int shuffleFlag) {
        return Props.create(MapWorker.class, stagePos, downstream, fun,shuffleFlag);
    }


}
