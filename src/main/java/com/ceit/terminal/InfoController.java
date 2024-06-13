package com.ceit.terminal;

import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;
import com.ceit.utils.SqlUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Controller("/terminal/info")
public class InfoController {
    
    @Autowired
    private SimpleJDBC simpleJDBC;
    
    private static LocalDateTime lastCheckTime = LocalDateTime.now().minus(Duration.ofDays(30));

    private String tableName = "terminal_info";
    private String[] setFileds = {"agent_id","ip","os","install_date","bootup_time","mac","disk_sn","name","model","online","online_time","online_ip"};
    private String[] optionNames = {"number","type", "name", "model", "SN",  "location", "org_name", "user_name", "secret_level"};

    //获取分页信息
    @RequestMapping("/page")
    public Result page(Map<String, Object> reqBody) {

        autoSetOnlineStatus();

        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable(tableName)
                .setAcceptOptions(optionNames);

        return sqlUtil.selectForTotalRowsResult();
    }

    @RequestMapping("/insert")
    public Result insert(Map<String, Object> reqBody) {

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        int ret = sqlUtil.setTable("dev_cert")
                .setFields("dev_hash","username","dev_name","dev_mac","dev_ip","org_id","user_name","location","operation_system","dev_desc","disk_sn","install_time","bootup_time")
                .insert();
        if (ret != 0) {
            return new Result("success", 200, "添加成功");
        }
        return new Result("error", 000, "添加失败");
    }

    @RequestMapping("/update")
    public Result update(Map<String, Object> reqBody) {

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        int ret = sqlUtil.setTable("dev_cert")
                .setFields("dev_hash","dev_name","dev_mac","dev_ip","org_id","user_name","location","operation_system","dev_desc","disk_sn","install_time","bootup_time","online")
                .setWhere("id=?",reqBody.get("id"))
                .update();
        if(reqBody.get("dev_mac") != null){
            reqBody.put("mac",reqBody.get("dev_mac"));
            reqBody.put("terminal_id",reqBody.get("id"));
            sqlUtil.setTable("asset_info")
                    .setFields("user_name","location","org_id")
                    .setWhere("mac like ?",reqBody.get("mac"))
                    .update();
        }
        if (ret != 0) {
            return new Result("success", 200, "更新成功");
        }
        return new Result("error", 000, "更新失败");
    }

    @RequestMapping("/delete")
    public Result delete(Map<String, Object> reqBody) {
        String str = reqBody.get("id").toString();
        String[] ids = str.split(",");
        int rSet = 0;
        for (int i = 0; i < ids.length; i++) {
            Integer id = Integer.parseInt(ids[i]);
            rSet = simpleJDBC.update("delete from dev_cert where id=?", id);
        }
        if (rSet != 0) {
            return new Result("success", 200, "删除成功");
        }
        return new Result("error", 000, "删除失败");
    }

    //自动设置超时的在线状态，每隔1分钟
    private void autoSetOnlineStatus()
    {
        LocalDateTime now = LocalDateTime.now();
        long minutes = Duration.between(lastCheckTime, now).toMinutes();
        //测试：每分钟都检查
        if(minutes > 1)
        {
            lastCheckTime =now;
            //把超时时间大于10分钟的自动设置为不在线
            simpleJDBC.update("update "+tableName+" set online=0 WHERE online=1 and timestampdiff(MINUTE,online_time ,NOW())>10");

            //顺便把会议终端的在线状态也自动设置了
            simpleJDBC.update("update meeting_pad set online=0 WHERE online=1 and timestampdiff(MINUTE,online_time ,NOW())>10");
        }
    }

    //报告终端状态，id可以多个
    @RequestMapping("/report")
    public Result report(Map<String, Object> reqBody) {

        autoSetOnlineStatus();

        Object objId= reqBody.get("id");
        if( objId==null || objId.toString().equals("0"))
        {         
            if(reqBody.get("agent_id")==null)
                return new Result("没有Id或Agentid", 500, -1);

            //查询ID
            objId= simpleJDBC.selectForOneNode("select id from "+ tableName +" where agent_id=?",  reqBody.get("agent_id"));
            if( objId!=null)
            {
                reqBody.put("id",objId);
            }
            else
            {
                //查询失败, 插入新纪录
            }
        }

        //使用服务器时间
        reqBody.put("online_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        //如果一次更新多个状态，不更新 online_ip
        String[] updateFileds = {"online","online_time","online_ip"};
        if(reqBody.get("online_ip")==null)
            updateFileds = new String[] {"online","online_time"};

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        if( objId==null || (objId.toString().equals("0")) ){
            //不存在，自动添加
            objId = sqlUtil.setTable(tableName)
                    .setFields(setFileds)
                    .insertAutoIncKey();
        }
        else
        {
            //更新状态,可以多个Id
            int ret = sqlUtil.setTable(tableName)
                    .setFields(updateFileds)
                    .setSearchFields("id")
                    .update();

            //更新状态
            /*
            simpleJDBC.update("update "+tableName+" set online=?,online_time=NOW() where id=?", 
                        reqBody.get("online"), reqBody.get("id"));
             */
            
            //如果是会议分发服务器，同时更新会议终端在线状态
            if(reqBody.get("meeting_server")!=null)
            {
                sqlUtil.setTable("meeting_pad")
                    .setFields(updateFileds)
                    .setSearchFields("id")
                    .update();
            }

             if(ret<0)
                objId =-1;
             else if(objId.toString().contains(","))
                objId =ret;   //多个Id,返回更新数量
             else 
                objId = Integer.parseInt(objId.toString());  //单个  objId 5 或者 "5" 
        }     

        if ( (int)objId >= 0) {
            return new Result("report成功", 200, objId);
        }
        return new Result("report失败", 500, objId);
    }
}
