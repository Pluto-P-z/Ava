package com.asoul.ava.utils;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.System.currentTimeMillis;

public class test {
    public static void main(String[] args) throws Exception {
        KafkaSource kafkaSource = new KafkaSource("test-topic");
//        while (true){
//
//        }

        //
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date_string = sdf.format(new Date(currentTimeMillis() / 1000 * 1000L));
        StringBuilder sb = new StringBuilder();
        FileWriter writer = new FileWriter(new File("F:\\大数据求职资料\\小白做毕设\\akka-big-data\\akka-project\\data\\sink.csv"), true);
        sb.append(date_string);
        sb.append('\n');
        writer.write("Window end time:"+"date_string"+" windowsize:"+"size"+"s"+"\n");
        writer.close();
    }
}
