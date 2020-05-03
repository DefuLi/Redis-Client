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
