package util.timer;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MyTimer {
    private final Lock lock = new ReentrantLock();
    private Timer timer;

    private volatile boolean    canceled;           /** 是否已经取消 */

    public MyTimer() {
        this.timer = new Timer();
        canceled = false;
    }

    /** 重启超时计时器（ms） */
    public void reset(TimerTask timerTask,long timeoutMs) {
        try {
            lock.lock();

            if(!canceled) {
                // 还没停止了
                timer.cancel();
            }
            timer = new Timer();

            timer.schedule(timerTask,timeoutMs);
            canceled = false;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void stop() {
        // 停止当前的timer任务不让他进行下去
        try {
            lock.lock();
            if(!canceled) {
                timer.cancel();
            }
            canceled = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}
