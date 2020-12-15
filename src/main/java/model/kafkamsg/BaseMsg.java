package model.kafkamsg;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class BaseMsg implements Serializable {
    private String  messageTypeName;        /** 消息类型名称 [robotdynamic,robotstatic,axledynamic,axlestatic] */
    private String  deviceId;               /** 设备Uuid 云上使用*/
    private long    timestamp;              /** 设备采集时间戳 */
}
