package concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RaftThreadPool {
    /** jvm可以使用的处理器核数 */
    private static int cup = 20;//Runtime.getRuntime().availableProcessors();

    /** 最大线程池为处理器数目*2 */
    private static int maxPoolSize = cup;// * 2;

    /** 阻塞队列大小 */
    private static final int queenSize = 1024;

    /** 空闲线程存活时间 1分钟 */
    private static final long keepTime = 1000 * 60;

    /** 存活时间单位毫秒 */
    private static TimeUnit keepTimeUnit = TimeUnit.MILLISECONDS;

    /** 定时执行任务 */
    private static ScheduledExecutorService scheduledExecutorService = getScheduled();

    /** 单纯执行任务 */
    private static ThreadPoolExecutor threadPoolExecutor = getThreadPoolExecutor();

    private static ThreadPoolExecutor getThreadPoolExecutor() {
        return new RaftThreadPoolExecutor(cup,              /** 核心线程数 */
                maxPoolSize,                                /** 最大线程池大小 */
                keepTime,                                   /** 线程最大空闲时间 */
                keepTimeUnit,                               /** 空闲时间单位 */
                new LinkedBlockingDeque(queenSize),         /** 线程等待队列 */
                new RaftNameThreadFactory()                 /** 线程创建工厂 */
        );
    }

    /** 可以周期性执行任务 */
    private static ScheduledExecutorService getScheduled() {
        return new ScheduledThreadPoolExecutor(cup,new RaftNameThreadFactory());
    }

    /**
     * 按照指定周期执行指定任务
     * @param r					要执行的任务
     * @param initDelay		初始等待时间
     * @param delay			周期
     */
    public static void scheduleAtFixedRate(Runnable r, long initDelay , long delay) {
        scheduledExecutorService.scheduleAtFixedRate(r, initDelay, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 按照指定周期执行任务
     * @param r			要执行的任务
     * @param delay	周期
     */
    public static void scheduleWithFixedDelay(Runnable r , long delay) {
        scheduledExecutorService.scheduleWithFixedDelay(r, 0, delay, TimeUnit.MILLISECONDS);
    }

    public static <T> Future<T> submit(Callable r) {
        return threadPoolExecutor.submit(r);
    }

    public static void execute(Runnable r) {
        threadPoolExecutor.execute(r);
    }

    /**
     * @param r			r是一个Runnable对象，传递进来的可能是一个可执行的对象比如(Thread)
     * 								也可能只是一段代码，比如函数式接口，如果是可执行对象，r内存在run方法，
     * 								就可以直接运行，如果是函数式接口，传进来的代码块不具备自己运行的能力，
     * 								就要交给线程池处理
     * @param sync		            true    自己运行
     * 								false 	交由线程池运行
     */
    public static void execute(Runnable r, boolean sync) {
        if(sync) {
            r.run();
        }
        else {
            threadPoolExecutor.execute(r);
        }
    }
}
