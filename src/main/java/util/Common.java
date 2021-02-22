package util;

public interface Common {
    // redis config
//    String  ADDR = "192.168.0.67";                  // 杭研院服务器地址
//    String  ADDR = "192.168.3.188";                 // 实验室台式机的虚拟机地址
    String  ADDR = "192.168.93.129";                // 本机虚拟机地址
    Integer PORT = 6379;                            // Redis的端口号
    String  AUTH = "redis2333";                     // 访问密码

    // Robot simulate Config
    /** 杭研院配置 */
//    String NODE01 = "192.168.0.26:8775";    // ubuntu-node02
//    String NODE02 = "192.168.0.26:8776";
//    String NODE03 = "192.168.0.42:8775";    // ubuntu-node04
//    String NODE04 = "192.168.0.42:8776";
//    String NODE05 = "192.168.0.46:8775";    // ubuntu-node05
//    String Robot_Addr = "192.168.0.65";     // end_robot_node02

    /** 树莓派 */
//    String NODE01 = "192.168.3.213:8775";
//    String NODE02 = "192.168.3.243:8775";
//    String NODE03 = "192.168.3.148:8775";
//    String NODE04 = "192.168.3.253:8775";
//    String NODE05 = "192.168.3.129:8775";
//    String Robot_Addr = "192.168.3.188";

    /** 北航虚拟机上节点配置 */
//    String NODE01 = "localhost:8775";
//    String NODE02 = "localhost:8776";
//    String NODE03 = "localhost:8777";
//    String NODE04 = "localhost:8778";
//    String NODE05 = "localhost:8779";
//    String Robot_Addr = "192.168.3.188";

    /** 本台电脑虚拟机节点配置 */
    String NODE01 = "192.168.93.129:8775";
    String NODE02 = "192.168.93.129:8776";
    String NODE03 = "192.168.93.129:8777";
    String NODE04 = "192.168.93.129:8778";
    String NODE05 = "192.168.93.129:8779";
    String Robot_Addr = "192.168.93.129";
}
