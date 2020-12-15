package consensus.impl;

import model.logmodulemodel.LogEntry;
import model.state.RobotStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import consensus.IConsensus;
import model.consensusmodel.aentry.AentryParam;
import model.consensusmodel.aentry.AentryResult;
import model.consensusmodel.rvote.RvoteParam;
import model.consensusmodel.rvote.RvoteResult;
import model.peer.Peer;
import node.NodeImpl;
import util.timer.ElectionTimerTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static model.node.NodeStatus.FOLLOWER;

/**
 *	多个服务器都会发送请求投票RPC
 *	所以这个类是一个多线程争夺的资源，都会在这里处理任务，对此对于一些变量
 *	需要保证线程安全
 */
public class ConsensusImpl implements IConsensus
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsensusImpl.class);
    private final NodeImpl node;

    public ConsensusImpl(NodeImpl nodeImpl) {
        this.node = nodeImpl;
    }

    @Override
    public RvoteResult requestVote(RvoteParam param) {
        try	{
            RvoteResult.Builder builder = RvoteResult.newBuilder();
            synchronized (node.lock) {
//                System.out.println("------------------------------------------------------------------------");
//
//                System.out.println("当前节点 [" + node.getPeerSet().getSelf() + "] " +
//                        "收到了节点 [" +	param.getCandidateId() + "] 的请求投票请求");
//                System.out.println("对方的任期是 " + param.getTerm() + ",自己的任期是 " + node.getPersistVal().getCurrentTerm());
                /** 对方任期没有自己新或者自己当前已经投票给其他节点 */
                if(param.getTerm() < node.getPersistVal().getCurrentTerm() ||
                        param.getTerm() == node.getPersistVal().getCurrentTerm() && node.getPersistVal().getVotedFor() != null && !node.getPersistVal().getVotedFor().equals(param.getCandidateId()))	{
//                    System.out.println("对方的任期没有自己的大，所以拒绝本次投票");
//                    System.out.println("########################################################################");
                    return builder.term(node.getPersistVal().getCurrentTerm()).voteGranted(false).build();
                }

                /** 对方任期比自己的要高，更改自己 */
                if(param.getTerm() > node.getPersistVal().getCurrentTerm()) {
                    // 修改了持久化的内容，这里需要持久化操作
                    node.getPersistVal().setCurrentTerm(param.getTerm());       /** 设置任期号 */
                    node.switchRoleTo(FOLLOWER);
                }

//                /** 用于输出注释 */
//                if(node.getPersistVal().getVotedFor() == null || node.getPersistVal().getVotedFor().length() == 0) {
//                    System.out.println("当前节点还没有投票给任何节点");
//                }
//                else {
//                    System.out.println("当前节点已经投票给[" + node.getPersistVal().getVotedFor() + "],所以拒绝");
//                }

                /** 判断当前节点和请求节点的最后一条日志谁更新一点 */
                LogEntry logEntry;
                int lastLogIndex = node.getPersistVal().getLog().size() - 1;		// 下标从1开始
                if(lastLogIndex > 1) {
                    logEntry = node.getPersistVal().getLog().get(lastLogIndex);
                    if(param.getLastLogTerm() < logEntry.getTerm() ||
                            param.getLastLogTerm() == logEntry.getTerm() && param.getLastLogIndex() < lastLogIndex) {
                        return builder.term(node.getPersistVal().getCurrentTerm()).voteGranted(false).build();
                    }
                }

                node.getPersistVal().setVotedFor(param.getCandidateId());	// 这个内容修改之后需要持久化
                node.getElectionTimer().reset(new ElectionTimerTask(),node.randTimerDuration());
//                System.out.println("当前节点 [" + node.getPeerSet().getSelf() + "] 认为符合条件，投 [" +
//                        node.getPersistVal().getVotedFor() + "]一票");
//                System.out.println("########################################################################");
                return builder
                        .term(node.getPersistVal().getCurrentTerm())
                        .voteGranted(true)
                        .build();
            }
        }
        catch (Exception e)	{
            System.out.println("this node is [" + node.getPeerSet().getSelf() + "] and the election task exists Error : " +	e.getMessage());
        } finally {
            node.persist();
        }
        return null;
    }

    @Override
    public AentryResult appendEntries(AentryParam param) {
        try {
            synchronized (node.lock) {
//                System.out.println("当前节点 [" + node.getPeerSet().getSelf().getAddr() + "] 收到了Leader [" +
//                        param.getLeaderId() + "] 的附加日志请求");
                AentryResult result = AentryResult.fail();
                result.setTerm(node.getPersistVal().getCurrentTerm());		// result = {node.currentTerm,false}

                /** 如果附加日志请求的节点的任期号小于当前节点任期直接返回false */
                if(param.getTerm() < node.getPersistVal().getCurrentTerm())	{
                    result.setRunningTasks(null);                           // 因为这种情况下，leader不再具备主导权，不需要给他返回任务执行情况
                    return result;
                }

                /** 如果对方的任期号比自己的大，更新自己的任期号，同时变更角色为Follower */
                if(param.getTerm() > node.getPersistVal().getCurrentTerm()) {
                    node.getPersistVal().setCurrentTerm(param.getTerm());
                    node.switchRoleTo(FOLLOWER);
                }

                /** 修改返回值添加当前节点的任务执行状况 */
                result.setRunningTasks(node.getRunningLogs());

                // 重置选举超时计时器
                node.getElectionTimer().reset(new ElectionTimerTask(),node.randTimerDuration());
                node.getPeerSet().setLeader(new Peer(param.getLeaderId()));

                int lastLogIndex = node.getPersistVal().getLog().size() - 1;
                if(lastLogIndex < param.getPrevLogIndex()) {
                    /** 这里没有优化，先实现，如果发现自己的索引长度还不到leader的prevLogIndex，那就直接返回不匹配 */
                    System.out.println("当前节点的最大索引下标小于prevLogIndex");
                    return result;
                }

                if(node.getPersistVal().getLog().get(param.getPrevLogIndex()).getTerm() != param.getPrevLogTerm()) {
                    // 在prevLogIndex位置处存在日志，但是不匹配
                    /** 这里也没有优化，先实现，后续需要重新实现  */
                    System.out.println("在prevLogIndex位置出的日志不匹配");
                    return result;
                }

                /** 添加本节点不存在的日志内容 */
                int unmatch_idx = -1;
                int idx = 0;
                for(;idx<param.getEntries().length;idx++) {
                    if (node.getPersistVal().getLog().size() < (param.getPrevLogIndex() + 2 + idx) ||
                            param.getEntries()[idx].getTerm() != node.getPersistVal().getLog().get(param.getPrevLogIndex() + 1 + idx).getTerm() ){
                        unmatch_idx = idx;
                        break;
                    }
                }

                if(unmatch_idx != -1) {
                    // 发现不匹配的地方
                    int index = (int)param.getPrevLogIndex() + 1 + unmatch_idx;
                    while(node.getPersistVal().getLog().size() >= (index+1)) {
                        node.getPersistVal().getLog().remove(index);
                    }
                    for(;unmatch_idx<param.getEntries().length;unmatch_idx++) {
                        node.getPersistVal().getLog().add(param.getEntries()[unmatch_idx]);
                    }
                }

                if(param.getLeaderCommit() > node.getCommitIndex()) {
                    node.setNowCommitIndex(Math.min(param.getLeaderCommit(),node.getPersistVal().getLog().size()-1));
                }

                result.setSuccess(true);
                return result;
            }
        }
        catch (Exception e)	{
            e.printStackTrace();
            System.out.println("this node is [" + node.getPeerSet().getSelf() + "] solve the append entry task exists" + " Error : " + e.getMessage());
        } finally {
            node.persist();
        }
        return null;
    }
}
