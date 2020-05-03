package otherexperiment.JedisImp;

/**
 * 手写Jedis客户端-多线程实现Runnable接口
 */
public class JedisRunnable implements Runnable {
    private JedisClientPool jedisClientPool;
    private String val;

    public JedisRunnable(JedisClientPool jedisClientPool, String val) {
        this.jedisClientPool = jedisClientPool;
        this.val = val;
    }

    @Override
    public void run() {
        JedisClient jedisClient = jedisClientPool.getClient();
        jedisClient.set("username", val);
        jedisClientPool.putClient(jedisClient);
    }
}
