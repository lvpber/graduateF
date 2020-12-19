package util.timer;

import node.NodeImpl;

import java.util.TimerTask;

public class HeartBeatTimerTask extends TimerTask {
    @Override
    public void run() {
        // 只是进行简单的超时提醒
        NodeImpl node = NodeImpl.getInstance();
        synchronized (node.getHeartBeatLock()) {
//            System.out.println("心跳超时计时器超时了，提醒开始心跳");
            node.getHeartBeatLock().notifyAll();
        }
    }
}
