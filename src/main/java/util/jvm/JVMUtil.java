package util.jvm;

import model.monitor.JVMInfo;

public class JVMUtil {
    private static JVMInfo jvmInfo;

    /** 获取jvm当前的内存信息 */
    public static JVMInfo getJvmInfo() {
        jvmInfo = JVMInfo.getInstance();
        jvmInfo.setFreeMem(Runtime.getRuntime().freeMemory()/1024/1024);
        jvmInfo.setMaxMem(Runtime.getRuntime().maxMemory()/1024/1024);
        jvmInfo.setTotalMem(Runtime.getRuntime().totalMemory()/1024/1024);
        return jvmInfo;
    }

    public static void main(String[] args) {
        System.out.println(JVMUtil.getJvmInfo());
    }
}
