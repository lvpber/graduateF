package model.consensusmodel.aentry;

import lombok.Getter;
import lombok.Setter;
import model.logmodulemodel.LogEntry;
import model.state.RobotStatus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AentryResult implements Serializable {
    private int     term;                           /** 跟随者周期，方便候选人更新自己 */
    private boolean success;                        /** 是否投票成功 */
    private ArrayList<LogEntry> runningTasks;        /** 当前节点的所有机器人状态 */

    public AentryResult(boolean success) {
        this.success = success;
    }

    private AentryResult(Builder builder) {
        setTerm(builder.term);
        setSuccess(builder.success);
        setRunningTasks(builder.runningTasks);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static AentryResult fail() {
        return new AentryResult(false);
    }

    public static AentryResult ok()
    {
        return new AentryResult(true);
    }

    public static final class Builder {
        private int term;
        private boolean success;
        private ArrayList<LogEntry> runningTasks;

        private Builder() {
        }

        public Builder term(int val) {
            term = val;
            return this;
        }

        public Builder success(boolean val) {
            success = val;
            return this;
        }

        public Builder runningTasks(ArrayList<LogEntry> runningTasks) {
            this.runningTasks = runningTasks;
            return this;
        }

        public AentryResult build() {
            return new AentryResult(this);
        }
    }
}
