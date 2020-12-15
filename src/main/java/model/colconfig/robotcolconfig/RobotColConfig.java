package model.colconfig.robotcolconfig;

import lombok.Getter;
import lombok.Setter;
import model.colconfig.BaseColConfig;


@Getter
@Setter
public class RobotColConfig extends BaseColConfig {
    private Long    workTypeId;         /** 采集工艺Id */
    private String  connectRole;        /** 连接角色 */
    private String  ipAddr;             /** 连接ip地址 */
    private int     port;               /** 连接port */

    public RobotColConfig(){}

    private RobotColConfig(Builder builder)  {
        setWorkTypeId(builder.workTypeId);
        setConnectRole(builder.connectRole);
        setIpAddr(builder.ipAddr);
        setPort(builder.port);
        setFrequency(builder.frequency);
        setDeviceId(builder.deviceId);
        setNumber(builder.number);
        setOperatorNumber(builder.operatorNumber);
        setOperatorName(builder.operatorName);
        setDescription(builder.description);
        setDriverFilePath(builder.driverFilePath);
        setProtocol(builder.protocol);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private Long    workTypeId;         /** 采集工艺Id */
        private String  connectRole;        /** 连接角色 */
        private String  ipAddr;             /** 连接ip地址 */
        private int     port;               /** 连接port */
        private int     frequency;				/** 采集的频率 */
        private String  deviceId;			    /** 操作的设备 */
        private String  number;				    /** 采集编号 */
        private String  operatorNumber;		    /** 采集人员编号 */
        private String  operatorName;		    /** 采集人员姓名 */
        private String  description;			/** 采集描述 */
        private String  driverFilePath;		    /** 采集驱动文件本地路径 */
        private String  protocol;			    /** 通信协议名称 */

        private Builder() {}

        public Builder workTypeId(Long workTypeId) {
            this.workTypeId = workTypeId;
            return this;
        }

        public Builder connectRole(String connectRole) {
            this.connectRole = connectRole;
            return this;
        }

        public Builder ipAddr(String ipAddr) {
            this.ipAddr = ipAddr;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder frequency(int frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder number(String number) {
            this.number = number;
            return this;
        }

        public Builder operatorName(String operatorName) {
            this.operatorName = operatorName;
            return this;
        }

        public Builder operatorNumber(String operatorNumber) {
            this.operatorNumber = operatorNumber;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder driverFilePath(String driverFilePath) {
            this.driverFilePath = driverFilePath;
            return this;
        }

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public RobotColConfig build() {
            return new RobotColConfig(this);
        }
    }
}
