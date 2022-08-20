package com.asoul.ava.nodes;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import akka.actor.Props;
import com.asoul.ava.messages.BatchMessage;
import com.asoul.ava.messages.Message;
import com.asoul.ava.messages.create.SourceMsg;
import com.asoul.ava.messages.source.*;
import com.asoul.ava.utils.KafkaSource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

public class Source  extends AbstractActor {

    private List<ActorRef> downstream = new ArrayList<>();


    private int sleepTime = 400;

    private volatile boolean running = true;
    private volatile boolean suspended = false;

    private int total = 0;


    private List <Message> batchQueue = new ArrayList<>();
    private List <Message> nextbatchQueue = new ArrayList<>();

    private long startTime=0;
    private long delay=0;
    private long waterMark=0;


    public Source() {
        super();
    }

    public Source(final List<ActorRef> downstream, int sleepTime,long startTime,long delay) {
        this.downstream = downstream;
        this.sleepTime = sleepTime;
        this.startTime=startTime;
        this.delay = delay;
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder() //
                .match(SourceMsg.class, this::setDownstream) //
                .match(StopSourceMsg.class, this::stopSource) //
                .match(KafkaSourceMsg.class, this::onKafkaMsg)//
                .match(KafkaSourceMsgWithDelay.class, this::onKafkaMsgWithDelay)//
                .build();
    }


    private void setDownstream(SourceMsg sourceMsg) {
        this.downstream = sourceMsg.getDownstream();
    }




    private void stopSource(StopSourceMsg message) { running = false; }



