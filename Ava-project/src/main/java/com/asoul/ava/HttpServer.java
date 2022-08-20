package com.asoul.ava;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Terminated;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.server.HttpApp;
import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.asoul.ava.messages.source.*;
import org.apache.commons.cli.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

//可以从8080端口发送请求

class HttpServer extends HttpApp {
    //在开始节点的ActorSystem
    private static ActorSystem system;
    // Static configuration
    private static String starterNodeURI = "akka.tcp://sys@127.0.0.1:6000";
    private static List<String> collaboratorNodesURI = Arrays.asList("akka.tcp://sys@127.0.0.1:6120",
                                                                     "akka.tcp://sys@127.0.0.1:6121");

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Options options = new Options();
        //主节点的ip和端口
        Option input = new Option("s", "starter", true, "ip:port of starter node");
        input.setRequired(false);
        options.addOption(input);
        //从节点
        Option output = new Option("c", "collabs", true, "ip1:port1,ip2:port2,..." +
                " list of collaborator nodes separated by a comma");
        output.setRequired(false);
        options.addOption(output);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }

        String starterNode = cmd.getOptionValue("starter");
        String starterIP = "localhost";

        if(starterNode != null) {
            starterIP = starterNode.split(":")[0];
            List<String> collabsNodes = Arrays.asList(cmd.getOptionValue("collabs").split(","));

            starterNodeURI = "akka.tcp://sys@".concat(starterNode);
            collaboratorNodesURI = new ArrayList<>();
            for(String c : collabsNodes) {
                collaboratorNodesURI.add("akka.tcp://sys@".concat(c));
            }
        }


        System.out.println("Starting the system...");
        //调用Starter类得到系统
        system = Starter.starterNode(starterNodeURI, collaboratorNodesURI, Job.jobOne.getOperators());

        //开启服务
        System.out.println(String.format("Server online at http://%s:8080/", starterIP));
        final HttpServer myHttpServer = new HttpServer();
        myHttpServer.startServer(starterIP, 8080);

    }


    private ActorRef getActorFromSystem(ActorSystem system, String actor) {
        try {
            ActorSelection actorSelection = system.actorSelection("/user/" + actor);
            Future<ActorRef> future = actorSelection.resolveOne(new FiniteDuration(10, TimeUnit.SECONDS));
            ActorRef actorRef = Await.result(future, new FiniteDuration(10, TimeUnit.SECONDS));
            return actorRef;

        } catch (Exception e) {
            System.out.println("Exception looking up actor!");
            return null;
        }
    }

    private void terminateActorSystem(ActorSystem system) {
        try {
            Future<Terminated> future = system.terminate();
            Terminated terminated = Await.result(future, new FiniteDuration(10, TimeUnit.SECONDS));
        } catch (Exception e) {
            System.out.println("Exception terminating actor system!");
        }
    }

    //路由

    @Override
    protected Route routes() {
        // SOURCE
        return concat(pathPrefix("source", () ->
                concat(

                        //带窗口的kafkaSource，用的是systemTime
                        path("kafka", () -> post(() ->  entity(
                                Jackson.unmarshaller(KafkaSource.class), kafkaSource -> {
                                    try {
                                        // 停止源进程
                                        ActorRef sourceRef = getActorFromSystem(system, "source");
                                        sourceRef.tell(new StopSourceMsg(), ActorRef.noSender());
                                        Thread.sleep(2000);

                                        String topic = kafkaSource.getTopic();
                                        long size = kafkaSource.getSize();
                                        sourceRef.tell(new KafkaSourceMsg(topic,size), ActorRef.noSender());

                                        return complete("Initialized new kafka source reading from topic: " + topic + "\n");

                                    } catch (Exception e) {
                                        return complete("Exception on initialize csv source!");
                                    }
                                }))),
                        //带窗口和触发时延的kafkaSource，用的是eventTime
                        path("kafka_event_with_delay", () -> post(() ->  entity(
                                Jackson.unmarshaller(KafkaSourceWithDelay.class), kafkaSourceWithDelay -> {
                                    try {
                                        // 停止源进程
                                        ActorRef sourceRef = getActorFromSystem(system, "source");
                                        sourceRef.tell(new StopSourceMsg(), ActorRef.noSender());
                                        Thread.sleep(2000);


                                        String topic = kafkaSourceWithDelay.getTopic();
                                        long size = kafkaSourceWithDelay.getSize();
                                        long delay =kafkaSourceWithDelay.getDelay();
                                        //给sourceActor发kafkaSource的消息，接下来应该跳转到nodes里面的Source进行匹配
                                        sourceRef.tell(new KafkaSourceMsgWithDelay(topic,size,delay), ActorRef.noSender());

                                        return complete("Initialized new kafka source with delay reading from topic: " + topic + "\n");

                                    } catch (Exception e) {
                                        return complete("Exception on initialize csv source!");
                                    }
                                })))
                            )

                        ),

                        concat(

                        path("job", () -> post(() -> entity(
                                Jackson.unmarshaller(JobId.class), jobId -> {
                                    try {
                                        System.out.println("Stopping previous job...");

                                        // 停止源进程
                                        ActorRef sourceRef = getActorFromSystem(system, "source");
                                        sourceRef.tell(new StopSourceMsg(), ActorRef.noSender());
                                        Thread.sleep(2000);

                                        // Terminate system
                                        terminateActorSystem(system);
                                        Thread.sleep(3000);

                                        // 开启新的job
                                        if(jobId.getId() == 1) {
                                            system = Starter.starterNode(starterNodeURI, collaboratorNodesURI,
                                                    Job.jobOne.getOperators());
                                        }

                                        return complete(String.format("Stopped previous Job. Started Job %d!\n",
                                                jobId.getId()));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        return complete("Exception while submitting new job.");
                                    }
                                }))
                        )

                    )
                );
    }



    private static class JobId {
        private final int id;

        @JsonCreator
        public JobId(@JsonProperty("id") int id) {
            this.id = id;
        }

        int getId() {
            return id;
        }

    }

    private static class SourceMode {
        private final String mode;

        @JsonCreator
        public SourceMode(@JsonProperty("mode") String mode) {
            this.mode = mode;
        }

        String getMode() {
            return mode;
        }
    }




    private static class KafkaSource {
        private String topic;
        private String size;

        @JsonCreator
        public KafkaSource(@JsonProperty("topic") String topic,@JsonProperty("size") String size) {
            this.topic = topic;
            this.size = size;
        }

        String getTopic() {
            return topic;
        }
        long getSize(){return Long.valueOf(size);}
    }
    private static class KafkaSourceWithDelay {
        private String topic;
        private String size;
        private String delay;

        @JsonCreator
        public KafkaSourceWithDelay(@JsonProperty("topic") String topic,@JsonProperty("size") String size,@JsonProperty("delay") String delay) {
            this.topic = topic;
            this.size = size;
            this.delay = delay;
        }

        String getTopic() {
            return topic;
        }
        long getSize(){return Long.valueOf(size);}
        long getDelay(){return Long.valueOf(delay);}
    }
}



