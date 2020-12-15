package node;

import collect.executor.ColExcutor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import concurrent.RaftThreadPool;
import consensus.impl.ConsensusImpl;
import io.netty.util.internal.StringUtil;
import lifecycle.ILifeCycle;
import logmodule.impl.LogModuleImpl;
import lombok.Getter;
import lombok.Setter;
import model.clientmodel.ClientKVAck;
import model.clientmodel.ClientKVReq;
import model.consensusmodel.aentry.AentryParam;
import model.consensusmodel.aentry.AentryResult;
import model.consensusmodel.rvote.RvoteParam;
import model.consensusmodel.rvote.RvoteResult;
import model.logmodulemodel.Command;
import model.logmodulemodel.LogEntry;
import model.monitor.JVMInfo;
import model.node.NodeConfig;
import model.node.NodeStatus;
import model.peer.Peer;
import model.peer.PeerSet;
import model.persistmodel.RaftPersistVal;
import model.rpcmodel.Request;
import model.rpcmodel.Response;
import model.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpc.impl.RpcClientImpl;
import rpc.impl.RpcServerImpl;
import statemachine.impl.StateMachineImpl;
import util.StoreUtil;
import util.jvm.JVMUtil;
import util.kafka.KafkaService;
import util.timer.ElectionTimerTask;
import util.timer.HeartBeatTimerTask;
import util.timer.MyTimer;

import java.util.*;
import java.util.concurrent.*;

import static model.node.NodeStatus.CANDIDATE;
import static model.node.NodeStatus.FOLLOWER;
import static model.node.NodeStatus.LEADER;

/** BootStrap -> this.setConfig(config)
 *  BootStrap -> this.init()
 * */

@Getter
@Setter
public class NodeImpl implements INode, ILifeCycle {
    public static final int MAXTASKCOUNT = 500;
    private static      final   Logger LOGGER = LoggerFactory.getLogger(NodeImpl.class);
    public              final   Object  lock = new Object();
    private final       long    HEART_BEAT_TIMEOUT  = 150;      /** 超时时间基准100 */
    private final       Random  random = new Random();          /** 随机数工具，用于生成随机数 */

    /** MyTimer本身线程安全，两个锁的目的是为了实现线程通信 */
    private             MyTimer heartBeatTimer;                 /** 心跳超时计时器，超时后notify heartBeat */
    private             Object  heartBeatLock = new Object();
    private             MyTimer electionTimer;                  /** 选举超时计时器，超时后notify election */
    private             Object  electionLock  = new Object();

    /** 两个任务 心跳和选举任务 */
    private HeartBeatTask heartBeatTask = new HeartBeatTask();
    private ElectionTask  electionTask  = new ElectionTask();

    private volatile int status = NodeStatus.FOLLOWER;
    private PeerSet peerSet;

    /** ===========所有服务器上持久存在的========== */
    private RaftPersistVal  persistVal;

    /** ===========所有服务器上经常变的 =========== */
    private int 			commitIndex;
    private int 			lastApplied = 0;

    /** ===========领导人上经常变的============== */
    private Map<Peer, Integer> nextIndexs;
    private Map<Peer, Integer> matchIndexs;

    /** ==================================== */
    private 			NodeConfig 			nodeConfig;
    private             RpcServerImpl       rpcServerImpl;		                    /** RPCServer 负责发送RPC请求 */
    private             RpcClientImpl       rpcClientImpl;		                    /** RPCClient 负责接受RPC请求 */
    private volatile 	boolean 			started;
    private             StateMachineImpl    stateMachine;

    private             ConsensusImpl       consensusImpl;			                /** 一致性模块 	*/
    private             LogModuleImpl       logModuleImpl;				            /** 日志模块 	*/
    private final       Gson                gson = new GsonBuilder().create();      /** object和json字符串转换 */
    private             String              LOGKEY;                                 /** logKey */
    private             String              STATEKEY;                               /** stateKey */
    private             int                 voteCount;                              /** 辅助工具用于投票 */

    /** 对于任务执行，所有节点上都有的 */
    private             ArrayList<LogEntry>                     RunningLogs;            /** 真实运行成功的任务 */
    /** 对于任务执行，所有节点都有，但是只有leader会使用 */
    private             HashMap<String,ArrayList<LogEntry>>     RunningTasksMap;        /** 逻辑运行成功的任务 */
    private             HashMap<String,Integer>                 OfflineMap;             /** 统计每一个节点没有返回心跳的次数 */

    private             ColExcutor          colExcutor;

    /** 单例模式 */
    private NodeImpl(){}
    private static class NodeLazyHolder {
        private static final NodeImpl instance = new NodeImpl();
    }
    public static NodeImpl getInstance() {
        return NodeLazyHolder.instance;
    }

