package com.ceit.workstation.service;

import com.alibaba.fastjson2.JSONArray;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Component;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;
import com.ceit.utils.SqlUtil;
import com.ceit.workstation.grpc.ZjWebAdminClient;
import com.ceit.workstation.util.MessageUtil;
import com.ceit.workstation.util.OrderUtil;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class WorkstationDistributionService {

    @Autowired
    SimpleJDBC simpleJDBC;

    //获取工作票工位对应列表
    public Result getCheckList(Map<String, Object> reqBody){
        //条件查询
        checkWorkstationOrderStatus();
        System.out.println(reqBody);
        String[] optionNames ={"workstation_name,order_num,order_name,operator_name"};
//        String selectFieldNames = "dev_cert.*,sys_organization.name,sys_user.name as user_name,last_online_time";
        String selectFieldNames = "workstation_order.*, bss_order.order_num,bss_order.order_name,name";
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        Result jsonData = sqlUtil
                //.setTable("dev_cert left join sys_organization on dev_cert.org_id = sys_organization.id left join sys_user on dev_cert.user_id = sys_user.id")
                .setTable("workstation_order left join bss_order on workstation_order.order_id = bss_order.order_id left join workstation_info on workstation_order.workstation_id = workstation_info.id")
                .setAcceptOptions(optionNames)
                .setFields(selectFieldNames)
                .setOrderBy("status desc, start_time desc, end_time desc")
                .selectForTotalRowsResult();
//        int count = sqlUtil.selectForTotalCount();
//        jsonData = "{\"totalCount\":" + count + ",\"jsonData\":" + jsonData + "}";
        return jsonData;
    }

    public Result checkWorkstationStatus(Map<String, Object> reqBody){
        //条件查询
        System.out.println(reqBody);
        String[] optionNames ={"name,ip,mac,location"};
//        String selectFieldNames = "dev_cert.*,sys_organization.name,sys_user.name as user_name,last_online_time";
        String selectFieldNames = "workstation_info.*";
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        Result jsonData = sqlUtil
                //.setTable("dev_cert left join sys_organization on dev_cert.org_id = sys_organization.id left join sys_user on dev_cert.user_id = sys_user.id")
                .setTable("workstation_order")
                .setAcceptOptions(optionNames)
                .setFields(selectFieldNames)
                .setOrderBy("online desc, online_time desc")
                .selectForTotalRowsResult();
        int count = sqlUtil.selectForTotalCount();
//        jsonData = "{\"totalCount\":" + count + ",\"jsonData\":" + jsonData + "}";
        return jsonData;
    }

    //工位分配
    public Result orderDistribute(MessageUtil messageUtil){
        System.out.println(messageUtil.toString());
        ArrayList<OrderUtil> orderUtilList = messageUtil.getOperatorList();
        int num = orderUtilList.size();
        if (num == 0)
            return new Result(100,"orderUtilList is null,please recheck",messageUtil);
        String location = messageUtil.getLocation();
        Timestamp startTime = Timestamp.valueOf(messageUtil.getOperator_start_time());
        Timestamp endTime = Timestamp.valueOf(messageUtil.getOperator_end_time());
        //提前分配半小时
        long millisecondsPerMinute = 60000;
        long thirtyMinutesInMillis = millisecondsPerMinute * 30;
        Timestamp halfHourBefore = new Timestamp(startTime.getTime() - thirtyMinutesInMillis);
        startTime = halfHourBefore;
        //数据库中查找在对应时间范围内空余的座位
        String sql = "select * from workstation_info WHERE location = ? and" +
                " id not IN (SELECT workstation_id FROM workstation_order WHERE disabled = 0 and ((start_time <= ? AND end_time >= ?) OR " +
                "(start_time >= ? AND end_time <= ?) OR " +
                "(start_time <= ? AND end_time >= ?))) order by id";
        List<Map<String, Object>> freelist = simpleJDBC.selectForMapList(sql, location, startTime, startTime, startTime, endTime, endTime, endTime);
        if (freelist.size() < num) {
            return new Result(200, "分配失败，该区域在"+startTime+"-"+endTime+"时间内空余座位数不足，请您更换选取！",messageUtil);
        }
        //从头分配
        int randomNumber = 0;
        //开始分配工位
        if(freelist.size()-num!=0) randomNumber = generateRandomNumberInRange(freelist.size()-num);  //随机生成一个数字
        int sum= 0;
        for (int i = 0; i < num; i++) {
            sql = "select name from sys_user where id = ?";
            String name = simpleJDBC.selectForOneString(sql,orderUtilList.get(i).getOperator_id());
            //将分配结果写到消息队列
            orderUtilList.get(i).setOrder_id(messageUtil.getOrder_id());
            orderUtilList.get(i).setWorkstation_id((Integer) freelist.get(randomNumber+i).get("id"));
            orderUtilList.get(i).setWorkstation_name((String) freelist.get(randomNumber+i).get("name"));
            orderUtilList.get(i).setOperator_start_time(startTime.toLocalDateTime());
            orderUtilList.get(i).setOperator_end_time(endTime.toLocalDateTime());
            orderUtilList.get(i).setLocation(location);
            String pwd = UUID.randomUUID().toString().substring(0,6);   //生成password
            //按照对应关系写数据库
            sql = "insert into workstation_order ( workstation_id, workstation_name, order_id ,operator_num, operator_id, operator_name, location, start_time, end_time, status, time, workstation_password)" +
                    " value (?,?,?,?,?,?,?,?,?,?,?,?)";
            int res = simpleJDBC.update(sql,freelist.get(randomNumber+i).get("id"),freelist.get(randomNumber+i).get("name"),messageUtil.getOrder_id(),
                    num,orderUtilList.get(i).getOperator_id(), name,location, startTime,endTime,"1",LocalDateTime.now(),pwd);
            //GRPC通信
            ZjWebAdminClient zjWebAdminClient = new ZjWebAdminClient();
            //zjWebAdminClient.setConfig();
            Result r = zjWebAdminClient.createAccount((Integer) freelist.get(randomNumber+i).get("id"),orderUtilList.get(i).getOperator_name(),pwd);
            if(r.getData() == freelist.get(randomNumber+i).get("id") && res != 0) {
//            if(res!=0){
                maintenanceRecord("assignAfterApproval",  freelist.get(randomNumber+i).get("id").toString(),messageUtil.getOrder_id(), orderUtilList.get(i).getOperator_id());
                /**空余位置！未来用于接入短信平台
                 *
                 */
                sum = sum + res;
            }
        }
        messageUtil.setOperatorList(orderUtilList);
        if (sum != num){
            return new Result(200,"写入数据库失败或与专机服务端通信失败",messageUtil);
        }
        return new Result(200,"success",messageUtil);
    }

    public Result workstationPwdEdit(Map<String, Object> reqBody) {
        System.out.println(reqBody);
        String id = reqBody.get("id").toString();
        String pwd = reqBody.get("workstation_password").toString();
        String sql = "update workstation_order set workstation_password=?,time=? where id=?";
        int res = simpleJDBC.update(sql, pwd, LocalDateTime.now(), id);
        if(res!=0){
            sql = "select * from workstation_order where id = ?";
            Map<String,Object> map = simpleJDBC.selectForMap(sql,id);
            maintenanceRecord("changePassword",map.get("workstation_id").toString(),map.get("order_id").toString(),map.get("operator_id").toString());
            return  new Result(200,"success","密码修改成功");
        }
        return new Result(100,"error","密码修改失败，请稍后重试");
    }
    public Result workstationRedistribute(Map<String, Object> reqBody) {
        System.out.println(reqBody);
        String id = reqBody.get("id").toString();
        String new_id = reqBody.get("freeworkstationlist1").toString();
//        String sql = "select name from workstation_info where id = ?";
//        String workstation_name = simpleJDBC.selectForOneString(sql,new_id);
        String sql = "update workstation_order set workstation_id=?,time=? where id=?";

        int res = simpleJDBC.update(sql, new_id, LocalDateTime.now(), id);
        if(res!=0){
            sql = "select * from workstation_order where id = ?";
            Map<String,Object> map = simpleJDBC.selectForMap(sql,id);
            maintenanceRecord("changeWorkstation",map.get("workstation_id").toString(),map.get("order_id").toString(),map.get("operator_id").toString());
            return  new Result(200,"success","专机/工位修改成功");
        }
        return new Result(100,"error","专机/工位修改失败");
    }
    public Result getFreeWorkstationList(Map<String, Object> reqBody) {
        System.out.println(reqBody);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        System.out.println(reqBody.get("start_time").getClass().getName());
        String startTime = (String) reqBody.get("start_time");
        String endTime = (String) reqBody.get("end_time");
        String createTime = simpleDateFormat.format(new Date());
//        Timestamp startTime = (Timestamp) reqBody.get("start_time");
//        Timestamp endTime = (Timestamp) reqBody.get("end_time");
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        String sql = "select * from workstation_info WHERE location = ? and" +
                " id not IN (SELECT workstation_id FROM workstation_order WHERE disabled = 0 and ((start_time <= ? AND end_time >= ?) OR " +
                "(start_time >= ? AND end_time <= ?) OR " +
                "(start_time <= ? AND end_time >= ?))) order by name";
        JSONArray freelist = simpleJDBC.selectForJsonArray(sql, reqBody.get("location"), startTime, startTime, startTime, endTime, endTime, endTime);
        int count = freelist.size();

        return new Result("ok", 200, freelist);
    }
    public Result delete(Map<String, Object> reqBody) {
        String str = reqBody.get("id").toString();
        String[] ids = str.split(",");
        int rSet = 0;
        for (int i = 0; i < ids.length; i++) {
            Integer id = Integer.parseInt(ids[i]);
            String sql = "select * from workstation_order where id = ?";
            Map<String,Object> map = simpleJDBC.selectForMap(sql,id);
            maintenanceRecord("deleteWorkstationOrderRecord",map.get("workstation_id").toString(),map.get("order_id").toString(),map.get("operator_id").toString());
            sql = "delete from workstation_order where id=?";
            rSet = simpleJDBC.update(sql, id);

        }
        if (rSet != 0) {
            return new Result("success", 200, "删除成功");
        }
        return new Result("error", 100, "删除失败");
    }

    private static int generateRandomNumberInRange(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("生成随机数错误，输入值n must be a positive integer");
        }

        Random random = new Random();
        return random.nextInt(n); // Generates a random int from 0 (inclusive) to n-1 (exclusive)
    }
    private Result maintenanceRecord(String maintenance,String workstation_id,String order_id,String operator_id){
        //System.out.println(workstation_id+order_id+operator_id);
        String sql = "insert into workstation_order_log (workstation_id,order_id,operator_id,operation,time) values (?,?,?,?,?)";
        int res = simpleJDBC.update(sql,workstation_id,order_id,operator_id,maintenance,LocalDateTime.now());
        if(res == 0)
            return new Result("error", 300, "维护日志记录失败。");
        else
            return new Result("success", 200, "维护日志记录成功。");
    }

    private boolean checkWorkstationOrderStatus(){
        String sql = "UPDATE workstation_order SET status = 0 WHERE end_time < ?";
        simpleJDBC.update(sql,LocalDateTime.now());
        sql = "UPDATE workstation_order SET status = 2 WHERE start_time < ? and end_time > ?";
        simpleJDBC.update(sql,LocalDateTime.now(),LocalDateTime.now());
        return true;
    }
}
