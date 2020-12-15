package rpc.impl;

import com.alipay.remoting.rpc.RpcServer;
import node.NodeImpl;
import rpc.IRpcServer;
import rpc.userprocessor.RaftUserProcessor;

public class RpcServerImpl implements IRpcServer {
    private RpcServer rpcServer;
    private NodeImpl nodeImpl;			/** 节点，用来处理请求 */
    public RpcServerImpl(){}
    public RpcServerImpl(int port, NodeImpl nodeImpl) {
        rpcServer = new RpcServer(port);
        /** 注册业务逻辑处理器 UserProcessor UserProcessor的任务是监听指定的请求类型，比如本例子中的request 然后处理这种请求 */
        rpcServer.registerUserProcessor(new RaftUserProcessor(nodeImpl));
        this.nodeImpl = nodeImpl;
    }

    @Override
    public void start() {
        rpcServer.startup();
    }

    @Override
    public void stop() {
        rpcServer.shutdown();
    }
}
