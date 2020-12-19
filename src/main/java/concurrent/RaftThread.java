package concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaftThread extends Thread{
    private static final Logger LOGGER = LoggerFactory.getLogger(RaftThread.class);
    private static final UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER =
            (t,e) -> LOGGER.warn("exception occured from thread {} ",t.getName(),e);

    /**
     * 创建一个线程，设定线程的uncaughtExceptionHandler
     * @param threadName
     * @param r
     */
    public RaftThread(String threadName , Runnable r) {
        super(r,threadName);
        setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER);
    }
}