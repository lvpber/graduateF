package model.clientmodel;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ClientKVAck implements Serializable {
    private boolean isSuccess;      /** 处理是否成功 */
    private Object result;          /** 返回的结果 */

    public ClientKVAck(Object obj){result = obj;}

    private ClientKVAck(Builder builder) {
        setResult(builder.result);
        setSuccess(builder.isSuccess);
    }

    public static ClientKVAck ok(){return new ClientKVAck("ok");}

    public static ClientKVAck fail(){return new ClientKVAck("fail");}

    public static Builder newBuilder(){return new Builder();}

    public static final class Builder {
        private boolean isSuccess;
        private Object  result;

        private Builder(){}

        public Builder result(Object val) {
            this.result = val;
            return this;
        }

        public Builder isSuccess(boolean isSuccess) {
            this.isSuccess = isSuccess;
            return this;
        }

        public ClientKVAck build(){return new ClientKVAck(this);}
    }
}
