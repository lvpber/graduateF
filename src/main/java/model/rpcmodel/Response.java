package model.rpcmodel;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Response<T> implements Serializable {
    /** run try connect the robot */
    private int code;
    private T result;

    private Response(Builder builder) {
        setResult((T) builder.result);
        setCode(builder.code);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public Response(T result){this.result = result;}

    @Override
    public String toString() {
        return "Response{" +
                "result=" + result +
                '}';
    }
    public static final class Builder {
        private int code;
        private Object result;

        private Builder() {
        }

        public Builder result(Object val) {
            result = val;
            return this;
        }

        public Builder code(int code) {
            this.code = code;
            return this;
        }

        public Response build() {
            return new Response(this);
        }
    }
}
