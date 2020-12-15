package statemachine.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.util.internal.StringUtil;
import model.logmodulemodel.Command;
import model.logmodulemodel.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import statemachine.IStateMachine;
import util.StoreUtil;

import java.util.concurrent.locks.ReentrantLock;

public class StateMachineImpl implements IStateMachine {

    /** 日志生成 */
    private static final 	Logger 			log = LoggerFactory.getLogger(StateMachineImpl.class);
    private final 			Gson 			gson = new GsonBuilder().create();								/** 对象与json互转工具 */
    private 				ReentrantLock 	reentrantLock = new ReentrantLock();							/** 可重入锁 */
    private final 			String 			statePrefix;													/** 状态机查询前缀 */
    private final 			String			HOST_ADDR;														/** 当前节点身份 */
//	/** 日志序列化路径 */
//	private final String STATE_MACHINE_FILE_PATH = "/home/lvpb/software/graduate/statefile.txt";
//
//	/** 如果没有就加载 */
//	private List<LogEntry> logEntries = new ArrayList<LogEntry>();

    public StateMachineImpl(String hostAddr) {
        this.HOST_ADDR = hostAddr;
        statePrefix = HOST_ADDR + ".state.LogEntry.key";
    }

//	// 私有构造方法
//	private StateMachineImpl()
//	{
//
//	}
//
//	/** 私有静态类实现单例模式 */
//	public static StateMachineImpl getInstance()
//	{
//		return DefaultStateMachineLazyHolder.INSTANCE;
//	}
//
//	private static class DefaultStateMachineLazyHolder
//	{
//		private static final StateMachineImpl INSTANCE = new StateMachineImpl();
//	}

    /** 最重要的一个方法 将日志应用到状态机上 */
    @Override
    public void apply(LogEntry logEntry) {
        Command command = logEntry.getCommand();
        if(command == null) {
            throw new IllegalArgumentException("command can not be null , logEntry : " + logEntry);
        }
        String key = command.getKey();
        String value = gson.toJson(logEntry);

        StoreUtil.write(this.statePrefix,key,value);
    }

    /** 获取状态值 返回状态描述 logEntry */
    @Override
    public LogEntry get(String key) {
        String logEntryJson = StoreUtil.read(statePrefix,key);

        if(logEntryJson == null)
            return null;

        return gson.fromJson(logEntryJson,LogEntry.class);
    }

    /** 读取值，获取字符串 */
    @Override
    public String getString(String key) {
        String logEntryJson = StoreUtil.read(statePrefix,key);

        if(logEntryJson == null)
            return "";

        return logEntryJson;
    }

    @Override
    public void setString(String key, String value) {
        // key & value
        if(StringUtil.isNullOrEmpty(key)) {
            log.info("StateMachineImpl -> setString : The key is null or empty");
            return;
        }
        if(StringUtil.isNullOrEmpty(value)) {
            log.info("StateMachineImpl -> setString : The value is null or empty");
            return;
        }
        StoreUtil.write(statePrefix,key,value);
    }

    @Override
    public void delString(String... keys) {
        for(String s : keys)
        {
            StoreUtil.delete(statePrefix,s);
        }
    }


    public static void main(String args[]) {
        StateMachineImpl stateMachine = new StateMachineImpl("localhost:8000");
        String[] array = new String[4];
        for(int i = 0;i<4;i++) {
            LogEntry logEntry = LogEntry.newBuilder()
                    .term(i)
                    .index(i)
                    .command(
                            Command.newBuilder()
                                    .key("hello state : " + i)
                                    .value("hey state : " + i)
                                    .build()
                    )
                    .build();

            array[i] = "hello state : " + i;
        }
//		stateMachine.apply(logEntry1);
//		stateMachine.apply(logEntry2);
//		stateMachine.apply(logEntry3);
//		String []array = {"111","222"};
//		stateMachine.delString(array);
//		logModule.removeOnStartIndex(2L);
    }
}
