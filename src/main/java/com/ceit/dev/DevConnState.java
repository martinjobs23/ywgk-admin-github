package com.ceit.dev;

import com.ceit.jdbc.SimpleJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;


public class DevConnState {

    private static final Logger logger = LoggerFactory.getLogger(DevConnState.class);

    // 获取HTTP POST数据
    private String getPostString(HttpServletRequest request) {

        String postString = null;
        try {
            int contentLength = request.getContentLength();
            if (contentLength < 0) {
                return null;
            }

            // 读取发送的数据
            byte[] buffer = new byte[contentLength];
            for (int i = 0; i < contentLength; ) {
                int readlen = request.getInputStream().read(buffer, i, contentLength - i);
                if (readlen == -1) {
                    break;
                }
                i += readlen;
            }

            // UTF8字符串
            String charEncoding = request.getCharacterEncoding();
            if (charEncoding == null) {
                charEncoding = "UTF-8";
            }

            postString = new String(buffer, charEncoding);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return postString;
    }

    public static String Connect(String server, int servPort) {
        return Connect(server, servPort, 3);
    }

    public static String Connect(String server, int servPort, int timeout) {

        SocketChannel clntChan = null;
        boolean connected = false;
        String errmsg = null;

        try {
            // 创建一个信道，并设为非阻塞模式
            clntChan = SocketChannel.open();
            clntChan.configureBlocking(false);

            // 向服务端发起连接
            connected = true;

            LocalDateTime startTime = LocalDateTime.now();
            long ms = 0;

            if (!clntChan.connect(new InetSocketAddress(server, servPort))) {
                // 不断地轮询连接状态，直到完成连接
                while (!clntChan.finishConnect()) {
                    // 在等待连接的时间里，可以执行其他任务，以充分发挥非阻塞IO的异步特性
                    // 这里为了演示该方法的使用，只是一直打印"."
                    //System.out.print(".");

                    ms = Duration.between(startTime, LocalDateTime.now()).toMillis();
                    if (ms > timeout * 1000) {
                        logger.info("Time out in " + timeout + " seconds");
                        connected = false;
                        errmsg = "Time out";
                        break;
                    }
                }

                System.out.println("clntChan.connect ms: " + ms);

            } else {
                System.out.println("clntChan.connect OK: ");
            }

        } catch (Exception e) {
            errmsg = e.getMessage();
            e.printStackTrace();
            connected = false;
        }

        // 打印出接收到的数据
        System.out.println("Close ");

        // 关闭信道
        try {
            if (clntChan != null)
                clntChan.close();
        } catch (Exception e) {
        }

        System.out.println("connected=" + connected);
        System.out.println("errmsg=" + errmsg);

        if (connected)
            return null;
        else
            return errmsg;
    }

    public static String GetDevicePorts(String deviceId) {
        String sql = "select GROUP_CONCAT(DISTINCT port) from dev_protocol where dev_id=?";

        SimpleJDBC jdbc = SimpleJDBC.getInstance();
        Object portObj = jdbc.selectForOneNode(sql, deviceId);
        if (portObj == null)
            return null;

        return portObj.toString();
    }

    public static List<Object> GetAllDeviceIpPorts(String deviceIds) {
        String idsStr = "";
        Object[] ids = deviceIds.split(",");
        for (Object id : ids) {
            idsStr += ",?";
        }

        String sql = "select CONCAT(ip,'#',GROUP_CONCAT(DISTINCT port)) from dev_protocol"
                + " where dev_id in (" + idsStr.substring(1) + ") group by ip";

        SimpleJDBC jdbc = SimpleJDBC.getInstance();
        List<Object> list = jdbc.selectForList(sql, ids);

        return list;
    }

    public static int GetProtocolPort(String pro) {
        int port = 0;

        if ("SSH".equals(pro))
            return 22;

        if ("RDP".equals(pro))
            return 3389;

        if ("SFTP".equals(pro))
            return 22;

        if ("FTP".equals(pro))
            return 21;

        if ("VNC".equals(pro))
            return 5901;

        if ("X11".equals(pro))
            return 22;

        if ("IE".equals(pro))
            return 80;

        if ("Navicat".equals(pro))
            return 3306;

        if ("Oracle".equals(pro))
            return 1521;

        if ("sap".equals(pro))
            return 21;

        return port;
    }

    //任意一个端口连上就返回OK
    public static String ConnectAny(String server, String deviceId, int timeout) {

        boolean connected = false;
        String errmsg = null;

        List<SocketChannel> clntChanList = new ArrayList<SocketChannel>();

        //查询设备服务端口
        String ports = GetDevicePorts(deviceId);
        if (ports == null || deviceId.length() < 1)
            return "Get Server Port Error";

        String[] portsStr = ports.split(",");
        for (String port : portsStr) {
            if (port.length() == 0)
                continue;

            int servPort = Integer.parseInt(port);
            if (servPort == 0)
                continue;

            try {

                logger.info("Connecting " + server + ":" + servPort);

                // 创建一个信道，并设为非阻塞模式
                SocketChannel clntChan = SocketChannel.open();
                clntChanList.add(clntChan);

                clntChan.configureBlocking(false);

                //发起连接，如果直接连接成功了
                if (clntChan.connect(new InetSocketAddress(server, servPort))) {
                    connected = true;
                    logger.info("Connect " + server + ":" + servPort + "OK");
                    break;
                }

            } catch (Exception e) {
                logger.info("Connect " + server + ":" + servPort + " Exception: " + e.getMessage());
                continue;
            }

        }

        // 不断地轮询连接状态，直到有一个完成连接

        LocalDateTime startTime = LocalDateTime.now();
        long ms = 0;

        while (connected == false && ms < timeout * 1000) {
            for (SocketChannel clntChan : clntChanList) {
                try {

                    if (clntChan.finishConnect()) {
                        connected = true;
                        logger.info("Connect OK in " + ms + " ms");
                        break;
                    }


                } catch (Exception e) {
                    logger.info("Connect " + server + " finishConnect Exception: " + e.getMessage());
                    continue;
                }

            }

            //
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }

            ms = Duration.between(startTime, LocalDateTime.now()).toMillis();
        }

        if (connected == false) {
            errmsg = "Connect " + server + " " + ports + " Timeout in " + timeout + " seconds";
            logger.info(errmsg);
        }

        // 关闭信道
        for (SocketChannel clntChan : clntChanList) {
            try {
                clntChan.close();
            } catch (Exception e) {
            }
        }


        //System.out.println("connected=" + connected);
        //System.out.println("errmsg=" + errmsg);

        if (connected)
            return null;
        else
            return errmsg;
    }

