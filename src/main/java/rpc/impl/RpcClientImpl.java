package rpc.impl;

import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import exception.rpcexception.RaftRemotingException;
import model.rpcmodel.Request;
import model.rpcmodel.Response;
import rpc.IRpcClient;

public class RpcClientImpl implements IRpcClient {
    private final static RpcClient rpcClient = new RpcClient();

    static {
        rpcClient.startup();
    }

    @Override
    public Response send(Request request) {
        Response result = null;
        try {
            result = (Response) rpcClient.invokeSync(request.getUrl(), request, 150);
        }
        catch (RemotingException e) {
            throw new RaftRemotingException();
        }
        finally {
            return result;
        }
    }
}
