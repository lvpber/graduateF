package model.logmodulemodel;

import io.netty.util.internal.StringUtil;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
public class LogEntry implements Serializable {
    private int index;              /** 日志在索引库中的下标 */
    private int term;               /** 日志的任期号 */
    private Command command;        /** 日志中包含的指令 */
    private String targetHost;      /** 最终执行的节点 */
    private String lastTargetHost;   /** 这条日志的上一个执行节点，主要用于任务恢复 */

    public LogEntry() { }

    public LogEntry(int term, Command command) {
        this.term = term;
        this.command = command;
    }

    public LogEntry(int index, int term, Command command) {
        this.index = index;
        this.term = term;
        this.command = command;
    }

    private LogEntry(Builder builder) {
        setIndex(builder.index);
        setTerm(builder.term);
        setCommand(builder.command);
        setTargetHost(builder.targetHost);
        setLastTargetHost(builder.lastTargetHost);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "{" +
                "index=" + index +
                ", term=" + term +
                ", command=" + command +
                ", targetHost=" + targetHost +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LogEntry logEntry = (LogEntry) o;
        return term == logEntry.term &&
                Objects.equals(index, logEntry.index) &&
                Objects.equals(command, logEntry.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, term, command);
    }

    public static final class Builder {

        private int index;
        private int term;
        private Command command;
        private String targetHost;      /** 最终执行的节点 */
        private String lastTargetHost;  /** 上一个执行该任务的节点 */

        private Builder() {
        }

        public Builder index(int val) {
            index = val;
            return this;
        }

        public Builder term(int val) {
            term = val;
            return this;
        }

        public Builder command(Command val) {
            command = val;
            return this;
        }

        public Builder targetHost(String targetHost) {
            this.targetHost = targetHost;
            return this;
        }

        public Builder lastTargetHost(String lastTargetHost) {
            this.lastTargetHost = lastTargetHost;
            return this;
        }

        public LogEntry build() {
            return new LogEntry(this);
        }
    }
}
