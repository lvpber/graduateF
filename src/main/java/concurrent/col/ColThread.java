package concurrent.col;

import lombok.Getter;
import lombok.Setter;
import model.logmodulemodel.LogEntry;
import node.NodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@Setter
public class ColThread extends Thread {
    private LogEntry logEntry;

    private static final Logger LOGGER = LoggerFactory.getLogger(ColThread.class);

    /** 线程运行不会抛出异常，有两种解决方式：
     * 一种在线程内部通过主动try-catch形式捕获处理
     * 一种是为线程添加UncaughtExceptionHandler
     */
    private UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER = (t,e) -> {
        System.out.println("线程异常中止，真实的中止，变更node中的状态");
        NodeImpl node = NodeImpl.getInstance();
        synchronized (node.lock) {
            node.getRunningLogs().removeIf(log -> log.getIndex() == logEntry.getIndex() && log.getTerm() == logEntry.getTerm());
        }
    };

    public ColThread(String threadName , Runnable r) {
        super(r,threadName);
        setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER);
    }
}