    /** ILifeCycle方法 */
    @Override
    public void init() {
        if (started) {
            return;
        }
        synchronized (this)	{
            if (started) {
                return;
            }
            /** 启用RPC Server 监听RPC请求（客户端、其他节点） */
            rpcServerImpl.start();

            // 初始化kafka模块
            KafkaService.init();

            /** 初始化一致性处理模块、日志存储模块 */
            consensusImpl = new ConsensusImpl(this);
            logModuleImpl = new LogModuleImpl(this.getPeerSet().getSelf().getAddr());
            stateMachine = new StateMachineImpl(this.getPeerSet().getSelf().getAddr());

            /** 初始化计时器 */
            heartBeatTimer = new MyTimer();
            electionTimer  = new MyTimer();
            heartBeatTimer.reset(new HeartBeatTimerTask(),HEART_BEAT_TIMEOUT);
            electionTimer.reset(new ElectionTimerTask(),randTimerDuration());

            /** 启动守护线程 */
            RaftThreadPool.execute(heartBeatTask,false);
            RaftThreadPool.execute(electionTask,false);

            /** 初始化节点任务情况，重启之后所有任务清空 */
            RunningLogs = new ArrayList<>();
            RunningTasksMap = new HashMap<>();              // 每一个节点都会维护一个，但是只有leader会真正使用它
            for(Peer peer : peerSet.getPeers()) {
                RunningTasksMap.put(peer.getAddr(),new ArrayList<>());
            }

            /** 初始化采集任务执行器 */
            colExcutor = ColExcutor.getInstance();

            /** 初始化每一个节点的宕机次数 */
            OfflineMap = new HashMap<>();
            for(Peer peer : peerSet.getPeers()) {
                String peerAddr = peer.getAddr();
                OfflineMap.put(peerAddr,0);
            }

            /** 如果是宕机恢复，当前任期为之前的最后一条日志的任期号 */
            /** 阅读持久化的内容包括Log，任期号和投票的对象 */
            persistVal = RaftPersistVal.getInstance();
            readPersist();

            started = true;	//开始
        }
    }

    @Override
    public void destroy() throws Throwable {
        rpcServerImpl.stop();
    }

    /** INode */
    @Override
    public void setConfig(NodeConfig config) {
        this.nodeConfig = config;

        peerSet = PeerSet.getInstance();
        for (String s : config.getPeerAddrs()) {
            Peer peer = new Peer(s);
            peerSet.addPeer(peer);
            if (s.equals(config.getSelfIpAddr() + ":" + config.getSelfPort())) {
                peerSet.setSelf(peer);
            }
        }
        LOGKEY = peerSet.getSelf().getAddr() + ".PERSIST";
        STATEKEY = peerSet.getSelf().getAddr() + ".STATE.";

        /** 初始化rpc工具 */
        rpcServerImpl = new RpcServerImpl(config.selfPort, this);
        rpcClientImpl = new RpcClientImpl();
    }

    @Override
    public RvoteResult handlerRequestVote(RvoteParam param) {
        return consensusImpl.requestVote(param);
    }

    @Override
    public AentryResult handlerAppendEntries(AentryParam param) {
        return consensusImpl.appendEntries(param);
    }

    @Override
    public ClientKVAck handlerClientRequest(ClientKVReq request) {
        synchronized (lock) {
            if(status != LEADER) {
                return redirect(request);
            }
        }

        /** 客户端建立采集请求之后，返回生成的LogEntry信息，客户端根据该信息来访问后续结果 */
        if(request.getType() == ClientKVReq.GET) {
            String key = request.getKey();
            /** 参数为空 */
            if(StringUtil.isNullOrEmpty(key)) {
                return new ClientKVAck(null);
            }

            /**
             *  如果状态机中已经存在，返回正常状态
             *  如果状态集中不存在
             *      如果日志小于当前已经应用于状态机的最后一条日志，那就是没戏了，返回fail
             *      否则返回处理中
             */
            synchronized (lock) {
                String stateStr;
                State state;
                if((stateStr = StoreUtil.read(STATEKEY,key)) != null) {
                    state = gson.fromJson(stateStr,State.class);
                    return new ClientKVAck(state.getValue());
                } else {
                    // 是否有可能写入状态机
                    if(request.getIndex() > lastApplied && request.getTerm() >= persistVal.getLog().get(lastApplied).getTerm()) {
                        return new ClientKVAck("processing");
                    } else {
                        return ClientKVAck.fail();
                    }
                }
            }
        }
        else {
            ClientKVAck clientKVAck = ClientKVAck.newBuilder()
                    .isSuccess(true)
                    .result(handleDealTask(request.getKey(),request.getValue(),Command.ADD))
                    .build();
            return clientKVAck;
        }
    }

