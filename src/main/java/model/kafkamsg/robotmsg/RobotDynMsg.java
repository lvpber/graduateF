package model.kafkamsg.robotmsg;

/** 机器人动态信息 */

import lombok.Getter;
import lombok.Setter;
import model.kafkamsg.BaseMsg;

@Getter
@Setter
public class RobotDynMsg extends BaseMsg {
    private String  cart_pos;       /** 当前笛卡尔位置 */
    private String  alarm_info;     /** 当前错误码 */
    private String  state;          /** 当前控制系统状态 */
    private long    happenTime;     /** 实际采集时间 */
}
