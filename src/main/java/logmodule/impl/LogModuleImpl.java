package logmodule.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import logmodule.ILogModule;
import model.logmodulemodel.Command;
import model.logmodulemodel.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.StoreUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LogModuleImpl implements ILogModule {
    /** 日志生成 */
    private static final Logger log = LoggerFactory.getLogger(LogModuleImpl.class);

//	/** 日志序列化路径 */
//	private final String LOG_MODULE_FILE_PATH = "/home/lvpb/software/graduate/logfile.txt";

//	/** 如果没有就加载 */
//	private List<LogEntry> logEntries = new ArrayList<LogEntry>();
    private final Gson gson = new GsonBuilder().create();           /** 对象与json互转工具 */
    ReentrantLock reentrantLock = new ReentrantLock();              /** 可重入锁 */

    private final String HOST_ADDR;                                 /** 现在用redis 实现lomodule 和 statemachine，然后需要一个ip地址做key的唯一鉴别，所以引进这个node */
    private final String logEntryPrefix;                            /** LogEntry前缀      : Host.log.logEntry.index */
    private final String logLastIndexPrefix;                        /** 最后一条日志下标前缀 : Host.log.lastIndex */

    public LogModuleImpl(String hostAddr) {
        this.HOST_ADDR = hostAddr;
        logEntryPrefix = HOST_ADDR + ".log.logEntry.index";
        logLastIndexPrefix = HOST_ADDR + ".log.lastIndex";
//		this.node = null;
//		this.logEntryPrefix = "127.0.0.1:8000.log.logEntry.index";
//		this.logLastIndexPrefix = "127.0.0.1:8000.log.lastIndex";
    }

    @Override
    public void write(LogEntry logEntry) {
        // 用于判断是否获取锁
        boolean success = false;
        try {
            reentrantLock.tryLock(3000, TimeUnit.MILLISECONDS);
            logEntry.setIndex(getLastIndex() + 1);
            String logEntryJson = gson.toJson(logEntry);
            StoreUtil.write(logEntryPrefix,""+logEntry.getIndex(),logEntryJson);	// 写进redis
            success = true;
            log.info("LogModuleImpl -> write() : redis success , logEntry info : [{}]" , logEntry );
        } catch (InterruptedException e) {
            log.info("LogModuleImpl -> write() : try lock faile");
            e.printStackTrace();
        } finally {
            if(success) {
                updateLastIndex(logEntry.getIndex());
            }
            if(reentrantLock.isHeldByCurrentThread()) {
                reentrantLock.unlock();
            }
        }
    }

    /**
     * 删除从startIndex到最后的所有日志
     * @param startIndex
     */
    @Override
    public void removeOnStartIndex(int startIndex) {
        boolean success = false;
        int count = 0;
        long result;
        try {
            reentrantLock.tryLock(3000,TimeUnit.MILLISECONDS);
            for(int i=startIndex;i<=getLastIndex();i++) {
                result = StoreUtil.delete(logEntryPrefix,""+i);
                count+=result;
            }
            success = true;
            log.info("LogModuleImpl -> removeOnStartIndex() : remove from startIndex success, count = {}," +
                    "startIndex = {},lastIndex = {}",count,startIndex,getLastIndex());
        } catch (InterruptedException e) {
            log.info("LogModuleImpl -> removeOnStartIndex() : try lock faile");
        } finally {
            if(success) {
                updateLastIndex(getLastIndex() - count);
            }
            if(reentrantLock.isHeldByCurrentThread()) {
                reentrantLock.unlock();
            }
        }
    }

    @Override
    public LogEntry read(int index) {
        if(index < 0)
            return null;

        String logEntryJson = StoreUtil.read(logEntryPrefix,""+index);
        if(logEntryJson == null)
            return null;

        return gson.fromJson(logEntryJson,LogEntry.class);
    }

    /** 从redis中获取最后一项日志的内容 */
    @Override
    public LogEntry getLast() {
        Integer index = getLastIndex();
        if(index == null)
            return null;

        String logEntryJson = StoreUtil.read(logEntryPrefix,""+index);
        if(logEntryJson == null)
            return null;

        return gson.fromJson(logEntryJson,LogEntry.class);
    }

    /** 从redis中获取当前主机的最后一条日志的索引值 */
    @Override
    public Integer getLastIndex() {
        String lastIndexStr = StoreUtil.read(logLastIndexPrefix,"");
        if(lastIndexStr == null)
            return 0;
        return Integer.parseInt(lastIndexStr);
    }

    /** 更新lastIndex */
    private void updateLastIndex(Integer lastIndex)	{
        if(lastIndex == null) {
            return;
        }

        StoreUtil.write(logLastIndexPrefix,"",lastIndex+"");
    }

    public static void main1(String[] args){
        LogModuleImpl logModule = new LogModuleImpl("localhost:8000");

        for(int i=0;i<4;i++){
            LogEntry logEntry = LogEntry.newBuilder()
                    .term(i)
                    .index(i)
                    .command(
                            Command.newBuilder()
                                    .key("hello log : " + i)
                                    .value("hey log : " + i)
                                    .build()
                    )
                    .build();
            logModule.write(logEntry);
            System.out.println(logModule.getLast() + "   " + logModule.getLastIndex());
        }

        logModule.removeOnStartIndex(1);
    }

}
