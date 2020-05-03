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
