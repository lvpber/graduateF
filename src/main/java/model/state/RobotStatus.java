package model.state;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/** 任务在follower上的执行状态 */

@Getter
@Setter
public class RobotStatus implements Serializable {
    public static final int INIT    = 0;
    public static final int RUNNING = 1;
    private String recordId;                    /** 记录id */
    private String deviceId;                    /** 任务对应的设备id */
    private int    taskStatus;                  /** 任务的执行状态 */

    /**
     * 有一种情况：在follower中处于init，但是在共识集中并没有该条任务信息
     *          可能1：leader还没来得及实现信息的一致性同步
     *          可能2：信息同步失败，因为leader宕机，或者其他原因导致该任务不可能继续执行
     * */
    private int    taskTerm;                    /** 该任务的任期号 */
    private int    taskIndex;                   /** 该任务的索引 */

    private RobotStatus(){}

    private RobotStatus(Builder builder) {
        setDeviceId(builder.deviceId);
        setTaskStatus(builder.taskStatus);
        setTaskTerm(builder.taskTerm);
        setTaskIndex(builder.taskIndex);
        setRecordId(builder.recordId);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private String deviceId;
        private int taskStatus;
        private int taskTerm;
        private int taskIndex;
        private String recordId;

        private Builder() {}

        public Builder DeviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder TaskStatus(int taskStatus) {
            this.taskStatus = taskStatus;
            return this;
        }

        public Builder TaskTerm(int taskTerm) {
            this.taskTerm = taskTerm;
            return this;
        }

        public Builder TaskIndex(int taskIndex) {
            this.taskIndex = taskIndex;
            return this;
        }

        public Builder RecordId(String recordId) {
            this.recordId = recordId;
            return this;
        }

        public RobotStatus build() {
            return new RobotStatus(this);
        }
    }
}