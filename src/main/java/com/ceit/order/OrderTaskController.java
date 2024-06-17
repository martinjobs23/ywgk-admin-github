package com.ceit.order;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ceit.admin.common.utils.HttpClientUtils;
import com.ceit.admin.service.UserService;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;
import com.ceit.utils.SqlUtil;
import com.ceit.workstation.service.WorkstationDistributionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller("/order/task")
public class OrderTaskController {

    @Autowired
    private SimpleJDBC simpleJDBC;
    private static final Logger logger= LoggerFactory.getLogger(OrderTaskController.class);
    @Autowired
    private WorkstationDistributionService workstationDistributionService;

    //  查询 我的审批任务
    @RequestMapping("/getOrderList")
    public Result getDeviceBlackList(Map<String, Object> reqBody){
        String searchFiledName = "order_name,deviceObject,deviceListName,deviceList,order_num,start_time,end_time,status";
        String selectFieldNames = "bss_order.*";
        String[] optionNames ={"order_name,deviceObject,deviceListName,deviceList,order_num,start_time,end_time,status,spe_user"};

        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable("bss_order")
                .setAcceptOptions(optionNames)
                .setSearchFields(searchFiledName)
                .setOrderBy( "status desc,end_time desc , start_time desc")
                .setWhere("status = 4")
//                .setWhere("operatorlist like ?","%崔文超%")
                .setFields(selectFieldNames);
        return sqlUtil.selectForTotalRowsResult();
    }

//  查询 我的运维目标设备
    @RequestMapping("/getOrderDevice")
    public Result getOrderDevice(Map<String, Object> reqBody){
        String searchFiledName = "order_name,deviceList,order_num,start_time,end_time,status,operatorList";
        String selectFieldNames = "bss_order.*";
        String[] optionNames ={"order_name,deviceList,order_num,start_time,end_time,status,spe_user,operatorList"};

        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable("(bss_order LEFT JOIN order_devices ON bss_order.order_id=order_devices.ORDER_ID) LEFT JOIN dev_info on order_devices.DEVICE_ID=dev_info.id")
                .setAcceptOptions("bss_order.status,order_name,deviceObject,deviceListName,ip,start_time,end_time,os_type,operatorList,order_devices.tqzh,DEVICE_ID")
                .setSearchFields("bss_order.status,order_name,deviceObject,deviceListName,ip,start_time,end_time,os_type,operatorList,order_devices.tqzh")
                .setOrderBy( "bss_order.status desc,end_time desc , start_time desc")
                .setWhere("bss_order.status in (4,5)")
//                .setWhere("operatorlist like ?","%崔文超%")
                .setFields("bss_order.*,order_name,ip,name,start_time,end_time,os_type,operatorList,order_devices.tqzh,DEVICE_ID");
        Result result =sqlUtil.selectForTotalRowsResult();
        List data = (List)((Map) result.getData()).get("rows");
        List resultData=new ArrayList<>();
        for (Object o :data) {
            Map map=(Map)o;
            if (map.get("DEVICE_ID")!=null){
                int DEVICE_ID=(int)map.get("DEVICE_ID");
                List device_protocol = new SqlUtil(new HashMap<>())
                        .setTable("dev_protocol")
                        .setWhere("dev_id = ? && status=0",DEVICE_ID)
                        .setFields("protocol,port")
                        .selectForMapList();
                map.put("device_protocol",device_protocol);
            }
        }
        return result;
    }

//  是否能够运维
    @RequestMapping("/permission")
    public Result checkPermission(Map<String, Object> reqBody, HttpServletRequest request){

//        获取访问当前系统的浏览器所在电脑IP，用于判断是否是检修专区分配的电脑
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }


        int orderId = (Integer) reqBody.get("order_id");
        System.out.println(workstationDistributionService);
        String workstationIp = workstationDistributionService.getWorkstationIp((Integer) reqBody.get("order_id"), UserService.getCurrentUserId());
        if(workstationIp==null)
            return new Result("非本人可运维，请查正", 200, "vab-hey-message-error");
        //检查时间策略
        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable("bss_order")
                .setWhere("order_id = ?",orderId)
                .setFields("start_time,end_time");

