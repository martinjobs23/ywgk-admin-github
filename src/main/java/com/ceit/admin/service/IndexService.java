package com.ceit.admin.service;

import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Component;
import com.ceit.ioc.annotations.PostConstruct;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.utils.SqlUtil;
import com.sun.management.OperatingSystemMXBean;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
public class IndexService {

    @Autowired
    private static SimpleJDBC simpleJDBC;

    private static String osName = System.getProperty("os.name");
    private static final int CPUTIME = 500;
    private static final int FAULTLENGTH = 10;

    /**
     * 获取网关信息
     * @return
     */
    public String getGateWayInfo(Map<String, Object> reqBody){
        //1.获取部署机器IP
        String IP = null;
        try {
            IP = getFirstNonLoopbackAddress(true, false).getHostAddress();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        //2.获取部署机器内存
        long memory = getMemory() / (1024 * 1024 * 1024); //由KB转为GB
        String strMemory = memory + "GB";
        //3.看部署端是ipsec的服务器还是客户
        String type;
        if (IP.equals(System.getProperty("ipsecServerIP"))){
            type = "IPSec服务端";
        }else if (IP.equals(System.getProperty("ipsecClientIP"))){
            type = "IPSec客户端";
        }else {
            type = "其它";
        }
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        //4.隧道数量
        //int count = sqlUtil.setTable("ipsec.tunnel").selectForTotalCount();
        int count = 0;
        String jsonData = "{\"IP\":\"" + IP + "\",\"osName\":\"" + osName +  "\",\"memory\":\"" + strMemory +"\",\"type\":\""+ type + "\",\"tunnelCount\":"
                + count+"}";
        return jsonData;
    }

    /**
     * 获取windows和linux下本机ip通用方法
     * @param preferIpv4
     * @param preferIPv6
     * @return
     * @throws SocketException
     */
    public InetAddress getFirstNonLoopbackAddress(boolean preferIpv4, boolean preferIPv6) throws SocketException {
        Enumeration en = NetworkInterface.getNetworkInterfaces();
        while (en.hasMoreElements()) {
            NetworkInterface i = (NetworkInterface) en.nextElement();
            for (Enumeration en2 = i.getInetAddresses(); en2.hasMoreElements();) {
                InetAddress addr = (InetAddress) en2.nextElement();
                if (!addr.isLoopbackAddress()) {
                    if (addr instanceof Inet4Address) {
                        if (preferIPv6) {
                            continue;
                        }
                        return addr;
                    }
                    if (addr instanceof Inet6Address) {
                        if (preferIpv4) {
                            continue;
                        }
                        return addr;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取操作系统
     * @return
     */
    public String getOsName() {
        return osName;
    }

    /**
     * 获取Linux内存信息
     * @return
     */
    public Map<String, Object> getLinuxMemory(){
        Map<String, Object> map = new HashMap<String, Object>();
        InputStreamReader inputs = null;
        BufferedReader buffer = null;
        try {
            inputs = new InputStreamReader(new FileInputStream("/proc/meminfo"));
            buffer = new BufferedReader(inputs);
            String line = "";
            while (true) {
                line = buffer.readLine();
                if (line == null)
                    break;
                int beginIndex = 0;
                int endIndex = line.indexOf(":");
                if (endIndex != -1) {
                    String key = line.substring(beginIndex, endIndex);
                    beginIndex = endIndex + 1;
                    endIndex = line.length();
                    String memory = line.substring(beginIndex, endIndex);
                    String value = memory.replace("kB", "").trim();
                    map.put(key, value);
                }
            }
        }catch (Exception e) {
            System.out.println(e.toString());
        } finally {
            try {
                buffer.close();
                inputs.close();
            } catch (Exception e2) {
                System.out.println(e2.toString());
            }
        }
        return map;
    }

    /**
     * 获取本机内存
     * @return
     */
    public long getMemory(){
        long totalvirtualMemory;
        if (osName.toLowerCase().contains("windows")
                || osName.toLowerCase().contains("win")) {
            OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory
                    .getOperatingSystemMXBean();
            // 总的物理内存+虚拟内存
            totalvirtualMemory = osmxb.getTotalSwapSpaceSize();
        } else {
            Map<String, Object> map = getLinuxMemory();
            totalvirtualMemory = Long.parseLong(map.get("MemTotal").toString());
        }
        return totalvirtualMemory;
    }

    /**
     * 获取Windows内存使用率
     */
    public double getWindowsMemUsage() {
        try {
            OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory
                    .getOperatingSystemMXBean();
            // 总的物理内存+虚拟内存
            long totalVirtualMemory = osmxb.getTotalSwapSpaceSize();
            // 剩余的物理内存
            long freePhysicalMemorySize = osmxb.getFreePhysicalMemorySize();
            double usage = (1 - freePhysicalMemorySize * 1.0 / totalVirtualMemory) * 100;
            String str = String.format("%.1f", usage);
            double memoryUsage = Double.parseDouble(str);
            return memoryUsage;
        } catch (Exception e) {
            System.out.println(e.toString());
            return 0;
        }
    }

    /**
     * 获取Linux内存使用率
     */
    public double getLinuxMemUsage() {
        Map<String, Object> map = getLinuxMemory();
        long memTotal = Long.parseLong(map.get("MemTotal").toString());
        long memFree = Long.parseLong(map.get("MemFree").toString());
        long memUsed = memTotal - memFree;
        long buffers = Long.parseLong(map.get("Buffers").toString());
        long cached = Long.parseLong(map.get("Cached").toString());
        double usage = (memUsed - buffers - cached) * 1.0 / memTotal * 100;
        String str = String.format("%.1f", usage);
        double memoryUsage = Double.parseDouble(str);
        return memoryUsage;
    }

    /**
     *获取Windows的cpu利用率
     * @return
     */
    public double getWindowsCpuUsage(){
        /*try {
            String procCmd = System.getenv("windir") + "\\system32\\wbem\\wmic.exe process get Caption,CommandLine,KernelModeTime,ReadOperationCount,ThreadCount,UserModeTime,WriteOperationCount";
            // 取进程信息
            long[] c0 = readCpu(Runtime.getRuntime().exec(procCmd));
            Thread.sleep(CPUTIME);
            long[] c1 = readCpu(Runtime.getRuntime().exec(procCmd));
            if (c0 != null && c1 != null) {
                long idletime = c1[0] - c0[0];
                long busytime = c1[1] - c0[1];
                double usage = (busytime) * 1.0 / (busytime + idletime) *100;
                String str = String.format("%.1f", usage);
                double cpuUsage = Double.parseDouble(str);
                return cpuUsage;
                //return  Double.valueOf(PERCENT * (busytime) * 1.0 / (busytime + idletime)).intValue();
            } else {
                return 0;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0;
        }*/
        OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double systemCpuLoad = operatingSystemMXBean.getSystemCpuLoad();
        return  systemCpuLoad;
    }

    /**
     * 获取linux中cpu使用率
     *
     * @return int
     * @author yanzy
     * @version 1.0
     * @date 2019/1/16 15:31
     */
    public double getLinuxCpuUsage() {
        try {
            Map<?, ?> map1 = getLinuxCpuInfo();
            Thread.sleep(5 * 1000);
            Map<?, ?> map2 = getLinuxCpuInfo();

            long user1 = Long.parseLong(map1.get("user").toString());
            long nice1 = Long.parseLong(map1.get("nice").toString());
            long system1 = Long.parseLong(map1.get("system").toString());
            long idle1 = Long.parseLong(map1.get("idle").toString());

            long user2 = Long.parseLong(map2.get("user").toString());
            long nice2 = Long.parseLong(map2.get("nice").toString());
            long system2 = Long.parseLong(map2.get("system").toString());
            long idle2 = Long.parseLong(map2.get("idle").toString());

            long total1 = user1 + system1 + nice1;
            long total2 = user2 + system2 + nice2;
            float total = total2 - total1;

            long totalIdle1 = user1 + nice1 + system1 + idle1;
            long totalIdle2 = user2 + nice2 + system2 + idle2;
            float totalidle = totalIdle2 - totalIdle1;

            double usage = (total * 1.0 / totalidle) * 100;
            String str = String.format("%.1f", usage);
            double cpuUsage = Double.parseDouble(str);
            return cpuUsage;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取Linux中CPU使用信息
     *
     * @return java.util.Map
     * @author yanzy
     * @version 1.0
     * @date 2019/1/16 15:31
     */
    public Map<?, ?> getLinuxCpuInfo() {
        InputStreamReader inputs = null;
        BufferedReader buffer = null;
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            inputs = new InputStreamReader(new FileInputStream("/proc/stat"));
            buffer = new BufferedReader(inputs);
            String line = "";
            while (true) {
                line = buffer.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("cpu")) {
                    StringTokenizer tokenizer = new StringTokenizer(line);
                    List<String> temp = new ArrayList<String>();
                    while (tokenizer.hasMoreElements()) {
                        String value = tokenizer.nextToken();
                        temp.add(value);
                    }
                    map.put("user", temp.get(1));
                    map.put("nice", temp.get(2));
                    map.put("system", temp.get(3));
                    map.put("idle", temp.get(4));
                    map.put("iowait", temp.get(5));
                    map.put("irq", temp.get(6));
                    map.put("softirq", temp.get(7));
                    map.put("stealstolen", temp.get(8));
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                buffer.close();
                inputs.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return map;
    }


    /**
     * 定时任务
     *获取cpu利用率,插入数据库
     */
    public void insertCpuUsage() {
        double cpuUsage;
        if (osName.toLowerCase().contains("windows")
                || osName.toLowerCase().contains("win")) {
            cpuUsage = getWindowsCpuUsage();
        } else {
            cpuUsage = getLinuxCpuUsage();
        }
        String sql = "insert into login.sys_cpu (usage_rate, check_time) values (? ,?)";
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH时");
        String check_time = sdf.format(new Date());
        simpleJDBC.update(sql, cpuUsage, check_time);
    }

    /**
     * 定时任务
     *获取内存利用率,插入数据库
     */
    public void insertMemoryUsage() {
        double memoryUsage;
        if (osName.toLowerCase().contains("windows")
                || osName.toLowerCase().contains("win")) {
            memoryUsage = getWindowsMemUsage();
        } else {
            memoryUsage = getLinuxMemUsage();
        }
        String sql = "insert into login.sys_memory (usage_rate, check_time) values (? ,?)";
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH时");
        String check_time = sdf.format(new Date());
        simpleJDBC.update(sql, memoryUsage, check_time);
    }

    @PostConstruct
    public void insertUsage(){
        // 创建任务队列
        ScheduledExecutorService scheduledExecutorService =
                Executors.newScheduledThreadPool(3); // 10 为线程数量
        /*
        // 执行任务-- 获取cpu利用率
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            insertCpuUsage();
        }, 0, 3, TimeUnit.HOURS); // 0s 后开始执行，每 3h 执行一次
        // 执行任务-- 获取内存利用率
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            insertMemoryUsage();
        }, 0, 3, TimeUnit.HOURS); // 0s 后开始执行，每 3h 执行一次
         */
    }


}
