package main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.clientmodel.ClientKVReq;
import model.colconfig.BaseColConfig;
import model.colconfig.robotcolconfig.RobotColConfig;
import model.node.NodeConfig;
import model.rpcmodel.Request;
import node.INode;
import node.NodeImpl;
import rpc.impl.RpcClientImpl;
import util.kafka.KafkaService;

import java.util.Arrays;

public class BootStrap {
    private static final Gson gson = new GsonBuilder().create();      /** object和json字符串转换 */
//    public static final String NODE01 = "192.168.0.26:8775";
//    public static final String NODE02 = "192.168.0.26:8776";
//    public static final String NODE03 = "192.168.0.42:8775";
//    public static final String NODE04 = "192.168.0.42:8776";
//    public static final String NODE05 = "192.168.0.46:8775";

    public static final String NODE01 = "localhost:8775";
    public static final String NODE02 = "localhost:8776";
    public static final String NODE03 = "localhost:8777";
    public static final String NODE04 = "localhost:8778";
    public static final String NODE05 = "localhost:8779";
    public static final boolean isClient = false;

    public static void main(String args[]) throws Throwable {
        if(isClient) {
            ClientMain(args);
        } else {
            String ipAddr = "localhost";
            int port = 8775;

            if(args.length == 2){
                ipAddr = args[0];
                port = Integer.parseInt(args[1]);
            }
            GraduateMain(ipAddr, port);
        }
    }

    /** 用于模拟客户端的启动程序 */
    public static void ClientMain(String[] args) {
        RpcClientImpl rpcClient = new RpcClientImpl();
        for(int i=0;i<5;i++) {
            String deviceId = "end_robot_0" + i;
            RobotColConfig robotColConfig = RobotColConfig.newBuilder()
                    .connectRole(BaseColConfig.CLIENT)
                    .description("人生第"+(i+1)+"次采集")
                    .deviceId(deviceId)
                    .frequency(100) // 100hz
//                    .ipAddr("192.168.0.65")
                    .ipAddr("192.168.3.188")
                    .port(8888)
                    .protocol("tcp")
                    .build();

            ClientKVReq clientKVReq = ClientKVReq.newBuilder()
                    .type(0)
                    .key(deviceId)
                    .value(gson.toJson(robotColConfig))
                    .build();
            Request request = Request.newBuilder()
                    .cmd(Request.CLIENT_REQ)
                    .obj(clientKVReq)
                    .url(NODE01)
                    .build();
            rpcClient.send(request);

            try{
                Thread.sleep(1000);
            }catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /** 毕设正常入口 */
    public static void GraduateMain(String ipAddr,int port) throws Throwable {
        String []peerAddrs = {
                NODE01,NODE02,NODE03,NODE04,NODE05
        };

        /** 第一步配置相关参数 */
        NodeConfig nodeConfig = new NodeConfig();
        nodeConfig.setSelfPort(port);
        nodeConfig.setSelfIpAddr(ipAddr);
        nodeConfig.setPeerAddrs(Arrays.asList(peerAddrs));

        INode node = NodeImpl.getInstance();
        node.setConfig(nodeConfig);

        /**
         * 第二步执行init操作
         *	init执行相应操作操作：
         * 1. 开启rpcserver服务
         * 2. 开启一个心跳线程，不断轮询，每次执行判断自己是不是leader 不是就不做了
         * 3. 开启一个选举线程，不断轮询，每次执行判断自己是不是candidate 不是就不做了
         */
        node.init();

        /**
         * 当jvm关闭的时候会执行系统中已经设置的所有通过方法addShutdownHook添加的钩子
         * 当系统执行完这些任务（比如下面的node.destroy())后，jvm才会关闭，
         * 所以这个钩子函数可以用来进行内存的清理，对象销毁的操作
         * 多适用于内存清理和对象销毁
         */
        Runtime.getRuntime().addShutdownHook(
                new Thread( () -> {
                    try
                    {
                        // 关闭Kafka
                        KafkaService.close();
                        node.destroy();
                    }
                    catch (Throwable e)
                    {
                        e.printStackTrace();
                    }
                }	));
    }
}