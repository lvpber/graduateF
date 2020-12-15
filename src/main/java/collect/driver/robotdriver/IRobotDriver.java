package collect.driver.robotdriver;

import collect.driver.IDeviceDriver;
import exception.collectingexception.ConnectInterruptedException;
import model.kafkamsg.axlemsg.AxleDynMsg;
import model.kafkamsg.robotmsg.RobotDynMsg;

public interface IRobotDriver extends IDeviceDriver {
    /**
     * @apiNote	    获取机器人动态信息
     * @return     机器人动态信息，按照 key:value 形式组成字符串 或者直接返回json字符串
     */
    RobotDynMsg getRobotDynamicData() throws ConnectInterruptedException;

    /**
     * @apiNote	    获取机器人静态信息
     * @return     机器人静态信息，按照 key:value 形式组成字符串 或者直接返回json字符串
     */
    String getRobotStaticData() throws ConnectInterruptedException;

    /**
     * @apiNote	    获取机器人轴动态信息
     * @return     机器人轴动态信息，按照 key:value 形式组成字符串 或者直接返回json字符串
     */
    AxleDynMsg getAxleDynameData() throws ConnectInterruptedException;

    /**
     * @apiNote	    获取机器人轴静态信息
     * @return     机器人轴静态信息，按照 key:value 形式组成字符串 或者直接返回json字符串
     */
    String getAxleStaticData() throws ConnectInterruptedException;
}
