package com.asoul.ava;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.io.IOException;

public class Collaborator {
    public static void main( String[] args ) throws InterruptedException, IOException {
        //如果从外界传入了参数，就从参数中取出端口号等信息，否则加载默认参数
        if (args.length > 0) {
            String[] addr = args[0].split(":");
            String ip = addr[0];
            String port = addr[1];


            final Config nodeConf = ConfigFactory.parseFile(new File("conf/node.conf"));

            Config collab = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port +"\n"
                    + "akka.remote.netty.tcp.hostname=" + ip);

            Config combined = collab.withFallback(nodeConf);
            Config complete = ConfigFactory.load(combined);
            //最终都是调用这个函数创建一个Collaborator节点
            collaboratorNode(complete);
        }

        // 加载默认参数
        else {
            String configPath = "conf/collaborator.conf";
            Config collabConfig = ConfigFactory.parseFile(new File(configPath));
            collaboratorNode(collabConfig);
        }

    }

    public static final void collaboratorNode(Config collabConfig) {

        final ActorSystem sys = ActorSystem.create("sys", collabConfig);
        System.out.println( "System created on collaborator node!");

    }

}