    //连接所有设备，检测所有端口状态
    public static String ConnectAllDevice(String deviceIds, int timeout) {

        String errmsg = "";

        Map<String, List> connctingIpMap = new HashMap<String, List>();
        Map<String, List> connctedIpMap = new HashMap<String, List>();
        Map<String, String> ipPortStateMap = new HashMap<String, String>();

        //查询设备服务端口
        List<Object> list = GetAllDeviceIpPorts(deviceIds);
        if (list == null || list.size() == 0)
            return "GetAllDeviceIpPorts Error";

        for (Object ipPorts : list) {
            String[] ipPortsStr = ipPorts.toString().split("#");
            if (ipPortsStr.length < 2)
                continue;

            String ip = ipPortsStr[0];
            String[] portsStr = ipPortsStr[1].split(",");

            List<SocketChannel> clntChanList = new ArrayList<SocketChannel>();


            boolean connected = false;
            String connectedPorts = "";
            String connectingPorts = "";

            for (String port : portsStr) {
                if (port.length() == 0)
                    continue;

                int servPort = Integer.parseInt(port);
                if (servPort == 0 || servPort > 65535)
                    continue;

                try {

                    logger.debug("Connecting " + ip + ":" + servPort);

                    // 创建一个信道，并设为非阻塞模式
                    SocketChannel clntChan = SocketChannel.open();
                    clntChanList.add(clntChan);

                    clntChan.configureBlocking(false);

                    //发起连接，如果直接连接成功了
                    if (clntChan.connect(new InetSocketAddress(ip, servPort))) {
                        connected = true;
                        connectedPorts += "/" + servPort + "+/";

                        logger.debug("Connect " + ip + ":" + servPort + " OK");
                        //break;
                    } else {
                        connectingPorts += "/" + servPort + "*/";
                        ;
                    }

                } catch (Exception e) {
                    logger.debug("Connect " + ip + ":" + servPort + " Exception: " + e.getMessage());
                    continue;
                }

            }

            //正在连接的ip
            if (connected)
                connctedIpMap.put(ip, clntChanList);
            else
                connctingIpMap.put(ip, clntChanList);

            ipPortStateMap.put(ip, ip + ":" + connectedPorts + connectingPorts);
        }

        // 不断地轮询每个正在连接IP的连接状态，直到有一个完成连接

        LocalDateTime startTime = LocalDateTime.now();
        long ms = 0;

        while (ms < timeout * 1000) {

            Iterator<Map.Entry<String, List>> iter = connctingIpMap.entrySet().iterator();

            if (!iter.hasNext()) {
                //ip列表为空，全部处理完毕
                break;
            }

            while (iter.hasNext()) {
                Map.Entry<String, List> entry = iter.next();
                String ip = entry.getKey();
                List<SocketChannel> clntChanList = entry.getValue();

                boolean connected = false;
                String portStateStr = ipPortStateMap.get(ip);
                for (SocketChannel clntChan : clntChanList) {
                    String ipport = ip;
                    String port = "";
                    try {
                        ipport = clntChan.getRemoteAddress().toString();
                        port = ipport.substring(ipport.lastIndexOf(":") + 1);
                        if (clntChan.finishConnect()) {
                            connected = true;
                            portStateStr = portStateStr.replace("/" + port + "*/", "/" + port + "+/");
                            logger.debug("Connect " + ip + ":" + port + " OK in " + ms + " ms");
                            break;
                        }

                    } catch (Exception e) {

                        //连接失败
                        portStateStr = portStateStr.replace("/" + port + "*/", "/" + port + "-/");

                        logger.debug("Connect " + ip + ":" + port + " finishConnect Exception: " + e.getMessage());
                        continue;
                    }

                }

                //当前IP已经连接上
                if (connected) {
                    connctedIpMap.put(ip, clntChanList);
                    iter.remove();
                }

                ipPortStateMap.put(ip, portStateStr);
            }

            //
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }

            ms = Duration.between(startTime, LocalDateTime.now()).toMillis();
        }

        // 关闭信道
        for (Map.Entry<String, List> entry : connctedIpMap.entrySet()) {
            List<SocketChannel> clntChanList = entry.getValue();
            for (SocketChannel clntChan : clntChanList) {
                try {
                    clntChan.close();
                } catch (Exception e) {
                }
            }
        }

        for (Map.Entry<String, List> entry : connctingIpMap.entrySet()) {
            List<SocketChannel> clntChanList = entry.getValue();
            for (SocketChannel clntChan : clntChanList) {
                try {
                    clntChan.close();
                } catch (Exception e) {
                }
            }
        }

        for (Map.Entry<String, String> entry : ipPortStateMap.entrySet()) {
            String portstate = entry.getValue();
            errmsg += portstate.replace("/", "") + ",";
        }

        //System.out.println("connected=" + connected);
        //System.out.println("errmsg=" + errmsg);

        return "OK:" + errmsg;
    }
}
