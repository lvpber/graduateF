package node;

import lifecycle.ILifeCycle;
import model.clientmodel.ClientKVAck;
import model.clientmodel.ClientKVReq;
import model.colconfig.BaseColConfig;
import model.consensusmodel.aentry.AentryParam;
import model.consensusmodel.aentry.AentryResult;
import model.consensusmodel.rvote.RvoteParam;
import model.consensusmodel.rvote.RvoteResult;
import model.logmodulemodel.LogEntry;
import model.monitor.JVMInfo;
import model.node.NodeConfig;
import model.rpcmodel.Response;

public interface INode extends ILifeCycle {
        void 			setConfig       				(NodeConfig config);			/** 设置配置文件 */
        RvoteResult     handlerRequestVote      		(RvoteParam param);				/** 处理请求投票 */
        AentryResult    handlerAppendEntries    	    (AentryParam param);			/** 处理附加日志请求 */
        ClientKVAck     handlerClientRequest    	    (ClientKVReq request);			/** 处理客户端请求 */
        ClientKVAck 	redirect			    	    (ClientKVReq request);			/** 当客户端发送请求的对象不是Leader时 */
        JVMInfo         handlerCapabilityRequest        ();								/** 获取当前节点性能参数 */
        Integer			handlerStartCollectRequest      (LogEntry logEntry);			/** 处理startCollect请求	*/
        void            persist                         ();                             /** 持久化部分状态信息,CurrentTerm,VotedFor,Logs */
        void            readPersist                     ();                             /** 从持久化内容中读取当前节点状态 */
}
