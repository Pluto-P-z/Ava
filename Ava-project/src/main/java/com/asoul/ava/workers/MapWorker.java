package com.asoul.ava.workers;

import java.util.List;

import akka.actor.ActorRef;
import akka.actor.Props;

import com.asoul.ava.functions.MapFunction;
import com.asoul.ava.messages.BatchMessage;
import com.asoul.ava.messages.Message;

//Map算子工作的actor
public class MapWorker extends Worker {
    private final MapFunction fun;

    public MapWorker( int stagePos, final List<ActorRef> downstream,
                     final MapFunction fun) {
        super( stagePos, downstream);
        this.fun = fun;
    }



    @Override
    protected final void onBatchMessage(BatchMessage batchMessage) {

        System.out.println(self().path().name() + "(" + stagePos + ") received batch: " + batchMessage);



        for(Message message : batchMessage.getMessages()) {
            final Message result = fun.process(message.getKey(), message.getVal());
            batchQueue.add(result);
        }
        final int receiver = Math.abs(batchQueue.get(0).getKey().hashCode()) % downstream.size();
        downstream.get(receiver).tell(new BatchMessage(batchQueue), self());

        batchQueue.clear();
    }

    public static Props props(int stagePos, List<ActorRef> downstream,
                               final MapFunction fun) {
        return Props.create(MapWorker.class, stagePos, downstream, fun);
    }


}
