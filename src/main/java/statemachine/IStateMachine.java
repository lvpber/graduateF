package statemachine;

import model.logmodulemodel.LogEntry;

public interface IStateMachine {
    void apply(LogEntry loeEntry);				/** 日志应用到状态机上  */
    LogEntry get(String key);					/** 获取状态机中key的value */
    String getString(String key);				/** 获取value */
    void setString(String key,String value);	/** 设置value */
    void delString(String... keys);				/** 删除一些列的key */
}
