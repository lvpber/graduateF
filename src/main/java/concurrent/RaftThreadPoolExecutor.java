package concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RaftThreadPoolExecutor extends ThreadPoolExecutor {

    /**
     * @param corePoolSize		    	核心线程数量
     * @param maximumPoolSize	        线程池最大线程量
     * @param keepAliveTime		        当线程数目超过核心线程数目后，多余空闲线程最大存活时间
     * @param unit					    时间的单位
     * @param workQueue				    阻塞队列
     * @param raftNameThreadFactory		线程工厂用于创建线程
     */
    public RaftThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                                  long keepAliveTime, TimeUnit unit,
                                  BlockingQueue<Runnable> workQueue,
                                  RaftNameThreadFactory raftNameThreadFactory
    ) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,raftNameThreadFactory);
        // TODO Auto-generated constructor stub
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
