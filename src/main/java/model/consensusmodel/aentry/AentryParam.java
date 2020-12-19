package model.consensusmodel.aentry;

import lombok.Getter;
import lombok.Setter;
import model.consensusmodel.base.BaseParam;
import model.logmodulemodel.LogEntry;

import java.util.Arrays;

@Getter
@Setter
public class AentryParam extends BaseParam {
    private String      leaderId;            /** 领导人Id 便于跟随者实现重定向 */
    private int         prevLogIndex;        /** 新的日志条目紧随之前的索引值（就是上一条日志条目） */
    private int         prevLogTerm;         /** prevLogIndex日志条目的任期号 */
    private LogEntry[]  entries;             /** 准备存储的日志条目（表示心跳时为空；一次性发送多个是为了提高效率） */
    private int         leaderCommit;        /** 领导人已经提交的日志的索引值  */

    public AentryParam() {
    }

    private AentryParam(Builder builder) {
        setTerm(builder.term);
        setServerId(builder.serverId);
        setLeaderId(builder.leaderId);
        setPrevLogIndex(builder.prevLogIndex);
        setPrevLogTerm(builder.prevLogTerm);
        setEntries(builder.entries);
        setLeaderCommit(builder.leaderCommit);
    }

    @Override
    public String toString() {
        return "AentryParam{" +
                "leaderId='" + leaderId + '\'' +
                ", prevLogIndex=" + prevLogIndex +
                ", prevLogTerm=" + prevLogTerm +
                ", entries=" + Arrays.toString(entries) +
                ", leaderCommit=" + leaderCommit +
                ", term=" + super.getTerm() +
                ", serverId='" + super.getServerId() + '\'' +
                '}';
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Setter
    @Getter
    public static final class Builder {

        private int term;
        private String serverId;
        private String leaderId;
        private int prevLogIndex;
        private int prevLogTerm;
        private LogEntry[] entries;
        private int leaderCommit;

        private Builder() {
        }

        public Builder term(int val) {
            term = val;
            return this;
        }

        public Builder serverId(String val) {
            serverId = val;
            return this;
        }

        public Builder leaderId(String val) {
            leaderId = val;
            return this;
        }

        public Builder prevLogIndex(int val) {
            prevLogIndex = val;
            return this;
        }

        public Builder prevLogTerm(int val) {
            prevLogTerm = val;
            return this;
        }

        public Builder entries(LogEntry[] val) {
            entries = val;
            return this;
        }

        public Builder leaderCommit(int val) {
            leaderCommit = val;
            return this;
        }

        public AentryParam build() {
            return new AentryParam(this);
        }
    }
}
