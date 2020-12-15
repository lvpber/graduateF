package model.kafkamsg.axlemsg;

import lombok.Getter;
import lombok.Setter;
import model.kafkamsg.BaseMsg;

@Getter
@Setter
public class AxleDynMsg extends BaseMsg {
    private String  jnt_pos;        /** 当前轴角度 */
    private String  jnt_vel;        /** 当前轴速度 */
    private String  jnt_trq;        /** 当前轴力矩千分比系数 */
    private long    happenTime;     /** 实际采集时间 */
}
