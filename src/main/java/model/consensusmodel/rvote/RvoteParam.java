package model.consensusmodel.rvote;

import lombok.Getter;
import lombok.Setter;
import model.consensusmodel.base.BaseParam;

@Getter
@Setter
public class RvoteParam extends BaseParam {
    private String candidateId;		/** 候选人id */
    private int lastLogIndex;		/** 候选人最后日志条目的索引值 */
    private int lastLogTerm;		/** 候选人最后日志条目的任期号 */

    @Override
    public String toString() {
        return "RvoteParam{" +
                "candidateId='" + candidateId + '\'' +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLogTerm=" + lastLogTerm +
                ", term=" + super.getTerm() +
                ", serverId='" + super.getServerId() + '\'' +
                '}';
    }

    private RvoteParam(Builder builder){
        setTerm(builder.term);
        setServerId(builder.serverId);
        setCandidateId(builder.candidateId);
        setLastLogIndex(builder.lastLogIndex);
        setLastLogTerm(builder.lastLogTerm);
    }

    public static Builder newBuilder() {return new Builder();}

    public static final class Builder {
        private int term;
        private String serverId;
        private String candidateId;
        private int lastLogIndex;
        private int lastLogTerm;

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

        public Builder candidateId(String val) {
            candidateId = val;
            return this;
        }

        public Builder lastLogIndex(int val) {
            lastLogIndex = val;
            return this;
        }

        public Builder lastLogTerm(int val) {
            lastLogTerm = val;
            return this;
        }

        public RvoteParam build() {return new RvoteParam(this);}
    }
}
