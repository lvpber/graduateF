package model.colconfig;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class BaseColConfig implements Serializable {
    public static final String CLIENT = "tcp_client";
    public static final String SERVER = "tcp_server";

    private int     frequency;				/** 采集的频率 */
    private String  deviceId;			    /** 操作的设备 */
    private String  number;				    /** 采集编号 */
    private String  operatorNumber;		    /** 采集人员编号 */
    private String  operatorName;		    /** 采集人员姓名 */
    private String  description;			/** 采集描述 */
    private String  driverFilePath;		    /** 采集驱动文件本地路径 */
    private String  protocol;			    /** 通信协议名称 */
}
