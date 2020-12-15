package rpc.userprocessor;

import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.protocol.SyncUserProcessor;
import model.clientmodel.ClientKVReq;
import model.consensusmodel.aentry.AentryParam;
import model.consensusmodel.rvote.RvoteParam;
import model.logmodulemodel.LogEntry;
import model.monitor.JVMInfo;
import model.rpcmodel.Request;
import model.rpcmodel.Response;
import node.NodeImpl;

public class RaftUserProcessor extends SyncUserProcessor<Request> {
    private NodeImpl nodeImpl;
    public RaftUserProcessor( NodeImpl nodeImpl ) {
        this.nodeImpl = nodeImpl;
    }

    @Override
    public Object handleRequest(BizContext bizContext, Request request) throws Exception {
        // 接收到请求投票RPC
        if (request.getCmd() == Request.R_VOTE) {
            RvoteParam rvoteParam = (RvoteParam) request.getObj();
            Response response = new Response(nodeImpl.handlerRequestVote(rvoteParam));
            return response;
        }
        // 接收到附加日志RPC
        else if (request.getCmd() == Request.A_ENTRIES) {
            AentryParam aentryParam = (AentryParam) request.getObj();
            return new Response(nodeImpl.handlerAppendEntries(aentryParam));
        }
        // 客户端请求RPC
        else if (request.getCmd() == Request.CLIENT_REQ) {
            // 客户端请求RPC 边缘服务器->Raspberry
            return new Response(nodeImpl.handlerClientRequest((ClientKVReq) request.getObj()));
        }
        // 获取节点性能RPC
        else if (request.getCmd() == Request.CAPABILITY_REQ) {
            System.out.println("接收到来自节点 Leader的获取性能请求参数");
            JVMInfo jvmInfo = nodeImpl.handlerCapabilityRequest();
            System.out.println("当前节点的性能参数" + jvmInfo);
            return new Response(jvmInfo);
        }
        // 执行开始采集任务
        else if (request.getCmd() == Request.START_COLLECT) {
            System.out.println("接受到来自节点Leader的开始采集请求");
            Integer res = nodeImpl.handlerStartCollectRequest( (LogEntry) request.getObj());
            System.out.println("开始采集执行的结果是["+res+"]");
            return new Response<>(res);
        }
        return null;
    }
    /**
     * 指定感兴趣的请求数据类型，该 UserProcessor 只对感兴趣的请求类型的数据进行处理；
     * 假设 除了需要处理 MyRequest 类型的数据，还要处理 java.lang.String 类型，有两种方式：
     * 1、再提供一个 UserProcessor 实现类，其 interest() 返回 java.lang.String.class.getName()
     * 2、使用 MultiInterestUserProcessor 实现类，可以为一个 UserProcessor 指定 List<String> multiInterest()
     */
    @Override
    public String interest() {
        return Request.class.getName();
    }
}
