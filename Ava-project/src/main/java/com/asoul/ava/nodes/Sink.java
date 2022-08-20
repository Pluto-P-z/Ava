package com.asoul.ava.nodes;

import java.io.*;

import akka.actor.AbstractActor;
import akka.actor.Props;

import com.asoul.ava.messages.BatchMessage;
import com.asoul.ava.messages.Message;

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
                .match(BatchMessage.class, this::onBatchMessage) //
                .build();
    }


    //写一批消息
    private final void onBatchMessage(BatchMessage batchMessage) throws Exception{
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
        System.out.println("batchMessage.getBatchInfo():"+batchMessage.getBatchInfo());
        FileWriter writer = new FileWriter(new File(filePath), true);
        StringBuilder sb = new StringBuilder();
        sb.append(batchMessage.getBatchInfo());
        sb.append('\n');
        writer.write(sb.toString());
        writer.close();
    }
    //写一条消息
    private final void writeMessage(Message message) {
        try (FileWriter writer = new FileWriter(new File(filePath), true)) {

            StringBuilder sb = new StringBuilder();

            // 第一次写
            if(firstWrite) {
                sb.append("key");
                sb.append(',');
                sb.append("value");
                sb.append('\n');
                firstWrite = false;
            }

            // 写入数据
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
