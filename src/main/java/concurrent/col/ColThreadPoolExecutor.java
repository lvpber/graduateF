package concurrent.col;

import concurrent.RaftNameThreadFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ColThreadPoolExecutor extends ThreadPoolExecutor {
    /**
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param unit
     * @param workQueue
     * @param raftNameThreadFactory
     */
    public ColThreadPoolExecutor(int corePoolSize,                              // 核心线程数量
                                 int maximumPoolSize,                           // 线程池最大线程量
                                 long keepAliveTime,                            // 当线程数目超过核心线程数目后，多余空闲线程最大存活时间
                                 TimeUnit unit,                                 // 时间的单位
                                 BlockingQueue<Runnable> workQueue,             // 阻塞队列
                                 RaftNameThreadFactory raftNameThreadFactory) { // 线程工厂用于创建线程
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,raftNameThreadFactory);
    }

    private static final ThreadLocal<Long> COST_TIME_WATCH = ThreadLocal.withInitial(System::currentTimeMillis);

    @Override
    protected void beforeExecute(Thread t,Runnable r) {
        COST_TIME_WATCH.get();
    }

    @Override
    protected void afterExecute(Runnable r,Throwable t) {
        COST_TIME_WATCH.remove();
    }
}
