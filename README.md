AvavaAvA,这里是字节跳动青训营Done当当Done小组开发的项目选题2——流式计算引擎Ava

项目介绍&答辩文档链接https://juejin.cn/post/7136086713771753485
一、项目介绍

项目核心信息：基于java和akkaRPC框架实现的分布式流式计算引擎，可以实现带有触发时延的窗口wordcount应用。
项目服务地址-必须
Github 地址：github.com/Pluto-P-z/A…

二、项目分工


团队成员主要贡献张奕旻负责技术选型，代码开发，测试李妍妍负责技术选型，查阅资料，路线尝试周陈文浩参与讨论
三、项目实现
3.1 技术选型与相关开发文档
语言:java
通信协议：RPC框架:Akka
3.2 架构设计
![image](https://user-images.githubusercontent.com/76391553/187567732-ff5a43ca-43d0-4ce9-bd13-a3ce01103dbc.png)

集群启动以后，通过Http服务发送命令控制集群的任务运行。主节点启动HttpServer程序，接受命令之后调用Starter中的函数创建主节点的ActorSystem。在Starter中ActorSystem创建Source和Sink，Master,发送消息给Master，由Master在主节点和从节点上创建Worker，Worker中包含有算子和计算逻辑。并初始化Source和Sink，接收到数据进行计算。一个并行的算子就是一个Stage。
3.3 项目代码介绍
整体目录：

3.3.1 conf
conf：RPC框架Akka的各个节点(Actor)参数配置：包括传输协议(tcp)，序列化方式等。
3.3.2 data
存放数据文件：主要是sink的文件。
3.3.3 datagen
数据生成器：持续随机生成指定范围指定格式的数据发往KafkaTopic
test-topic。
数据格式：
KafkaDataGen:word,count
KafkaDataGenWithTs:timestamp,word,count
word范围:
随机生成范围为如下的单词，注意单词区分大小写，用于在任务中测试我们用map函数将单词统一变成小写。
hadoop,spark,flink,kafka,zookeeper
,presto,clickhouse,hive,Hudi,
Hadoop,Spark,Flink,Kafka,Zookeeper,
Presto,Clickhouse,Hive,Hudi

count:
//count为10到14范围闭区间的整数
int count = rand.nextInt(5)+10;

timestamp:
//当前时间随机+0-29s,用于测试乱序数据延迟触发的窗口
int ts = rand.nextInt(30)+(int)(System.currentTimeMillis() / 1000);
复制代码
3.3.4 functions
计算函数函数的接口
包含map(映射算子)，aggregate(聚合算子)，filter(过滤算子)三种算子，用户可以使用该接口自定义算子的计算逻辑。
3.3.5 mailboxes
Akka信箱设置
3.3.6 messages-create
封装通知创建负责各种功能的Acotr节点节点的消息。
3.3.7 messages-source
封装数据源的通知信息
3.3.8 messages-BatchMessage&Message
用于计算的数据抽象。BatchMessage为一个窗口的数据，封装有
List< String >的Message
3.3.9 nodes
Akka的Actor节点的封装
Master：
再Starter类中被创建的Actor主节点，负责Worker节点，Source节点，Sink节点的创建和管理。当接收到messages-create封装的创建Acotr节点的消息时，就会根据messages-create的类型创建不同类型的Actor节点(Worker)，或初始化Source或Sink节点。
Source:
在Starter类中被创建的负责管理数据源的Actor节点，当接收到Master节点的消息时，设置不同的数据源的数据格式。
Sink：
在Starter类中被创建的Sink节点，当接收到Worker节点的消息时，把消息写入文件。
3.3.10 operators
是3.3.4 functions的封装，为算子链中的算子对象。
3.3.11 utils
KafkaSource:写了测试时Kafka集群的配置，获取Kafka的Consumer，在Source节点被调用拉取数据。
3.3.12 workers
负责计算的Actor节点，包含map(映射算子)，aggregate(聚合算子)，filter(过滤算子)三种worker。运行时会由Master在每台机器上创建，接收上游算子数据计算之后发往下游。
3.3.13 Collaborator
启动从机器上应用程序的主进程，在从机器上创建
ActorSystem。
3.3.14 HttpServer
启动主机器上应用程序的主进程，在主机器上创建
通过Starter类创建ActorSystem，Master，Sink，Source。用户可通过http的POST请求设置Job，Source的类型。
3.3.15 Job
封装了用户自定义的Job，一个Job为一个用户自定义的算子链。这里演示测试中用到的Job的示例。
public static final Job jobOne = new Job(Arrays.asList(
        //先全部变成小写
        new MapOperator("Map",(MapFunction & Serializable)(String k,String v)->{
            String newKey = k.toLowerCase();
            return new Message(newKey,v);
            }),
        //保留h开头的key的kv对
        new FilterOperator("Filter",(FilterFunction & Serializable) (String k,String v)-> {
            if(k.startsWith("h")){
                return true;
            }
            else {
                return false;
            }
        }),
        //聚合
        new AggregateOperator(  "Aggregate",(AggregateFunction & Serializable) (String k, List<String> vs)->{
            int sum = 0;
            for(int i=0;i<vs.size();i++){
                sum+=Integer.parseInt(vs.get(i));
            }
            return new Message(k,sum+"");
        }
        )
        ), "jobOne");
复制代码
3.3.16 Starter
创建主进程的ActorSystem，Actor节点Master，Sink，Source。给Master发送创建算子链的消息让其创建算子链。管理算子链的上下游逻辑(Stage的关系,这里的一个Stage就是一个算子，而不是像Spark出现shuffle才划分Stage)。给Master发送初始化Source和Sink的消息。
3.3.17 APIS
运行项目的命令。
四、项目部署&测试
项目部署环境为至少两台linux主机。本测试使用的是3台Centos7的linux主机，2主1从的节点设置。
将项目打包成zip，通过rz命令传入linux，分发给各个节点。

在三台机子的Ava-project目录分别打包项目：mvn package
分别命令
mvn exec:java -Dexec.mainClass="com.asoul.ava.Collaborator" -Dexec.args="192.168.19.3:6120"
mvn exec:java -Dexec.mainClass="com.asoul.ava.Collaborator" -Dexec.args="192.168.19.5:6121"
mvn exec:java -Dexec.mainClass="com.asoul.ava.HttpServer" -Dexec.args="-s 192.168.19.4:6000 -c 192.168.19.3:6120,192.168.19.5:6121"

测试1：
发送HTTP请求，启动job2
curl -d '{"id":"2"}' -H "Content-Type: application/json" -X POST http://192.168.19.4:8080/job

发送HTTP请求，指定KafkaTopic，窗口大小为10s,触发时延为5s
curl -d '{"topic":"test-topic","size":10,"delay":5}' -H "Content-Type: application/json" -X POST http://192.168.19.4:8080/source/kafka_event_with_delay

将如下数据复制到KafkaProducer客户端
10,hadoop,1 
12,spark,1	
11,hadoop,2
13,spark,3
15,flink,1 
16,flink,3
18,flink,2
24,presto,1 
18,hadoop,3
17,hadoop,2 
25,spark,2 
28,flink,2
26,flink,2
36,presto,1
40,hive,1
44,hive,3
100,presto,1
1659869756,hadoop,1 
1659869754,spark,1	
1659869759,hadoop,2
1659869760,spark,3
1659869756,flink,1 
1659869765,flink,3
1659869764,flink,2
1659869763,presto,1 
1659869770,hadoop,3
1659869772,hadoop,2 
1659869775,spark,2 
1659869780,flink,2
1659869794,flink,2
1659869792,presto,1
1659869790,hive,1
1659869835,hive,3
1659869850,presto,1

测试结果：
key,value
flink,6
spark,4
hadoop,8
Window end time:1970-01-01 08:00:20 windowsize:10s delay:5 cur_watermark:25

flink,4
spark,2
presto,1
Window end time:1970-01-01 08:00:30 windowsize:10s delay:5 cur_watermark:36

presto,1
Window end time:1970-01-01 08:00:40 windowsize:10s delay:5 cur_watermark:100

hive,4
Window end time:1970-01-01 08:00:50 windowsize:10s delay:5 cur_watermark:100

presto,1
Window end time:1970-01-01 08:01:40 windowsize:10s delay:5 cur_watermark:1659869756

flink,1
spark,1
hadoop,3
Window end time:2022-08-07 18:56:00 windowsize:10s delay:5 cur_watermark:1659869765

flink,5
spark,3
presto,1
Window end time:2022-08-07 18:56:10 windowsize:10s delay:5 cur_watermark:1659869775

spark,2
hadoop,5
Window end time:2022-08-07 18:56:20 windowsize:10s delay:5 cur_watermark:1659869794

flink,2
Window end time:2022-08-07 18:56:30 windowsize:10s delay:5 cur_watermark:1659869835

flink,2
hive,1
presto,1
Window end time:2022-08-07 18:56:40 windowsize:10s delay:5 cur_watermark:1659869835

hive,3
Window end time:2022-08-07 18:57:20 windowsize:10s delay:5 cur_watermark:1659869850

测试2：使用job1，由3个算子组成，map算子把单词开头映射成小写，filter算子过滤出h开头的单词，aggregate算子做求和运算。
测试结果：
key,value
hive,22
hudi,60
hadoop,49
Window end time:2022-08-27 08:07:00 windowsize:30s delay:5 cur_watermark:1661558827

hive,58
hudi,35
hadoop,82
Window end time:2022-08-27 08:07:30 windowsize:30s delay:5 cur_watermark:1661558855

hive,58
hudi,83
hadoop,119
Window end time:2022-08-27 08:08:00 windowsize:30s delay:5 cur_watermark:1661558885

hive,137
hudi,59
hadoop,100
Window end time:2022-08-27 08:08:30 windowsize:30s delay:5 cur_watermark:1661558916

hive,122
hudi,40
hadoop,65
Window end time:2022-08-27 08:09:00 windowsize:30s delay:5 cur_watermark:1661559137

hive,12
hadoop,10
Window end time:2022-08-27 08:12:30 windowsize:30s delay:5 cur_watermark:1661559156

hive,143
hudi,52
hadoop,59
Window end time:2022-08-27 08:13:00 windowsize:30s delay:5 cur_watermark:1661559185

hive,92
hudi,34
hadoop,84
Window end time:2022-08-27 08:13:30 windowsize:30s delay:5 cur_watermark:1661559221
复制代码
从节点截图：

主节点截图：

五、演示 Demo （必须）
视频演示demo
六、项目总结与反思

1.目前仍存在的问题:
支持的数据结构较为单一，只允许keyvalue的数据结构。不支持复杂数据结构人为指定key的操作。容错性能不好，exaclyonlyonce语义无法实现，单批数据无法并行计算，只能随机发往下游的某一个节点。
2.已识别的优化项：akka框架不稳定，机器之间容易断开连接，还需要做好容错措施。
3.架构演进的可能性:
改进算子上下游逻辑，满足同一批数据并行计算,而不是统一随机发往某个节点进行运算。可以对算子进行分类，分成shuffle算子和非shuffle算子，非shuffle算子就直接在当前节点上进行运算，而不是随机发往下游的其他节点，相当于相邻的算子之间都要shuffle。这样改进可以避免多余的网络传输。
4.项目过程中的反思与总结：整个项目收获还是不小的，查找资料到讨论技术选型，阅读相关代码，进行开发和测试都对我起到锻炼作用。感谢组员的配合和支持。在项目的开始，我组织组员对flink的源码进行学习，李妍妍同学则查找其他流式计算引擎的架构思路。我们经常讨论，根据我们的能力和知识储备讨论方案的可行性。由于之前对于分布式系统的通信没有涉猎，我们借鉴了一个基于akka实现的项目github.com/tmscarla/ak… ，并在其基础上完成了我们任务所需要的逻辑。工程的复杂度是超乎我们想象的，随着运行时间的增加，数据量的增大，可能出现的错误都是我们难以预料的。在实际的工程开发中我们应该更加严谨，一丝不苟。