    /** 处理任务，自己加锁 */
    private LogEntry handleDealTask(String key,String value,int opType) {
        /** step1 挑选节点这里其实有一个问题，如果我找的点内存不足，但是占比还是最大，那下次可能还是发给他，但是他执行不了，这时系统就陷入死循环，不考虑这里 */
        Peer peer = findTargetPeer();
        String targetHost = peer.getAddr();

        /** step2 进行节点信息同步 */
        LogEntry logEntry = null;
        synchronized (lock) {
            logEntry = LogEntry.newBuilder()
                    .command(Command.newBuilder()
                            .key(key)                       /** deviceId */
                            .value(value)                   /** collectingConfig */
                            .opType(opType)                 /** 建立采集 */
                            .build())
                    .index(persistVal.getLog().size())      /** nowIndex */
                    .term(persistVal.getCurrentTerm())      /** currentTerm */
                    .targetHost(targetHost)                 /** 执行采集任务的节点地址 */
                    .build();
            persistVal.getLog().add(logEntry);
            persist();
            int index = logEntry.getIndex();
            matchIndexs.put(peerSet.getSelf(), index);
            nextIndexs.put(peerSet.getSelf(), index + 1);
        }

        return logEntry;
    }

    @Override
    public JVMInfo handlerCapabilityRequest() {
        JVMInfo jvmInfo = JVMUtil.getJvmInfo();
        synchronized (lock) {
            jvmInfo.setTaskCount(RunningLogs.size());
        }

        double rate = jvmInfo.getFreeMem() * 1.0 / jvmInfo.getMaxMem();
        jvmInfo.setPeer(this.peerSet.getSelf());
        return jvmInfo;
    }

    @Override
    public ClientKVAck redirect(ClientKVReq request) {
        Request r = Request.newBuilder()
                .obj(request)
                .url(peerSet.getLeader().getAddr())
                .cmd(Request.CLIENT_REQ)
                .build();
        Response response = rpcClientImpl.send(r);
        return (ClientKVAck)response.getResult();
    }

    /** 直接执行采集任务 */
    @Override
    public Integer handlerStartCollectRequest(LogEntry logEntry){
        return startCollect(logEntry);
    }

    /** 执行采集任务，采集任务执行成功，直接变更内村 */
    public int startCollect(LogEntry logEntry) {
        /** 判断是否已经采集中了 */
        synchronized (lock) {
            for(LogEntry tempLogEntry : RunningLogs) {
                if(tempLogEntry.getIndex() == logEntry.getIndex() && tempLogEntry.getTerm() == logEntry.getTerm()) {
                    return ColExcutor.COLLECTSUCCESS;
                }
            }
        }

        /** 获取采集所需要的参数 */
        int resCode = colExcutor.startCollect(logEntry,1);

        /** 执行采集成功 */
        if(resCode == ColExcutor.COLLECTSUCCESS) {
            synchronized (lock) {
                /** 变更状态，需要加锁 */
                RunningLogs.add(logEntry);
            }
        }

        return resCode;
    }

