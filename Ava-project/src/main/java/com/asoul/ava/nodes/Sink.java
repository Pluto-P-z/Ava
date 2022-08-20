package com.asoul.ava.nodes;

import java.io.*;

import akka.actor.AbstractActor;
import akka.actor.Props;

import com.asoul.ava.messages.BatchMessage;
import com.asoul.ava.messages.Message;
import com.asoul.ava.messages.stats.RequestStatsMsg;
import com.asoul.ava.messages.stats.StatsMsg;

public class Sink extends AbstractActor {
    private String filePath = "data/sink.csv";
    private boolean firstWrite = true;
    private boolean writeEnabled = true;

    private int total = 0;
    private int totalBatches = 0;

    public Sink() {}

    public Sink(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder() //
                .match(RequestStatsMsg.class, this::onRequestStatsMsg) //
                .match(BatchMessage.class, this::onBatchMessage) //
                .build();
    }

    protected void onRequestStatsMsg(RequestStatsMsg message) {
        try {
            StatsMsg result = new StatsMsg(self().path().name(), 0, total, 0,
                    totalBatches, 0, 0, 0);
            getSender().tell(result, getSelf());
        } catch (Exception e) {
            getSender().tell(new akka.actor.Status.Failure(e), getSelf());
            throw e;
        }
    }


    private final void onBatchMessage(BatchMessage batchMessage) {
        System.out.println("Sink received batch: " + batchMessage);
        totalBatches++;
        total += batchMessage.getMessages().size();
        System.out.println(String.format("Total batches: %d", totalBatches));
        System.out.println(String.format("Total messages: %d", total));

        // Write to csv
        if (writeEnabled) {
            for(Message message: batchMessage.getMessages()) {
                writeMessage(message);
            }
        }
    }

    private final void writeMessage(Message message) {
        try (FileWriter writer = new FileWriter(new File(filePath), true)) {

            StringBuilder sb = new StringBuilder();

            // If is first write, write header first
            if(firstWrite) {
                sb.append("key,");
                sb.append(',');
                sb.append("value");
                sb.append('\n');
                firstWrite = false;
            }

            // Write message on csv
            sb.append(message.getKey());
            sb.append(',');
            sb.append(message.getVal());
            sb.append('\n');

            writer.write(sb.toString());

        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static final Props props() {
        return Props.create(Sink.class);
    }
}