        String start_time=reqBody.get("start_time").toString();
        String end_time=reqBody.get("end_time").toString();
        boolean between_time = isBetweenBeginAndEnd_time((String) start_time, (String) end_time);
        boolean isWorkstationIp=workstationIp.equals(ip);
        if (!reqBody.get("status").equals("5")) {
            return new Result("工作票待许可", 200, "vab-hey-message-error");
        }
         else if (!between_time ) {
            return new Result("不在运维时间段内", 200, "vab-hey-message-error");
        }
         else if (!isWorkstationIp && Integer.parseInt((String) reqBody.get("order_type"))==1) {
            return new Result("检修票需要在分配运维专机运维，此终端非分配运维专机，无法运维", 200, "vab-hey-message-error");
        }
        return new Result(200, "success", "vab-hey-message-success");
    }

//  运维插件
    @RequestMapping("/pluginparam")
    public Result pluginparam(Map<String, Object> reqBody) throws Exception {
//        String orderId = reqBody.get("order_id").toString();
        String orderId="7e2229c3f4364c08b17efd036ab1383c";
        String targetIp = (String) reqBody.get("ip");
        String targetUsername = (String) reqBody.get("user");
        String targetPassword = (String) reqBody.get("password");

        HttpClientUtils httpClientUtils=new HttpClientUtils();

        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("token","12345678");
        hashMap.put("action","yunwei");
        hashMap.put("orderId",orderId);
        hashMap.put("deviceId","35b7d326-4b79-4959-9cbc-621b6b3e3fb5");
        if(reqBody.get("task").equals("ssh")){
            hashMap.put("protocol","ssh");
            hashMap.put("clientName","Xshell");
        }
        else if(reqBody.get("task").equals("rdp")) {
            hashMap.put("protocol","rdp");
            hashMap.put("clientName","mstsc");
        }
        else {
            hashMap.put("protocol","mysql");
            hashMap.put("clientName","Navicat");
        }
        hashMap.put("targetIp",targetIp);
        hashMap.put("targetPort","22");
        hashMap.put("secondLogin","0");
        hashMap.put("targetUsername",targetUsername);
        hashMap.put("targetPassword",targetPassword);
        hashMap.put("operatorId","88fd70b4cdf54560941672a70d1cbb5f");
        hashMap.put("operatorIp","192.168.106.207");
        hashMap.put("ywAccount","niudong");
        hashMap.put("ywUsername","牛栋");
        if(ping("192.168.200.1")){
            String result=httpClientUtils.sendByHttp(hashMap,"https://192.168.200.1/summer/ywgk/yunwei");
            JSONObject obj= JSON.parseObject(result);
            return new Result((String) obj.get("msg"), (Integer) obj.get("code"),obj.get("data"));
        }
        else
            return new Result(200,"堡垒机不可达","error");
    }



    //是否当前时间是否在开始时间结束时间内
    public static boolean isBetweenBeginAndEnd_time(String begin, String end){
        SimpleDateFormat format_demo = new SimpleDateFormat ("yyyy-MM-dd hh:mm:ss");

        try{
            Date startTime = format_demo.parse(begin);
            Calendar cal_begin = Calendar.getInstance();
            cal_begin.setTime(startTime);
            cal_begin.add(Calendar.MINUTE, -30);

            Date endTime = format_demo.parse(end);
            Calendar cal_end = Calendar.getInstance();
            cal_end.setTime(endTime);
            cal_end.add(Calendar.MINUTE, 30);

            Date nowTime = new Date();
            Calendar date = Calendar.getInstance();
            date.setTime(nowTime);

            logger.info("开始时间：" + begin + ",结束时间：" + end);
            if( date.after(cal_begin) && date.before(cal_end) ){
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            logger.error("规范时间格式(yyyy-MM-dd hh:mm:ss)出错-开始时间为：" + begin + ",结束时间为：" + end);
            e.printStackTrace();

            return false;
        }
    }
//    实现ping命令
    public static boolean ping(String ipAddress) throws Exception {
        int  timeOut =  1 ;  //超时应该在3钞以上
        boolean status = InetAddress.getByName(ipAddress).isReachable(timeOut);
        // 当返回值是true时，说明host是可用的，false则不可。
        return status;
    }

//
//    // 新增违规外联条目
//    @RequestMapping("/insert")
//    public Result insert(Map<String, Object> reqBody, HttpServletRequest request) {
//
////        String checkErrorMsg=checkOrgData(request,reqBody);
////        if(checkErrorMsg != null)
////        {
////            return new Result("检查部门数据权限失败,"+checkErrorMsg, 200, -1);
////        }
////        reqBody.put("status",0);
//
//        reqBody.put("time",new Date());
//        SqlUtil sqlUtil = new SqlUtil(reqBody);
//        int ret = sqlUtil.setTable("check_ip")
//                .setFields(setFileds)
//                .insert();
//
//
//        if (ret == 1) {
//            return  new Result("添加成功",200, "success");
//        }
//        return new Result("添加失败",200,"error");
//    }
//
//    // 更新违规外联条目
//    @RequestMapping("/update")
//    public Result update(Map<String, Object> reqBody, HttpServletRequest request) {
//
//        SqlUtil sqlUtil = new SqlUtil(reqBody);
//        int ret = sqlUtil.setTable(tableName)
//                .setFields(setFileds)
//                .setWhere("id=?")
//                .update();
//
//        if (ret != 0) {
//            return new Result("更新成功",200,"success");
//        }
//        return new Result("更新失败",200,"error");
//    }
//
//    // 设置状态为生效中
//    @RequestMapping("/changeStatusWork")
//    public Result changeStatusWork(Map<String, Object> reqBody, HttpServletRequest request) {
//
//        //TODO: 删除时判断数据权限
//
//        String str = reqBody.get("id").toString();
//        String[] ids = str.split(",");
//        int rSet = 0;
//        int count =0;
//        for (int i = 0; i < ids.length; i++) {
//            Integer id = Integer.parseInt(ids[i]);
//            rSet = simpleJDBC.update("update check_http set status="+"1" +" where id=?", id);
//            if (rSet!=0)
//            count++;
//        }
//        if (count >0) {
//            return new Result("设置成功", 200, count);
//        }
//        return new Result("设置失败", 200, "error");
//    }
//
//    // 设置状态为生效中
//    @RequestMapping("/changeStatusRest")
//    public Result changeStatusRest(Map<String, Object> reqBody, HttpServletRequest request) {
//
//        //TODO: 删除时判断数据权限
//
//        String str = reqBody.get("id").toString();
//        String[] ids = str.split(",");
//        int rSet = 0;
//        int count =0;
//        for (int i = 0; i < ids.length; i++) {
//            Integer id = Integer.parseInt(ids[i]);
//            rSet = simpleJDBC.update("update check_http set status="+"0" +" where id=?", id);
//            if (rSet!=0)
//                count++;
//        }
//        if (count >0) {
//            return new Result("设置成功", 200, count);
//        }
//        return new Result("设置失败", 200, "error");
//    }
//
//    // 删除违规外联条目
//    @RequestMapping("/delete")
//    public Result delete(Map<String, Object> reqBody, HttpServletRequest request) {
//
//        String str = reqBody.get("id").toString();
//        String[] ids = str.split(",");
//        int rSet = 0;
//        for ( int i = 0; i < ids.length; i++) {
//            Integer id = Integer.parseInt(ids[i]);
//            rSet = simpleJDBC.update("delete from "+tableName+" where id=?",id);
//        }
//
//        if (rSet != 0) {
//            return new Result("删除成功",200,"success");
//        }
//        return new Result("删除失败",200,"error");
//    }
//
//
//
//    //查找所有未在入网黑名单中的设备
//    @RequestMapping("/getDevice")
//    public Result getDevice(Map<String,Object> reqBody){
//        String sql = "select * from dev_cert where dev_hash not in (select dev_hash from radblacklist)";
//        List res = simpleJDBC.selectForList(sql);
//        if (res.size()==0){
//            return new Result("error",200,null);
//        }
//        return new Result("success",200,res);
//    }
//
//    //批量添加终端至入网黑名单
//    @RequestMapping("/insertDevice")
//    public Result insertDevice(Map<String,Object> reqBody){
//        List<String> deviceList = (List) reqBody.get("device_usernames");
//        String sql1 = "insert into radblacklist (dev_hash,date,reason) value (?,?,?)";
//        Date date = new Date();
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        String time = simpleDateFormat.format(date);
//        int count = 0;
//        for (String dev_hash:deviceList) {
//            int res = simpleJDBC.update(sql1,dev_hash,time,"管理员手动加入");
//            if (res ==1 ){
//                count++;
//            }
//        }
//        return new Result("成功添加"+count+"条终端",200,"success");
//    }
//
//    //获取设备入网结果
//    @RequestMapping("/getUserNetAccessList")
//    public Result getUserNetAccessList(Map<String, Object> reqBody){
//        String searchFiledName = "username,dev_ip";
//        String selectFieldNames = "radpostauth.*";
//        String[] optionNames ={"username","dev_ip"};
//
//        SqlUtil sqlUtil = new SqlUtil(reqBody);
//        String jsonData = sqlUtil.setTable("radpostauth")
//                .setAcceptOptions(optionNames)
//                .setSearchFields(searchFiledName)
//                .setFields(selectFieldNames)
//                .selectForJsonArray();
//        int count = sqlUtil.selectForTotalCount();
//        jsonData = "{\"totalCount\":" + count + ",\"jsonData\":" + jsonData + "}";
//        return new Result("success",200,jsonData);
//    }
//
//
//    //    分发违规外联策略
//    @RequestMapping("/distributeHttp")
//    public Result distributeIP(Map<String, Object> reqBody) {
//        String str = reqBody.get("id").toString();
//        String[] ids = str.split(",");
//        System.out.println(ids.toString());
//        ArrayList rSet = new ArrayList();
//        for (int i = 0; i < ids.length; i++) {
//            Integer id = Integer.parseInt(ids[i]);
////            Object temp = simpleJDBC.selectForList("select * from check_ip where id=?", id);
//            rSet.add(simpleJDBC.selectForList("select * from check_http where id=?", id).get(0));
//            System.out.println(rSet.get(0));
//        }
//
//        Map<String, Object> resultMap=new HashMap<>();
//        resultMap.put("hashcode",rSet);
//        if (rSet!=null) {
//            Result result=checkRequestService.distributeIPGrpc(resultMap);
//            return result;
//        }
//        return new Result("分发失败", 200, "error");
//    }
//
//
//    @RequestMapping("/import")
//    public Result importAsset(Map<String, Object> reqBody, HttpServletRequest request) {
//
//        //自动添加一些字段
//        reqBody.put("add_time",new Date());
//
//        String sql1 = "insert into check_http (http,status,person,reason,time) value (?,?,?,?,?)";
////        int ret = simpleJDBC.update(sql1,reqBody.get(0),reqBody.get(1),reqBody.get(2),reqBody.get(3),reqBody.get(4),reqBody.get(5),new Date());
//        Object http=reqBody.get("网址");
//        Object status=reqBody.get("生效状态");
//        Object person=reqBody.get("添加人员");
//        Object reason=reqBody.get("加入原因");
//        int ret = simpleJDBC.update(sql1,http,status,person,reason,new Date());
//        if (ret >0) {
//            return new Result("添加成功", 200, 1);
//        }
//        return new Result("添加失败", 200, 0);
//    }
}
