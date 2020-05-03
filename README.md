# 实现Redis客户端

## 1 主要内容介绍
基于[RESP](https://redis.io/topics/protocol)协议，实现了两个Redis客户端(get和set操作)，分别是线程不安全和线程安全。<br>
在客户端和服务端间进行通信使用的是socket，socket有个问题就是它是单例的，当多个线程并发去发送数据和接受数据时，就会面临线程不安全的问题，所以在线程安全的客户端版本中，我基于有界阻塞队列LinkedBlockingQueue创建了一个连接池，每个线程都有自己的socket对象，这样可以保证线程安全。<br>

## 2 项目结构
JedisConn.java 连接层，负责连接到服务端。<br>
JedisRESPProtocol.java 协议层，基于RESP协议生成服务端认识的字节数组。<br>
JedisClient.java 接口层，对外提供统一的访问接口。<br>
JedisClientPool.java 创建连接池，线程安全的客户端中使用。<br>
JedisRunnable.java 实现Runnable接口，用于多线程并发访问。<br>
JedisTest.java 功能测试。<br>

## 3 各层代码
JedisConn.java 连接层，负责连接到服务端。<br>

```java
package otherexperiment.JedisImp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * 手写Jedis客户端-建立连接
 */

public class JedisConn {
    private String ip;
    private int port;

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    public JedisConn(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public boolean isConnection() {
        if (socket != null && socket.isBound() && socket.isConnected() && inputStream != null &&
                outputStream != null && !socket.isClosed()) {
            return true;
        }
        try {
            socket = new Socket(ip, port);  // 建立连接 三次握手
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String sendCommand(byte[] command) {
        StringBuilder sb = new StringBuilder();
        if (isConnection()) {
            try {
                outputStream.write(command);
                byte[] bytes = new byte[1024];
                int length = 0;
                while ((length = inputStream.read(bytes)) > 0) {
                    System.out.println(Thread.currentThread().getName() + " | " + new String(bytes, 0, length));
                    // 由于无法控制redis服务器关闭输出流 所以我们只进行读取一次 即最长1024个字节
                    sb.append(new String(bytes, 0, length));
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}

```

JedisRESPProtocol.java 协议层，基于RESP协议生成服务端认识的字节数组。<br>
```java
package otherexperiment.JedisImp;

/**
 * 手写Jedis客户端-基于RESP协议解析命令
 */
public class JedisRESPProtocol {
    private static final String DOLLAR = "$";
    private static final String ASTERISK = "*";
    private static final String CRLF = "\r\n";

    public static byte[] buildRESPByte(Command command, byte[]... args) {
        StringBuilder sb = new StringBuilder();
        // 拼接 *数字
        sb.append(ASTERISK).append(args.length + 1).append(CRLF);
        // 拼接 $数字
        sb.append(DOLLAR).append(command.name().length()).append(CRLF);
        // 拼接 GET or SET
        sb.append(command.name()).append(CRLF);

        for (byte[] arg: args) {
            // 拼接 $数字
            sb.append(DOLLAR).append(arg.length).append(CRLF);
            // 拼接 具体值
            sb.append(new String(arg)).append(CRLF);
        }
        return sb.toString().getBytes();
    }

    public enum Command{
        GET,SET
    }

}

```

JedisClient.java 接口层，对外提供统一的访问接口。<br>
```java
package otherexperiment.JedisImp;

/**
 * 手写Jedis客户端-对外提供接口
 */
public class JedisClient {
    JedisConn conn;

    public JedisClient(String ip, int port) {
        conn = new JedisConn(ip, port);
    }

    public String set(String key, String val) {
        byte[] setByteArr = JedisRESPProtocol.buildRESPByte(JedisRESPProtocol.Command.SET, key.getBytes(), val.getBytes());
        String resp = conn.sendCommand(setByteArr);
        return resp;
    }

    public String get(String key) {
        byte[] setByteArr = JedisRESPProtocol.buildRESPByte(JedisRESPProtocol.Command.GET, key.getBytes());
        String resp = conn.sendCommand(setByteArr);
        return resp;
    }

}

```

JedisClientPool.java 创建连接池，线程安全的客户端中使用。<br>
```java
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

```

JedisRunnable.java 实现Runnable接口，用于多线程并发访问。<br>
```java
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

```

JedisTest.java 功能测试。<br>
```java
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

```