    //从Kafka数据源读数据的
    public void onKafkaMsg(KafkaSourceMsg message){
        running = true;
        suspended = false;
        new Thread(() -> {
            startTime = System.currentTimeMillis() / 1000;
            while(running) {
                KafkaSource kafkaSource = new KafkaSource(message.getTopic());
                KafkaConsumer<String, String> consumer = kafkaSource.getConsumer();
                long size = message.getSize();
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(100);
                    if (!suspended) {
                        try {
                            for (ConsumerRecord<String, String> record : records) {
                                //下面写处理逻辑
                                String kfkLine = record.value();
                                if (kfkLine.length() != 0) {
                                    readWindowMessage(kfkLine,size);
                                }
                                //提交偏移量，要不然会一直重复消费
                                consumer.commitSync();
                            }
                        } catch (InterruptedException e) {
                            running = false;
                        }
                    }
                }
            }
        }).start();
    }
    //带偏移量和事件时间的KafkaSource
    public void onKafkaMsgWithDelay(KafkaSourceMsgWithDelay message){
        running = true;
        suspended = false;
        new Thread(() -> {
            System.out.println("reach onKafkaMsgWithDelay");
            delay = message.getDelay();
            while(running) {
                KafkaSource kafkaSource = new KafkaSource(message.getTopic());
                KafkaConsumer<String, String> consumer = kafkaSource.getConsumer();
                long size = message.getSize();
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(100);
                    if (!suspended) {
                        try {
                            for (ConsumerRecord<String, String> record : records) {
                                //下面写处理逻辑
                                String kfkLine = record.value();
                                if (kfkLine.length() != 0) {
                                    readWindowMessageWithDelay(kfkLine,size);
                                }
                                //提交偏移量，要不然会一直重复消费
                                consumer.commitSync();
                            }
                        } catch (Exception e) {
                            running = false;
                        }
                    }
                }
            }
        }).start();
    }
    //配套的读取不带delay的window消息
    private void readWindowMessage(String line,long size) throws InterruptedException {
        String [] content =line.trim().split(",");
        String key = content[0];
        String value = content[1];

        Message msg = new Message(key, value);
        sendWindowMessage(msg,size);
    }
    //配套的读取带delay的window消息
    private void readWindowMessageWithDelay(String line,long size) throws Exception {
        String [] content =line.trim().split(",");
        String time = content[0];
        String key = content[1];
        String value = content[2];

        Message msg = new Message(time + "," + key, value);
        sendWindowMessageWithDelay(msg,size);
    }

    /**
     * 发送带有触发时延的滚动窗口消息
     * @param msg 发送的消息
     * @param size 窗口的大小
     */
    public void sendWindowMessageWithDelay(Message msg,long size) throws Exception {
        //watermark和当前message的事件时间比较，更新watermark
        long eventTime = Long.parseLong(msg.getKey().split(",")[0]);
        System.out.println("eventTime:"+eventTime);
        //重新封装一个Message对象
        Message m = new Message(msg.getKey().split(",")[1],msg.getVal());
        waterMark = Math.max(waterMark,eventTime);
        //如果判断时间超过了窗口的结束
        //触发窗口的条件换成了当前窗口的结束时间+触发时延<=watermark
        System.out.println("startTime:"+startTime);
        //在下一个窗口范围内，但是>watermark
        if(startTime+size+delay<=waterMark&&startTime+size*2>waterMark){
            //watermark落到下个window并触发该次window的计算，注意当前元素添加进batchQueue
            delayWindowTrigger();
            printTimeInfo(size);
            System.out.println("batchQueue=nextbatchQueue");
            batchQueue.clear();
            for (Message message : nextbatchQueue) {
                batchQueue.add(message);
            }
            System.out.println("nextbatchQueueClear");
            nextbatchQueue.clear();
            System.out.println("batchQueueadd");
            batchQueue.add(m);
            startTime=startTime+size;
        }
        //watermark落到下下个window但还没触发,触发该次window
        else if(startTime+size*2<=waterMark&&startTime+size*2+delay>waterMark){
            delayWindowTrigger();
            printTimeInfo(size);
            System.out.println("batchQueue=nextbatchQueue");
            batchQueue.clear();
            for (Message message : nextbatchQueue) {
                batchQueue.add(message);
            }
            System.out.println("nextbatchQueueClear");
            nextbatchQueue.clear();
            System.out.println("nextbatchQueueadd");
            nextbatchQueue.add(m);
            startTime=startTime+size;
        }
        else if(waterMark>=startTime+size*2+delay){
            delayWindowTrigger();
            printTimeInfo(size);
            startTime=startTime+size;
            System.out.println("batchQueue=nextbatchQueue");
            batchQueue.clear();
            for (Message message : nextbatchQueue) {
                batchQueue.add(message);
            }
            delayWindowTrigger();
            printTimeInfo(size);
            //把startTime更新至比waterMark小的最大的窗口
            while (startTime+size<waterMark){
                startTime=startTime+size;
            }
            batchQueue.clear();
            nextbatchQueue.clear();
            batchQueue.add(m);
        }
        else{
           //添加进本窗口
            if(startTime+size>eventTime&&eventTime>=startTime){
                System.out.println("batchQueue.add");
                batchQueue.add(m);
            }
            //添加进下一个窗口
            else if(eventTime>=startTime+size&&eventTime<startTime+size+delay){
                System.out.println("nextbatchQueue.add");
                nextbatchQueue.add(m);
            }
        }
    }
    //因为网络传输的时延问题，会出现打印窗口信息先于窗口数据出现，以及连续打印两个窗口信息，再连续打印两个窗口的数据，所以修改代码结构拆成两个方法Z加个锁试试
    //窗口触发的函数，把当前窗口清空
    public synchronized void delayWindowTrigger(){
        //把该批数据发往下游
        if(batchQueue.size()!=0) {
            BatchMessage batchMsg = new BatchMessage(batchQueue);
            final int receiver = Math.abs(batchQueue.get(0).getKey().hashCode()) % downstream.size();
            //发送到下游
            downstream.get(receiver).tell(batchMsg, self());

            //打印这一批消息的信息

            total += batchMsg.getMessages().size();
            System.out.println("Source sending batch " + batchMsg);
            System.out.println(String.format("Total messages: %d", total));
        }
    }
    public synchronized void printTimeInfo(long size) throws Exception {
        //startTime是当前窗口的开始时间
        Thread.sleep(size/2*1000);
        long end = startTime+size;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date_string = sdf.format(new Date(end * 1000L));
        try {
            FileWriter writer = new FileWriter(new File("/opt/apps/Ava/Ava-project/data/sink.csv"), true);
            String info = "Window end time:"+date_string+" windowsize:"+size+"s"+" delay:"+delay+" cur_watermark:"+waterMark+"\n";
            System.out.println("Window end time:"+date_string+" windowsize:"+size+"s"+" delay:"+delay+" cur_watermark:"+waterMark+"\n");
            writer.write(info);
            writer.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendWindowMessage(Message msg,long size){
        long curTime = System.currentTimeMillis() / 1000;
        //如果当前时间超过了窗口的结束时间，窗口关闭
        if(startTime+size<=curTime){

            long end = startTime+size;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String date_string = sdf.format(new Date(end * 1000L));
            //更新startTime,为<=currentTime的最大的开始时间
            while (startTime+size<=curTime){
                startTime=startTime+size;
            }
            //把该批数据发往下游
            if(batchQueue.size()!=0) {
                BatchMessage batchMsg = new BatchMessage(batchQueue);
                final int receiver = Math.abs(batchQueue.get(0).getKey().hashCode()) % downstream.size();
                downstream.get(receiver).tell(batchMsg, self());

                //打印这一批消息的信息

                total += batchMsg.getMessages().size();
                System.out.println("Source sending batch " + batchMsg);
                System.out.println(String.format("Total messages: %d", total));

                //清空队列，加入当前一条新的消息
                batchQueue.clear();
                batchQueue.add(msg);
                try {
                    FileWriter writer = new FileWriter(new File("/opt/apps/Ava/Ava-project/data/sink.csv"), true);
                    String info = "Window end time:"+date_string+" windowsize:"+size+"s"+"\n";
                    System.out.println("Window end time:"+date_string+" windowsize:"+size+"s"+"\n");
                    writer.write(info);
                    writer.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else{
            //不触发窗口，只添加消息就好
            batchQueue.add(msg);
        }
    }

    // 使用props创建Source

    public static final Props props() {
        return Props.create(Source.class);
    }

}
