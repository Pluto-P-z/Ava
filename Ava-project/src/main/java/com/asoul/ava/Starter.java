package com.asoul.ava;

import java.io.File;
import java.io.IOException;
import java.util.*;

import akka.actor.*;

import com.asoul.ava.messages.create.*;
import com.asoul.ava.operators.*;
import com.asoul.ava.nodes.Master;
import com.asoul.ava.nodes.Sink;
import com.asoul.ava.nodes.Source;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;


public class Starter {
    protected static int shuffleFlag = 0;
    public static void main( String[] args ) throws InterruptedException, IOException {
        //主节点的地址，这里是默认的。实际程序运行是在HttpServer里面执行starterNode方法传入命令行解析来的参数
        String starterNodeURI = "akka.tcp://sys@127.0.0.1:6000";
        List<String> collaboratorNodesURI = Arrays.asList("akka.tcp://sys@127.0.0.1:6120",
                                                          "akka.tcp://sys@127.0.0.1:6121");

        try {
            //开启主节点，拿到算子链
            starterNode(starterNodeURI, collaboratorNodesURI, Job.jobOne.getOperators());
        } catch (RuntimeException ignored) {

        }

    }

    public static final ActorSystem starterNode(String starterNodeURI,
                                         List<String> collaboratorNodesURI,
                                         List<Operator> operators) throws InterruptedException {
        //初始化
        //添加主节点和从节点的地址
        ArrayList<Address> nodesAddr = new ArrayList<>();
        nodesAddr.add(AddressFromURIString.parse(starterNodeURI));

        for(String uri : collaboratorNodesURI) {
            nodesAddr.add(AddressFromURIString.parse(uri));
        }
        int numMachines = nodesAddr.size();

        //一个stage就是一个算子，一批运行在各个机器上的同一个算子
        int numStages = operators.size();

        //如果最后split之后没有merge


        // 设置配置参数
        final Config nodeConf = ConfigFactory.parseFile(new File("conf/node.conf"));

        String[] addr = starterNodeURI.split("@")[1].split(":");
        String ip = addr[0];
        String port = addr[1];

        Config starter = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port +"\n"
                                                    + "akka.remote.netty.tcp.hostname=" + ip);
        Config combined = starter.withFallback(nodeConf);
        Config complete = ConfigFactory.load(combined);

        //创建一个rpc系统
        final ActorSystem sys = ActorSystem.create("sys", complete);


        System.out.println("System created on starter node!" );

        //创建rpc节点Sink
        Map<Integer,ActorRef> sink = createSink(sys);
        System.out.println("Sink created!" );

        //创建rpc节点master
        final ActorRef master = sys.actorOf(Master.props(numMachines), "master");
        System.out.println("Master created!" );

        // Set sink as a downstream and change stage
        //先收到一个sink，把sink作为当前
        master.tell(new SinkMsg(sink.get(0)), ActorRef.noSender());
        master.tell(new ChangeStageMsg(), ActorRef.noSender());



        //posStage的作用是打印信息，知道当前是第几个算子
        int posStage = numStages + 1;


        //把算子的顺序翻转过来，按照反的顺序遍历，方便寻找每个stage的下游。
        List<Operator> reverseOperators = new ArrayList<>();
        for(Operator op : operators) {
            reverseOperators.add(op.clone());
        }
        Collections.reverse(reverseOperators);

        //先遍历算子再遍历机器，算子的遍历是反过来的
        for(Operator op : reverseOperators) {
            posStage--;
            boolean isLocal;
            String rootName = op.name;

            //对每个算子，遍历机器
            for(int i=0; i < numMachines; i++) {
                //判断计算节点部署在本机还是远程
                if(i==0) {
                    isLocal = true;
                } else {
                    isLocal = false;
                }



                op.name = rootName + "-" + Integer.toString(i+1);
                //进行运算逻辑，给master发送创建算子的消息，master就会创建相应的worker
                // Map
                if(op instanceof MapOperator) {
                    master.tell(new CreateMapMsg(op.name,  posStage, isLocal, nodesAddr.get(i),
                            ((MapOperator) op).fun,i,shuffleFlag), ActorRef.noSender());
                    if (shuffleFlag > 0){
                        shuffleFlag--;
                    }
                }
                // Filter
                else if(op instanceof FilterOperator) {
                    master.tell(new CreateFilterMsg(op.name, posStage, isLocal, nodesAddr.get(i),
                            ((FilterOperator) op).fun,i,shuffleFlag), ActorRef.noSender());
                    if (shuffleFlag > 0){
                        shuffleFlag--;
                    }
                }
                // Aggregate
                else if(op instanceof AggregateOperator) {
                    //下一批算子需要shuffle
                    shuffleFlag++;
                    master.tell(new CreateAggMsg(op.name, posStage, isLocal, nodesAddr.get(i),
                            ((AggregateOperator) op).fun,i,shuffleFlag), ActorRef.noSender());
                }
            }
            master.tell(new ChangeStageMsg(), ActorRef.noSender());
        }

        System.out.println(String.format("Operators created! Total number of stages: %d", numStages));


        //创建source
        final ActorRef source = sys.actorOf(Source.props(), "source");
        //source给master发消息SourceMsg，通知master初始化source
        master.tell(new SourceMsg(source), source);
        System.out.println("Source created!");
        return sys;
    }
    private static final Map<Integer,ActorRef> createSink(final ActorSystem sys) {
        return Collections.singletonMap(1,sys.actorOf(Sink.props(), "sink"));
    }

}
