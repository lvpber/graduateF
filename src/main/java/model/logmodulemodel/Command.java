package model.logmodulemodel;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
public class Command implements Serializable {
    public static final int DEL = 0;
    public static final int ADD = 1;
    public static final int SET = 2;

    private Integer opType;         /** 操作类型，删除或者设置 */
    private String  key;            /** 被采集设备id */
    private String  value;          /** 采集描述 */

    @Override
    public String toString() {
        return "Command{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", opType='" + opType + '\'' +
                '}';
    }

    public Command(String key, String value) {
        this.key = key;
        this.value = value;
    }

    private Command(Builder builder) {
        setKey(builder.key);
        setValue(builder.value);
        setOpType(builder.opType);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Command command = (Command) o;
        return Objects.equals(key, command.key) &&  Objects.equals(value, command.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    public static final class Builder {

        private String key;
        private String value;
        private Integer opType;

        private Builder() {
        }

        public Builder key(String val) {
            key = val;
            return this;
        }

        public Builder value(String val) {
            value = val;
            return this;
        }

        public Builder opType(Integer opType) {
            this.opType = opType;
            return this;
        }

        public Command build() {
            return new Command(this);
        }
    }
}
