package util.timer;
import node.NodeImpl;

import java.util.TimerTask;

public class ElectionTimerTask extends TimerTask {

    @Override
    public void run() {
        // 选举超时提醒
        NodeImpl node = NodeImpl.getInstance();
        synchronized (node.getElectionLock()) {
            System.out.println("选举超时计时器超时了，提醒开始选举");
            node.getElectionLock().notifyAll();
        }
    }
}
