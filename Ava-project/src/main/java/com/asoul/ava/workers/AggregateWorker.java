package com.asoul.ava.workers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.Props;

import com.asoul.ava.functions.AggregateFunction;
import com.asoul.ava.messages.BatchMessage;
import com.asoul.ava.messages.Message;

//Aggregate算子工作的actor
public class AggregateWorker extends Worker {

    private final AggregateFunction fun;


    private final Map<Integer, List<String>> windows = new HashMap<>();

    public AggregateWorker( int stagePos, final List<ActorRef> downstream,
                            final AggregateFunction fun) {
        super( stagePos, downstream);

        this.fun = fun;
    }


    //处理一批数据（一个窗口）
    @Override
    protected void onBatchMessage(BatchMessage batchMessage) {
        //stagePoss是用来打印信息的
        System.out.println(self().path().name() + "(" + stagePos + ") received batch: " + batchMessage);


        // 遍历那一批的数据
        //需要有一个Map<key,List(value)>
        HashMap<String, List<String>> window = new HashMap<>();
        for(Message message : batchMessage.getMessages()) {
            // Get key and value of the message
            // 取出每一个message的key和value
            final String key = message.getKey();
            final String value = message.getVal();

            List<String> winValues = window.get(key);
            if (winValues == null) {
                winValues = new ArrayList<>();
                window.put(key, winValues);
            }
            winValues.add(value);
        }
        for (Map.Entry < String, List<String> > entry: window.entrySet()) {
            String word = entry.getKey();
            List<String> winValues = entry.getValue();
            final Message result = fun.process(word, winValues);
            batchQueue.add(result);
        }
        final int receiver = Math.abs(batchQueue.get(0).getKey().hashCode()) % downstream.size();
        downstream.get(receiver).tell(new BatchMessage(batchQueue), self());

        batchQueue.clear();

    }

    static public final Props props(int stagePos, List<ActorRef> downstream,
                                    final AggregateFunction fun) {
        return Props.create(AggregateWorker.class, stagePos, downstream, fun);
    }
}
