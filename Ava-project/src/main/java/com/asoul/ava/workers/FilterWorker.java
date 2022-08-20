package com.asoul.ava.workers;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.asoul.ava.functions.FilterFunction;
import com.asoul.ava.messages.BatchMessage;
import com.asoul.ava.messages.Message;

import java.util.List;
//Filter算子工作的actor
public class FilterWorker extends Worker {
    private final FilterFunction fun;

    public FilterWorker(int stagePos, final List<ActorRef> downstream, final FilterFunction fun) {
        super(stagePos, downstream);
        this.fun = fun;
    }
    //处理一批数据（一个窗口）
    @Override
    protected void onBatchMessage(BatchMessage batchMessage) {

        System.out.println(self().path().name() + "(" + stagePos + ") received batch: " + batchMessage);



        for(Message message : batchMessage.getMessages()) {
            //根据判断函数判断是否要加进结果队列中
            final Boolean predicateResult = fun.predicate(message.getKey(), message.getVal());

            if(predicateResult) {
                batchQueue.add(message);
            }
        }
        final int receiver = Math.abs(batchQueue.get(0).getKey().hashCode()) % downstream.size();
        downstream.get(receiver).tell(new BatchMessage(batchQueue,batchMessage.getBatchInfo()), self());
        System.out.println("batchMessage.getBatchInfo():"+batchMessage.getBatchInfo());
        batchQueue.clear();
    }

    public static Props props( int stagePos, List<ActorRef> downstream,
                              final FilterFunction fun) {
        return Props.create(FilterWorker.class,stagePos, downstream, fun);
    }
}
