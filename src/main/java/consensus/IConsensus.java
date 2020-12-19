package consensus;

import model.consensusmodel.aentry.AentryParam;
import model.consensusmodel.aentry.AentryResult;
import model.consensusmodel.rvote.RvoteParam;
import model.consensusmodel.rvote.RvoteResult;

public interface IConsensus {
    /**
     * 	 处理请求投票RPC
     *  接收者实现：
     *  		1. 如果 param.term < currentTerm 返回false
     *  		2. 如果votedFor为空或者当前就是candidateId，
     *  			并且候选人的日志至少和自己一样新，投票给他
     */
    RvoteResult requestVote(RvoteParam param);

    /**
     * 	处理附加日志RPC
     * 接收者实现：
     * 			1. 如果param.term < currentTerm 返回false
     * 			2. 如果在prevLogIndex位置处的日志条目的任期号和prevLogTerm不匹配 返回false
     * 			3. 如果已存在日志条目和新的产生了冲突，删除这一条之后的所有日志
     * 			4. 把收到的附加日志中本地不存在的附加到本地日志库中
     * 			5. 如果param.LeaderCommit > commitIndex，
     * 				comminIndex = leaderCommit和新的日志条目索引值中较小的一个
     */
    AentryResult appendEntries(AentryParam param);
}
