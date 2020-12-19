package concurrent;

import java.util.concurrent.ThreadFactory;

public class RaftNameThreadFactory implements ThreadFactory{

    @Override
    public Thread newThread(Runnable r) {
        // TODO Auto-generated method stub
        Thread thread = new RaftThread("Raft Thread", r);
        thread.setDaemon(true);			//为啥设置成守护线程
        return thread;
    }

}
