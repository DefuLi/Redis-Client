package otherexperiment.JedisImp;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 手写Jedis客户端-功能测试
 */
public class JedisTest {
    public static void main(String[] args) {
        JedisTest test = new JedisTest();
        test.multiThread();
    }

    /**
     * 单线程 线程不安全
     */
    public void singleThread() {
        JedisClient client = new JedisClient("127.0.0.1", 6379);
        String resp = client.get("username");
        System.out.println(resp);
    }

    /**
     * 多线程 线程安全
     */
    public void multiThread() {
        // 连接池的大小是5
        JedisClientPool jedisClientPool = new JedisClientPool("127.0.0.1", 6379, 5);
        ExecutorService threadPool = Executors.newCachedThreadPool();
        // 创建20个线程
        for (int i = 0; i < 20; i++) {
            threadPool.execute(new JedisRunnable(jedisClientPool, String.valueOf(i)));
        }
    }
}
