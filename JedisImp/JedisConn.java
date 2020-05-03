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
