package com.ceit.device;

import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.json.JSON;
import com.ceit.response.Result;
import com.ceit.response.ResultCode;
import com.ceit.utils.SqlUtil;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

@Controller("/device")
public class DeviceController {

    @Autowired
    private SimpleJDBC simpleJDBC;

    private final String tableName = "ywsj_device";
    private final String[] optionNames ={"ip","host_ip","manager"};       //可以选择的查询条件
    private final String searchFiledName = "dgroup_id";
//  private final String selectFieldNames = "ywsj_device.id,ywsj_device.code,ywsj_device.name,ywsj_device.ip,ywsj_device.os,ywsj_device.create_time,ywsj_device.type,ywsj_device.manager,ywsj_device.location,ywsj_device.description,GROUP_CONCAT(ywsj_device_service.type) as service_type" ;
    private final String selectFieldNames = "id,code,name,ip,os,type,host_ip,dgroup_id,manager,location,description" ;
//  private final String orderBy = "ywsj_device.create_time";
    private final String groupBy = null;
    private final String where = null;



//    private final String[] optionNames ={"code", "name", "ip", "os", "create_time", "type","manager","location","description"};
//    private final String searchFiledName = "group_id";
//    private final String tableName = "ywsj_device";


    @Autowired
    private JSON json;

    @RequestMapping("/page")
    public Result page(Map<String, Object> reqBody){

        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable(tableName)
                .setSearchFields(searchFiledName)
                .setAcceptOptions(optionNames)
                .setFields(selectFieldNames)
                .setGroupBy(groupBy)
                .setWhere(where);

        return sqlUtil.selectForTotalRowsResult();
    }

    @RequestMapping("/update")
    public Result update(Map<String, Object> reqBody) {
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        int rSet = sqlUtil.setTable("ywsj_device")
                .setFields("code","name","ip","os","type","host_ip","dgroup_id","manager","location","description")
                .setWhere("id = ?",reqBody.get("id"))
                .update();
        return new Result("ok",200,rSet);
    }

    @RequestMapping("/insert")
    public Result insert(Map<String, Object> reqBody) {
        System.out.println("reqBody:"+reqBody);
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        int rSet = sqlUtil.setTable("ywsj_device")
                .setFields("id","code","name","ip","os","type","host_ip","dgroup_id","manager","location","description")
                .insert();
        return new Result("success", 200, rSet);
    }

    @RequestMapping("/delete")
    public Result delete(Map<String, Object> reqBody) {
        System.out.println("........................6666666666............................"+reqBody);
        String str = reqBody.get("ids").toString();
        String[] deviceIds = str.split(",");
        int rSet = 0;
        for (int i = 0; i < deviceIds.length; i++) {
            Integer id = Integer.parseInt(deviceIds[i]);
            rSet = simpleJDBC.update("delete from ywsj_device where id=?", id);
        }
        return new Result("success", 200, rSet);
    }

    @RequestMapping("/upload")
    public Result upload(Map<String, Object> reqBody) {
        Date date = new Date();
        Timestamp currentTime = new Timestamp(date.getTime());
        reqBody.put("create_time",currentTime);
        reqBody.put("dgroup_id",21);
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        int rSet = sqlUtil.setTable("ywsj_device")
                .setFields("code","name","ip","os","type","photo","dgroup_id","create_time","camera")
                .setSearchFields("id")
                .insert();
        return new Result("ok", 200, rSet);
    }

    @RequestMapping("/getProtocols")
    public Result getProtocols(Map<String, Object> reqBody) {
        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable("ywsj_device_protocols")
                .setFields("protocol_id","protocol_port")
                .setSearchFields("device_id");
        return sqlUtil.selectForTotalRowsResult();
    }

    //获取协议列表
    @RequestMapping("/protocolList")
    public Result protocols(Map<String, Object> reqBody) {
        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable("ywsj_protocols")
                .setSearchFields("protocol_id","protocol_name","protocol_port");

        return sqlUtil.selectForTotalRowsResult();
    }

    //获取设备类型
    @RequestMapping("/getDeviceType")
    public Result getDeviceType(Map<String, Object> reqBody) {

        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable("ywsj_device_type")
                .setSearchFields("id","pid","code","name");

        return sqlUtil.selectForTotalRowsResult();
    }

    //编辑时获取指定设备的设备服务
    @RequestMapping("/getDeviceService")
    public Result getDeviceService(Map<String, Object> reqBody) {
        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable("ywsj_device_service")
                .setFields("type","name","port","via_proxy","via_winserver","options")
                .setSearchFields("device_id");
        return sqlUtil.selectForTotalRowsResult();
    }

    //编辑时获取指定设备的设备账号
    @RequestMapping("/getDeviceAccount")
    public Result getDeviceAccount(Map<String, Object> reqBody) {
        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable("ywsj_device_account")
                .setFields("name","password")
                .setSearchFields("device_id");

        return sqlUtil.selectForTotalRowsResult();
    }


}
