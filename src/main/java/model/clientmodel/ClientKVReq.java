package model.clientmodel;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ClientKVReq implements Serializable {
    public static int PUT = 0;
    public static int GET = 1;
    private int type;               /** 指令类型，读取状态还是设置状态 */
    private String key;             /** 指令的key deviceId */
    private String value;           /** 指令的内容，具体的CollectingConfig */
    /** leader接受任务请求之后会返回生成的日志信息，用户根据日志信息请求处理结果 */
    private int index;              /** 日志的索引号 */
    private int term;               /** 日志的任期号 */

    private ClientKVReq(Builder builder) {
        setType(builder.type);
        setKey(builder.key);
        setValue(builder.value);
        setIndex(builder.index);
        setTerm(builder.term);
    }

    public static Builder newBuilder(){return new Builder();}

    public enum Type  {
        PUT(0),GET(1),
        ;
        int code;
        Type(int code){this.code = code;}

        public static Type value(int code) {
            for(Type type : values()) {
                if(type.code == code)
                    return type;
            }
            return null;
        }
    }

    public static final class Builder {
        private int type;
        private String key;
        private String value;
        private int index;
        private int term;

        private Builder(){}

        public Builder type(int val) {
            this.type = val;
            return this;
        }

        public Builder key(String val) {
            key = val;
            return this;
        }

        public Builder value(String val) {
            value = val;
            return this;
        }

        public Builder index(int index) {
            this.index = index;
            return this;
        }

        public Builder term(int term) {
            this.term = term;
            return this;
        }

        public ClientKVReq build(){return new ClientKVReq(this);}
    }
}
