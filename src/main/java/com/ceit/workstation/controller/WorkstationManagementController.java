package com.ceit.workstation.controller;

import com.ceit.utils.SqlUtil;
import com.ceit.workstation.service.WorkstationManegementService;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;

import java.util.Map;

@Controller("/workstationManagement")
public class WorkstationManagementController {

    @Autowired
    private WorkstationManegementService workstationManegementService;

    @Autowired
    private SimpleJDBC simpleJDBC;

    //查询已注册设备
    @RequestMapping(value = "/getCheckList")
    public Result getCheckList(Map<String, Object> reqBody){
        return workstationManegementService.getCheckList(reqBody);
    }

    @RequestMapping(value = "/getOrderList")
    public Result getOrderList(Map<String, Object> reqBody){
        return workstationManegementService.getOrderList(reqBody);
    }
    //获取终端状态
    @RequestMapping("/workstationStatus")
    public Result workstationStatus(Map<String, Object> reqBody){
        return workstationManegementService.workstationStatus(reqBody);
    }

    @RequestMapping("/insert")
    public Result insert(Map<String, Object> reqBody) {

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        int ret = sqlUtil.setTable("workstation_info")
                .setFields("name","ip","mac","location","logined_username","decription")
                .insert();
        if (ret != 0) {
            return new Result("success", 200, "添加成功");
        }
        return new Result("error", 000, "添加失败");
    }

    @RequestMapping("/update")
    public Result update(Map<String, Object> reqBody) {
        /**
         * 如果禁用，需要重新分配在用和待用的工作票
         */

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        int ret = sqlUtil.setTable("workstation_info")
                .setFields("name","ip","mac","location","online","logined","logined_username","disabled","decription")
                .setWhere("id=?",reqBody.get("id"))
                .update();
//        if(reqBody.get("dev_mac") != null){
//            reqBody.put("mac",reqBody.get("dev_mac"));
//            reqBody.put("terminal_id",reqBody.get("id"));
//            sqlUtil.setTable("asset_info")
//                    .setFields("user_name","location","org_id")
//                    .setWhere("mac like ?",reqBody.get("mac"))
//                    .update();
//        }
        if (ret != 0) {
            return new Result("success", 200, "更新成功");
        }
        return new Result("error", 000, "更新失败");
    }

    @RequestMapping("/delete")
    /**
     * 如果删除，需要重新分配在用和待用的工作票
     */
    public Result delete(Map<String, Object> reqBody) {
        String str = reqBody.get("id").toString();
        String[] ids = str.split(",");
        int rSet = 0;
        for (int i = 0; i < ids.length; i++) {
            Integer id = Integer.parseInt(ids[i]);
            rSet = simpleJDBC.update("delete from workstation_info where id=?", id);
        }
        if (rSet != 0) {
            return new Result("success", 200, "删除成功");
        }
        return new Result("error", 000, "删除失败");
    }

//
//    //获取硬件认证信息
//    @RequestMapping("/harawareCheckInfo")
//    public Result harawareCheckInfo(Map<String, Object> reqBody){
//       return terminalManegementService.harawareCheckInfo(reqBody);
//    }
//
//    //获取软件认证信息
//    @RequestMapping("/softwareCheckInfo")
//    public Result softwareCheckInfo(Map<String, Object> reqBody){
//        return terminalManegementService.softwareCheckInfo(reqBody);
//    }
}
