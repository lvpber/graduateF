package model.state;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class State implements Serializable {
    private String targetAddr;          /** 最终执行采集任务的节点 */
    private String value;               /** 采集的任务描述 */

    @Override
    public String toString() {
        return "State{" +
                "targetAddr='" + targetAddr + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    private State(Builder builder) {
        setValue(builder.value);
        setTargetAddr(builder.targetAddr);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {

        private String targetAddr;
        private String value;

        private Builder() {
        }

        public Builder targetAddr(String targetAddr) {
            this.targetAddr = targetAddr;
            return this;
        }

        public Builder value(String val) {
            value = val;
            return this;
        }

        public State build() {
            return new State(this);
        }
    }
}


