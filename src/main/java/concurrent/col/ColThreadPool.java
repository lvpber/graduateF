package concurrent.col;

import concurrent.RaftNameThreadFactory;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ColThreadPool {

    private static int cup = 20;//Runtime.getRuntime().availableProcessors();               /** jvm可以使用的处理器核数 */
    private static int maxPoolSize = cup;// * 2;                                            /** 最大线程池为处理器数目*2 */
    private static final int queenSize = 1024;                                              /** 阻塞队列大小 */
    private static final long keepTime = 1000 * 60;                                         /** 空闲线程存活时间 1分钟 */
    private static TimeUnit keepTimeUnit = TimeUnit.MILLISECONDS;                           /** 存活时间单位毫秒 */
    private static ThreadPoolExecutor threadPoolExecutor = getThreadPoolExecutor();         /** 单纯执行任务 */

    private static ThreadPoolExecutor getThreadPoolExecutor() {
        return new ColThreadPoolExecutor(cup,              /** 核心线程数 */
                maxPoolSize,                                /** 最大线程池大小 */
                keepTime,                                   /** 线程最大空闲时间 */
                keepTimeUnit,                               /** 空闲时间单位 */
                new LinkedBlockingDeque(queenSize),         /** 线程等待队列 */
                new RaftNameThreadFactory()                 /** 线程创建工厂 */
        );
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
        } else {
            threadPoolExecutor.execute(r);
        }
    }
}
