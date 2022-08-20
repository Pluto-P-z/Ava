package com.asoul.ava.workers;

import java.util.List;

import akka.actor.ActorRef;
import akka.actor.Props;

import com.asoul.ava.functions.FlatMapFunction;
import com.asoul.ava.messages.BatchMessage;
import com.asoul.ava.messages.Message;

//FlatMap算子工作的actor
public class FlatMapWorker extends Worker {
    private final FlatMapFunction fun;

    public FlatMapWorker(int stagePos, final List<ActorRef> downstream
                         , final FlatMapFunction fun) {
        super(stagePos, downstream);
        this.fun = fun;
    }


    //处理一批数据（一个窗口）
    @Override
    protected final void onBatchMessage(BatchMessage batchMessage) {

        System.out.println(self().path().name() + "(" + stagePos + ") received batch: " + batchMessage);


        for(Message message : batchMessage.getMessages()) {
            //进去每个message出来一个list
            final List<Message> result = fun.process(message.getKey(), message.getVal());
            //再把list展开放进batchQueue中
            for(Message m : result) {
                batchQueue.add(m);
            }
        }
        final int receiver = Math.abs(batchQueue.get(0).getKey().hashCode()) % downstream.size();
        downstream.get(receiver).tell(new BatchMessage(batchQueue), self());

        batchQueue.clear();
    }

    public static Props props(int stagePos, List<ActorRef> downstream,
                               final FlatMapFunction fun) {
        return Props.create(FlatMapWorker.class,  stagePos, downstream, fun);
    }


}

