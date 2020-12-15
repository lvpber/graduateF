package model.monitor;

import lombok.Getter;
import lombok.Setter;
import model.peer.Peer;

import java.io.Serializable;

@Getter
@Setter
public class JVMInfo implements Serializable {
    private Peer peer;      // 节点标志
    private long freeMem;   // jvm剩余内存，单位mb
    private long totalMem;  // jvm所分配内存的剩余量
    private long maxMem;    // jvm最大可获得内存量
    private int  taskCount; // 节点执行的任务数目

    private JVMInfo() {}

    private static class SingleTonHolder {
        private static JVMInfo jvmInfo = new JVMInfo();
    }

    /** 单例模式 */
    public static JVMInfo getInstance() {return SingleTonHolder.jvmInfo;}

    /** 非单例模式 */
    public static JVMInfo getInstanceNoSin() {return new JVMInfo();}

}
