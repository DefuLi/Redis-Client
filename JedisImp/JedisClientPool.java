package otherexperiment.JedisImp;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 手写Jedis客户端-线程安全 使用连接池
 */
public class JedisClientPool {
    private LinkedBlockingQueue<JedisClient> linkedBlockingQueue;
    public JedisClientPool(String ip, int port, int count) {
        linkedBlockingQueue = new LinkedBlockingQueue<>(count);
        for (int i = 0; i < count; i++) {
            try {
                linkedBlockingQueue.put(new JedisClient(ip, port));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public JedisClient getClient() {
        try {
            return linkedBlockingQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void putClient(JedisClient jedisClient) {
        try {
            linkedBlockingQueue.put(jedisClient);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
