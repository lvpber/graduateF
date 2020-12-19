package rpc;

import model.rpcmodel.Request;
import model.rpcmodel.Response;

// 发送请求
public interface IRpcClient {
    Response send(Request request);
}
