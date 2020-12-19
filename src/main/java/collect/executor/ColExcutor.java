package collect.executor;

import collect.driver.IDeviceDriver;
import collect.driver.robotdriver.IRobotDriver;
import collect.driver.robotdriver.impl.RokaeRobotDriverImpl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import concurrent.col.ColThread;
import concurrent.col.ColThreadPool;
import exception.collectingexception.ConnectInterruptedException;
import model.colconfig.BaseColConfig;
import model.colconfig.robotcolconfig.RobotColConfig;
import model.kafkamsg.axlemsg.AxleDynMsg;
import model.kafkamsg.robotmsg.RobotDynMsg;
import model.logmodulemodel.LogEntry;
import node.NodeImpl;
import util.kafka.KafkaService;

/**
 *  1. init 如果失败，说明任务问题，leader会直接删除
 *  2. run  如果失败，不做处理等待leader重试
 */
public class ColExcutor {
    private ColExcutor(){}
    private static class NodeLazyHolder {
        private static final ColExcutor instance = new ColExcutor();
    }
    public static ColExcutor getInstance() {
        return ColExcutor.NodeLazyHolder.instance;
    }

    public static final int COLLECTSUCCESS = 100;
    public static final int DEVCONNECTFAIL = 101;
    public static final int PEERCONNFAIL = 201;

    public static final int ROBOT = 1;
    public static final int SENSOR = 2;

    private static final Gson gson = new GsonBuilder().create();      /** object和json字符串转换 */

    public int startCollect(LogEntry logEntry, int deviceType) {
        switch (deviceType) {
            case ROBOT:{
                BaseColConfig baseColConfig = gson.fromJson(logEntry.getCommand().getValue(), RobotColConfig.class);
                if(baseColConfig.getFrequency() == 0)
                    baseColConfig.setFrequency(1);
                IRobotDriver iRobotDriver = new RokaeRobotDriverImpl();
                if (!iRobotDriver.init(baseColConfig)) {
                    // 机器人连接失败
                    return DEVCONNECTFAIL; //"101:Device connection failed";
                } else {
                    // 机器人连接成功 异步执行
                    ColThread colThread = new ColThread(logEntry.getCommand().getKey(),() -> {
                        StartCollecting(baseColConfig,iRobotDriver,ROBOT,logEntry);
                    });
                    colThread.setLogEntry(logEntry);
                    ColThreadPool.execute(colThread,false);
                }
                break;
            }
            case SENSOR:{
                break;
            }
        }

        // 执行成功之后，更改NodeImpl中该任务的状态
        return COLLECTSUCCESS;
    }

    private void StartCollecting(BaseColConfig baseColConfig,IDeviceDriver deviceDriver,int deviceType,LogEntry logEntry) {
        try {
            if(baseColConfig.getFrequency() < 1) {
                baseColConfig.setFrequency(100);
            }
            long interval = 1000 / baseColConfig.getFrequency(); // 真实采集间隔，一般10ms
//            interval = 3000; // 测试采集间隔，目的是为了让我看的清楚
            if(deviceType == ROBOT) {
                IRobotDriver iRobotDriver = (RokaeRobotDriverImpl)deviceDriver;
                RobotDynMsg robotDynMsg = null;
                AxleDynMsg axleDynMsg = null;
                String robotDynamicData,axleDynamicData;
                long timestamp,deltaTime;

                try {
                    while(true) {
                        timestamp = System.currentTimeMillis();

                        robotDynMsg  = iRobotDriver.getRobotDynamicData();
                        axleDynMsg = iRobotDriver.getAxleDynameData();

                        robotDynMsg.setTimestamp(timestamp);
                        axleDynMsg.setTimestamp(timestamp);

                        robotDynamicData = gson.toJson(robotDynMsg);
                        axleDynamicData = gson.toJson(axleDynMsg);

//                        byte[] robotDynDataBytes = robotDynamicData.getBytes();
//                        byte[] axleDynDataBytes = axleDynamicData.getBytes();
//                        System.out.println("robotDynamicData的长度(字节数B) : " + robotDynDataBytes.length);
//                        System.out.println("axleDynamicData的长度(字节数B) : " + axleDynDataBytes.length);
//                        System.out.println("robotDynamicData : " + robotDynamicData);
//                        System.out.println("axleDynamicData : " + axleDynamicData);
                        KafkaService.sendMsgToKafka("robotdynamic", robotDynamicData);
                        KafkaService.sendMsgToKafka("axledynamic", axleDynamicData);
                        deltaTime = System.currentTimeMillis() - timestamp;

                        if(deltaTime < interval)
                            Thread.sleep(interval - deltaTime);
                    }
                }
                catch(ConnectInterruptedException connectInterruptedException) {
                    NodeImpl node = NodeImpl.getInstance();
                    node.removeTasks(logEntry);
                }
                // 这里需要做资源释放
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