    /** 用wait notify 模拟go的计时器超时事件 */
    public class HeartBeatTask extends Thread {
        @Override
        public void run() {
            while(true) {
                boolean flag = false;
                synchronized (heartBeatLock) {
                    /** 等待超时响应 */
                    try {
                        heartBeatLock.wait();
                        // 到达这里说明选举超时计时器超时了
//                        System.out.println("收到心跳超时消息了");
                        flag = true;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                /** 是正常被唤醒 */
                if(flag) {
                    synchronized (lock) {
                        if(status == LEADER) {
                            heartBeats();
                            heartBeatTimer.reset(new HeartBeatTimerTask(),HEART_BEAT_TIMEOUT);
                        }
                    }
                }
            }
        }
    }

    public class ElectionTask extends Thread {
        @Override
        public void run() {
            while(true) {
                boolean flag = false;
                synchronized (electionLock) {
                    /** 等待超时响应 */
                    try {
                        electionLock.wait();
//                        System.out.println("收到选举超时计时器消息了，现在马上开始选举");
                        // 到达这里说明选举超时计时器超时了
                        flag = true;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                /** 是正常被唤醒 */
                if(flag) {
                    synchronized (lock) {
//                        System.out.println("获得对象锁，进来了，准备转换角色");
                        switch (status) {
                            case FOLLOWER : {
//                                System.out.println("当前是FOLLOWER，转变成CANDIDATE");
                                switchRoleTo(CANDIDATE);
                                break;
                            }
                            case CANDIDATE : {
//                                System.out.println("当前是CANDIDATE，直接进行选举任务");
                                startElection();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /** 选举，调用者加锁 */
    public void startElection() {
        /** 如果当前已经是leader 就不需要执行选举操作 */
        if (status == NodeStatus.LEADER) {
            return;
        }

        int currentTerm = persistVal.getCurrentTerm() + 1;
        persistVal.setCurrentTerm(currentTerm);                             /** 任期号+1 */
        String votedFor = peerSet.getSelf().getAddr();
        persistVal.setVotedFor(votedFor);                                   /** 给自己投票 */
        persist();

        /** 变更了内容，现在持久化 */
        electionTimer.reset(new ElectionTimerTask(),randTimerDuration());   /** 重置选举超时计时器 */
        voteCount = 1;

        /** 开始选举 */
        List<Peer> peers = peerSet.getPeersWithoutSelf();				// 要投票的对象
        for (Peer peer : peers)	{
            RaftThreadPool.execute(() -> {
                // 因为是新的线程，所以需要加锁
                NodeImpl node = NodeImpl.getInstance();
                int lastLogIndex = 0;
                int lastLogTerm = 0;
                int currentTerm1 = 0;
                RvoteParam param;
                Request request;

                /** 构造rpc参数 */
                synchronized (node.lock) {
                    lastLogIndex = node.getPersistVal().getLog().size() - 1;
                    LogEntry logEntry = node.getPersistVal().getLog().get(lastLogIndex);
                    if(logEntry != null) {
                        lastLogTerm = logEntry.getTerm();
                    }
                    currentTerm1 = node.getPersistVal().getCurrentTerm();

                    // 请求投票RPC参数
                    param = RvoteParam.newBuilder()
                            .term(currentTerm1)
                            .candidateId(peerSet.getSelf().getAddr())
                            .lastLogIndex(lastLogIndex)
                            .lastLogTerm(lastLogTerm)
                            .build();

                    // rpc请求
                    request = Request.newBuilder()
                            .cmd(Request.R_VOTE)	// 类型
                            .obj(param)				// 内容
                            .url(peer.getAddr())	// 发送的对象
                            .build();
                }


                try	{
                    Response<RvoteResult> response = rpcClientImpl.send(request);
                    if(response != null) {
                        synchronized (node.lock) {
                            if(node.getStatus() == CANDIDATE && response.getResult().isVoteGranted()) {
                                int voteCount = node.getVoteCount() + 1;
                                node.setVoteCount(voteCount);
                                if(voteCount > peers.size() / 2) {
                                    // 成为Leader
//                                        System.out.println("当前节点["+ peerSet.getSelf().getAddr() +"]成为LEADER" +
//                                                ",当前任期号[ " + node.getPersistVal().getCurrentTerm() + " ]");
                                    node.switchRoleTo(LEADER);
                                }
                            } else {
                                // 请求投票失败，原因：
                                // 1. 对方比自己任期大
                                // 2. 对方的日志比自己要新（最后一条日志的索引号或者任期号）
                                // 3. 对方已经投票了
                                int resTerm = response.getResult().getTerm();
                                // 发现对方的任期比自己大，立刻变回FOLLOWER
                                if(resTerm > node.getPersistVal().getCurrentTerm()) {
                                    node.getPersistVal().setCurrentTerm(resTerm);       /** 变更当前任期 */
                                    node.switchRoleTo(FOLLOWER);                        /** 转变成为FOLLOWER */
                                    persist();                                          /** 持久化 */
                                }
                            }
                        }
                    } else {
                        System.out.println("没收到结果");
                    }
                    return;
                }
                catch (Exception e)	{
                    System.out.println("远程投票RPC失败 , 失败的节点URL : " + request.getUrl());
                    e.printStackTrace();
                    return;
                }
            },false);
        }
    }

    public void heartBeats() {
        /** 对于所有节点发送心跳，并且处理任务情况 */
        for(Peer peer : peerSet.getPeersWithoutSelf()) {
            // 对于每一个角色发送心跳
            RaftThreadPool.execute(() -> {
                int prevLogIndex,prevLogTerm,i;                 /** prevLogIndex prevLogTerm */
                LogEntry []entries;                             /** the entries need to send */
                int currentTerm;                                /** currentTerm */
                AentryParam param;                              /** AppendEntry Param */
                Request request;                                /** RPC Param */
                NodeImpl node = NodeImpl.getInstance();         /** raftNode */

                /** 构造面向peer的请求投票rpc报文 */
                synchronized (lock) {
                    if(status != LEADER) {
                        return;
                    }

                    prevLogIndex = nextIndexs.get(peer) - 1;
                    prevLogTerm  = persistVal.getLog().get(prevLogIndex).getTerm();
                    currentTerm = persistVal.getCurrentTerm();
                    entries = new LogEntry[persistVal.getLog().size() - prevLogIndex - 1];
                    for(i=0;i<entries.length;i++) {
                        entries[i] = persistVal.getLog().get(i + prevLogIndex + 1);
                    }

                    // 心跳包参数
                    param = AentryParam.newBuilder()
                            .term(currentTerm)                          /** 当前任期号 */
                            .leaderId(peerSet.getSelf().getAddr())      /** 领导人地址，用于重定向 */
                            .prevLogIndex(prevLogIndex)                 /** 这次发送日志的上一条日志的索引 */
                            .prevLogTerm(prevLogTerm)                   /** 这次发送日志的上一条日志的任期号 */
                            .entries(entries)                           /** 这次要发送的日志内容 */
                            .leaderCommit(commitIndex)                  /** 已知领导人提交的任期索引号 */
                            .serverId(peer.getAddr())                   /** 心跳要发送的队形的地址 */
                            .build();

                    // request 参数构造
                    request = Request.newBuilder()
                            .cmd(Request.A_ENTRIES)
                            .obj(param)
                            .url(peer.getAddr())
                            .build();
                }

                try {
                    Response<AentryResult> response = rpcClientImpl.send(request);
                    if(response != null) {
                        // 心跳处理成功
                        ArrayList<LogEntry> peerLogs = null;
                        synchronized (lock) {
                            if(status != LEADER) {
                                return;
                            }

                            /** 设置节点宕机次数为0 */
                            OfflineMap.put(peer.getAddr(),0);

                            /** 处理附加日志相关情况 */
                            if(response.getResult().isSuccess()) {
                                // successfully replicated args.Entries 成功将缺少的日志附加上去了，提高效率
                                int matchIndex = param.getPrevLogIndex() + entries.length;
                                node.getMatchIndexs().put(peer,matchIndex);
                                node.getNextIndexs().put(peer,matchIndex+1);

                                // if there exists an N such that N > commitIndex ,
                                // and a majority of matchIndex[peer] ≥ N, and log[N].term == currentTerm
                                // set commitIndex = N
                                int N;
                                for(N=persistVal.getLog().size()-1;N>commitIndex;N--) {
                                    int count = 0;
                                    for(Peer peer1 : peerSet.getPeers()) {
                                        if(matchIndexs.get(peer1) >= N) {
                                            count++;
                                        }
                                    }
                                    if(count > peerSet.getPeers().size() / 2) {
                                        System.out.println("fuck 草，够了，持久化");
                                        node.setNowCommitIndex(N);
                                        break;
                                    }
                                }
                            } else {
                                /**
                                 *  失败，两种原因
                                 *  1 对方的任期号比当前大，当前节点成为FOLLOWER
                                 *  2 附加日志失败，prevLogIndex位置日志不匹配 降低pervLogIndex并重试
                                 */
                                if(response.getResult().getTerm() > currentTerm) {
                                    node.getPersistVal().setCurrentTerm(response.getResult().getTerm());
                                    node.switchRoleTo(FOLLOWER);
                                    persist();
                                } else {
                                    // 不匹配 nextIndex - 1 重试
                                    int nextIndex = nextIndexs.get(peer) - 1;
                                    node.getNextIndexs().put(peer,nextIndex);
                                }
                            }

                            /** 获取该节点实际执行的任务 */
                            peerLogs = response.getResult().getRunningTasks();
                        }
                        peerCheckTask(peerLogs,peer);
                    }
                    else {  // 该节点没有反应
                        int offlineCount = 0;
                        synchronized (lock) {
                           offlineCount = OfflineMap.get(peer.getAddr()) + 1;
                           OfflineMap.put(peer.getAddr(),offlineCount);
                        }
                        if(offlineCount == 5) { // 已经5次没有返回消息了，确诊为宕机，执行任务恢复策略
                            recoverHost(peer);
                        }
                    }
                } catch (Exception e) {
                    // System.out.println("心跳请求RPC失败 , 失败的节点URL : " + request.getUrl());
                }
            },false);
        }

        /** 对于自己的任务处理 */
        selfCheckTask();
    }

    /** 将CurrentTerm、VotedFor、Logs持久化到redis中,由调用方加锁 */
    @Override
    public void persist() {
        // persistVal from mem to Redis
        String value = gson.toJson(persistVal);
        StoreUtil.write("",LOGKEY,value);
    }

    /** 将CurrentTerm、VotedFor、Logs读取到内存中，由调用方加锁 */
    @Override
    public void readPersist() {
        // persistval from Redis to mem
        String persistValJson = StoreUtil.read("",LOGKEY);
        if(persistValJson == null || persistValJson.length() == 0) {
            return;
        }
        persistVal = gson.fromJson(persistValJson,RaftPersistVal.class);
    }

    /** 改变当前节点的角色，需要加锁，由调用方加锁和进行信息持久化 */
    public void switchRoleTo(int ROLE) {
        if(this.status == ROLE) {
            return;
        }

        status = ROLE;

        switch (ROLE) {
            case FOLLOWER:{
                /** 停止心跳线程的定时运行，重置选举超时时间，设置当前选举对象为空 */
                heartBeatTimer.stop();
                electionTimer.reset(new ElectionTimerTask(),randTimerDuration());
                persistVal.setVotedFor("");
                break;
            }
            case CANDIDATE:{
                /** 立刻执行选举任务 */
                startElection();
                break;
            }
            case LEADER:{
                /** 重置NextIndex数组和MatchIndex数组，关闭选举线程，执行心跳线程 */
                nextIndexs = new ConcurrentHashMap<>();
                matchIndexs = new ConcurrentHashMap<>();

                int len = persistVal.getLog().size();
                for(Peer peer : peerSet.getPeers()) {
                    nextIndexs.put(peer,len);
                    matchIndexs.put(peer,0);
                }
                electionTimer.stop();                                                       /** 关闭选举超时计时器 */
                heartBeats();                                                               /** 立刻执行心跳操作 */
                heartBeatTimer.reset(new HeartBeatTimerTask(),HEART_BEAT_TIMEOUT);          /** 重置心跳超时计时器 */
                break;
            }
        }
    }

    /** 设置已提交索引号调用方加锁 */
    public void setNowCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
        // apply all entries between lastApplied and commitIndex
        // should be called after commitIndex update
        if(commitIndex > lastApplied) {
            int len = commitIndex - lastApplied;
            List<LogEntry> entries = new ArrayList<>();
            for(int i=0;i<len;i++) {
                entries.add(persistVal.getLog().get(lastApplied + i + 1));
            }

            RaftThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    // 将日志应用到状态机 并告诉客户端
                    // raft.apply(log)
                    NodeImpl node = NodeImpl.getInstance();
                    node.apply(entries);
                }
            },false);
        }
    }

    /** 生成一个随机选举超时时间 */
    public long randTimerDuration() {
        return HEART_BEAT_TIMEOUT * 3 + random.nextInt((int)(2 * HEART_BEAT_TIMEOUT));
    }

    /** 将list的内容持久化到状态机中 新线程 由调用方加锁 */
    public void apply(List<LogEntry> logs) {
        /** 持久化到redis */
        for(LogEntry logEntry : logs) {
            if(logEntry.getCommand().getOpType() == Command.DEL) {              /** del log */
                /** 变更状态机 */
                StoreUtil.delete(STATEKEY,logEntry.getCommand().getKey());
                /** 变更内存 */
                String targetHost = logEntry.getTargetHost();
                ArrayList<LogEntry> tasks;
                tasks = RunningTasksMap.get(targetHost);
                tasks.removeIf(log -> log.getCommand().getKey().equals(logEntry.getCommand().getKey()) &&
                        log.getCommand().getValue().equals(logEntry.getCommand().getValue()));
                RunningTasksMap.put(targetHost,tasks);
            } else if(logEntry.getCommand().getOpType() == Command.ADD) {
                State state = State.newBuilder()
                    .value(logEntry.getCommand().getValue())
                    .targetAddr(logEntry.getTargetHost())
                    .build();
                String value = gson.toJson(state);
                StoreUtil.write(STATEKEY,logEntry.getCommand().getKey(),value);

                /** 变更内存的map */
                String targetHost = logEntry.getTargetHost();
                ArrayList<LogEntry> tasks;
                tasks = RunningTasksMap.get(targetHost);
                tasks.add(logEntry);
                RunningTasksMap.put(targetHost,tasks);
            } else if(logEntry.getCommand().getOpType() == Command.SET) {
                /** 删除上一个节点的信息，在当前节点添加信息 */
                State state = State.newBuilder()
                        .value(logEntry.getCommand().getValue())
                        .targetAddr(logEntry.getTargetHost())
                        .build();
                String value = gson.toJson(state);
                StoreUtil.write(STATEKEY,logEntry.getCommand().getKey(),value); // kv状态机直接变更就可以

                // 从故障节点对应的链表中删除
                String lastTargetHost = logEntry.getLastTargetHost();
                ArrayList<LogEntry> tasks;
                tasks = RunningTasksMap.get(lastTargetHost);
                tasks.removeIf(log -> log.getCommand().getKey().equals(logEntry.getCommand().getKey()) &&
                        log.getCommand().getValue().equals(logEntry.getCommand().getValue()));
                RunningTasksMap.put(lastTargetHost,tasks);

                // 向新的链表添加
                String targetHost = logEntry.getTargetHost();
                tasks = RunningTasksMap.get(targetHost);
                tasks.add(logEntry);
                RunningTasksMap.put(targetHost,tasks);
            }

            /** 变更上一条持久化日志的下标 */
            if(lastApplied < logEntry.getIndex()) {
                lastApplied = logEntry.getIndex();
            }
        }
    }

    /** 向每一个节点发送性能探测包，对返回的结果进行分析，选取一个最合适的节点 */
    public Peer findTargetPeer() {
        /** leader向所有节点发送性能探测请求 */
        JVMInfo selfJvmInfo = JVMUtil.getJvmInfo();
        // 比较的参数，freeMem / maxMem 默认为1，要求选一个最小的
        double memRate = 1.0 * selfJvmInfo.getFreeMem() / selfJvmInfo.getMaxMem();
        int taskCount = 0;
        synchronized (lock) {
            taskCount = RunningLogs.size();
        }
        // 目标Peer 默认为自己
        Peer targetPeer = this.getPeerSet().getSelf();

        List<Future<JVMInfo>> memInfoFutures = new CopyOnWriteArrayList<>();
        for(Peer peer : peerSet.getPeersWithoutSelf()) {
            memInfoFutures.add(getJVMInfo(peer));
        }

        CountDownLatch countDownLatch = new CountDownLatch(memInfoFutures.size());
        List<JVMInfo> jvmInfos = new CopyOnWriteArrayList<>();

        for(Future<JVMInfo> future : memInfoFutures) {
            RaftThreadPool.execute(() -> {
                JVMInfo jvmInfo = null;
                try	{
                    jvmInfo = future.get(HEART_BEAT_TIMEOUT,TimeUnit.MILLISECONDS);	// 等待结果
                }
                catch (CancellationException | TimeoutException | ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    jvmInfo = null;
                }
                finally {
                    jvmInfos.add(jvmInfo);
                    countDownLatch.countDown();
                }
            });
        }

        try {
            countDownLatch.await(300,TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        /** 找到处理的节点 */
        for(JVMInfo jvmInfo : jvmInfos) {
            if(jvmInfo != null) {
                double rate = jvmInfo.getFreeMem() * 1.0 / jvmInfo.getMaxMem();	// [freeMemory / maxMemory]
                /** 如果对方的内存占用情况比当前节点的占用情况要好,或者对方和当前节点占用情况一样，但是执行的任务少 */
                int compare = Double.compare(rate,memRate);
                if(compare > 0 || compare == 0 && jvmInfo.getTaskCount() < taskCount){
                    memRate = rate;
                    targetPeer = jvmInfo.getPeer();
                    taskCount = jvmInfo.getTaskCount();
                }
            }
        }
        return targetPeer;
    }

    /** leader向followers发送获取性能rpc后分析结果 */
    public Future<JVMInfo> getJVMInfo(Peer peer) {
        return RaftThreadPool.submit(() -> {
            Request request = Request.newBuilder()
                    .cmd(Request.CAPABILITY_REQ)
                    .obj(null)
                    .url(peer.getAddr())
                    .build();

            Response response = rpcClientImpl.send(request);

            if(response == null) {
                JVMInfo jvmInfo = JVMInfo.getInstanceNoSin();
                jvmInfo.setPeer(peer);
                jvmInfo.setTotalMem(1);
                jvmInfo.setMaxMem(1);
                jvmInfo.setFreeMem(0);
                jvmInfo.setTaskCount(MAXTASKCOUNT);
                return jvmInfo;
            }
            return response.getResult();
        });
    }

    /** 任务执行失败，从runninglogs中删除指定内容 */
    public void removeTasks(LogEntry logEntry) {
        if(logEntry == null) {
            return;
        }
        synchronized (lock) {
            RunningLogs.removeIf(log -> log.getIndex() == logEntry.getIndex() && log.getTerm() == logEntry.getTerm());
        }
    }

    /**
     * 为什么要把自己和peer的任务执行情况拆开处理，明明很多相同的逻辑，理由是因为放到一起需要有一个if，在我们的系统里不断地心跳
     * 每次心跳都去做if ， 计算机系统if的指令执行速度最慢，所以不这么做
     * */
    /** 检测自己的任务执行情况 */
    private void selfCheckTask() {
        ArrayList<LogEntry> peerLogs = null;
        synchronized (lock) {
            if(status != LEADER) {
                return;
            }
            peerLogs = RunningTasksMap.get(peerSet.getSelf().getAddr());
        }

        if(peerLogs.size() == 0) {
            return;
        }

        HashMap<String,LogEntry> map = new HashMap<>();
        for(LogEntry log : RunningLogs) {
            String key = log.getIndex()+","+log.getTerm();
            map.put(key,log);
        }

        for(LogEntry logEntry : peerLogs) {
            String key = logEntry.getIndex()+","+logEntry.getTerm();
            if(map.get(key) == null) {
                /** 异步线程，内部访问共享变量需要自己加锁 */
                RaftThreadPool.execute(() -> {
                    int colRes = startCollect(logEntry);
                    sloveRes(colRes,logEntry);
                },false);
            }
        }
    }

    /** 检测任务执行情况，并让其恢复任务 */
    public void peerCheckTask(ArrayList<LogEntry> logs,Peer peer) {
        /** log是peer节点实际执行的任务，peerLogs是peer节点应该执行的任务 */
        ArrayList<LogEntry> peerLogs = null;
        synchronized (lock) {
            if(status != LEADER) {
                return;
            }
            peerLogs = RunningTasksMap.get(peer.getAddr());
        }

        if(peerLogs.size() == 0 ) {
            return;
        }
        HashMap<String,LogEntry> map = new HashMap<>();
        for(LogEntry log : logs) {
            String key = log.getIndex()+","+log.getTerm();
            map.put(key,log);
        }

        for(LogEntry logEntry : peerLogs) {
            String key = logEntry.getIndex()+","+logEntry.getTerm();
            if(map.get(key) == null) {
                /** 异步线程，内部访问共享变量需要自己加锁 */
                RaftThreadPool.execute(() -> {
                    int colRes = 0;
                    Request startRequest = Request.newBuilder()
                            .url(peer.getAddr())
                            .cmd(Request.START_COLLECT)
                            .obj(logEntry)
                            .build();
                    Response<Integer> startResponse = rpcClientImpl.send(startRequest);
                    if(startResponse != null) {
                        colRes = startResponse.getResult();
                    } else {
                        // 认为是误判，不做处理 因为到这之前已经接收到心跳反应，所以认为本次是网络波动
                        return;
                    }

                    /** 尝试恢复任务，恢复失败 */
                    sloveRes(colRes,logEntry);
                },false);
            }
        }
    }

    /** 如果采集不顺利，执行日志删除操作 */
    private void sloveRes(int colRes,LogEntry logEntry) {
        if(colRes != ColExcutor.COLLECTSUCCESS) {
            synchronized (lock) {
                LogEntry delLog = LogEntry.newBuilder()
                        .targetHost(logEntry.getTargetHost())
                        .index(persistVal.getLog().size())
                        .term(persistVal.getCurrentTerm())
                        .command(Command.newBuilder()
                                .opType(Command.DEL)
                                .key(logEntry.getCommand().getKey())
                                .value(logEntry.getCommand().getValue())
                                .build())
                        .build();
                persistVal.getLog().add(delLog);
                persist();
                int index = delLog.getIndex();
                matchIndexs.put(peerSet.getSelf(), index);
                nextIndexs.put(peerSet.getSelf(), index + 1);
            }
        }
    }

    /** 恢复故障节点任务，自己加锁加锁 */
    private void recoverHost(Peer peer) {
        /** 获取该节点应该执行的任务，然后将每一个任务分配出去 */
        ArrayList<LogEntry> peerLogs = null;
        synchronized (lock) {
            /** 获取当前节点应该执行的任务，然后重新分配出去 */
            peerLogs = RunningTasksMap.get(peer.getAddr());
            if (peerLogs.size() == 0) {
                return;
            }
        }
        /** 对每一个日志，进行变更操作 */
        for(LogEntry oldLog : peerLogs) {
            /** 找到新的执行节点 */
            Peer newTargetHost = findTargetPeer();
            LogEntry logEntry = null;
            synchronized (lock) {
                logEntry = LogEntry.newBuilder()
                        .command(Command.newBuilder()
                                .key(oldLog.getCommand().getKey())      /** deviceId */
                                .value(oldLog.getCommand().getValue())  /** collectingConfig */
                                .opType(Command.SET)                    /** 建立采集 */
                                .build())
                        .index(persistVal.getLog().size())              /** nowIndex */
                        .term(persistVal.getCurrentTerm())              /** currentTerm */
                        .targetHost(newTargetHost.getAddr())            /** 执行采集任务的节点地址 */
                        .lastTargetHost(oldLog.getTargetHost())
                        .build();
                persistVal.getLog().add(logEntry);
                persist();
                int index = logEntry.getIndex();
                matchIndexs.put(peerSet.getSelf(), index);
                nextIndexs.put(peerSet.getSelf(), index + 1);
            }
        }
    }
}