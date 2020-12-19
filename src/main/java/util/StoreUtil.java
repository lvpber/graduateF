package util;

import redis.RedisPool;
import redis.clients.jedis.Jedis;

// 先用redis实现，将来用file实现或者其他实现方式
public class StoreUtil {
    /**
     * 日志项存储:(log)
     * key : IpAddrPort.log.logEntry.logIndex
     * value : logEntry
     * key : IpAddrPort.log.lastIndex
     * value : lastLogIndex
     *
     * 状态机存储 : (log)
     * key : state + logindex
     * value : logentry
     * key : state + lastIndex
     * value : statemachine的lastLogEntryIndex
     */
//    private static final Jedis jedis = RedisPool.getJedis();

    // 写入
    public static void write(String prefix,String key,String value) {
        String realKey = prefix + key;
        Jedis jedis = null;
        try {
            jedis = RedisPool.getJedis();
            jedis.set(realKey,value);
        }
        catch (Exception e) {
            System.out.println("写入redis失败 : " + e.getMessage());
        }
        finally {
            if(jedis != null)
                jedis.close();
        }
    }

    // 读出
    public static String read(String prefix,String key) {
        String realKey = prefix + key;
        Jedis jedis = null;
        try {
            jedis = RedisPool.getJedis();
            return jedis.get(realKey);
        }
        catch (Exception e) {
            System.out.println("读redis失败，失败的key [" + realKey + "] ，失败原因 : " + e.getMessage());
        }
        finally {
            if(jedis != null)
                jedis.close();
        }
        return null;
    }

    // 删除
    public static Long delete(String prefix,String key) {
        String realKey = prefix + key;
        Jedis jedis = null;
        try {
            jedis = RedisPool.getJedis();
            return jedis.del(realKey);
        }
        catch (Exception e) {
            System.out.println("删除redis失败，失败的key [" + realKey + "] ，失败原因 : " + e.getMessage());
        }
        finally {
            if(jedis != null)
                jedis.close();
        }
        return null;
    }
}
