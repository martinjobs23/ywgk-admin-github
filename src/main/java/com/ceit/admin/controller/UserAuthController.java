package com.ceit.admin.controller;

import com.ceit.admin.service.UserService;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;
import com.ceit.response.ResultCode;
import com.ceit.utils.SqlUtil;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;

@Controller("/userAuth")
public class UserAuthController {
    @Autowired
    private SimpleJDBC simpleJDBC;

    private final String tableName = "sys_role_user";
    private final String[] selectFieldNames = { "user_id" };
    private final String[] searchFiledNames = { "role_id" };


    @RequestMapping("/list")
    public Result list(Map<String, Object> reqBody) {

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        SqlUtil.changeSearchFieldName(reqBody, "roleId", "role_id");
        sqlUtil.setTable(tableName)
                .setSearchFields(searchFiledNames)
                .setFields(selectFieldNames);
        return new Result(200,"ok",sqlUtil.selectForMapList());
    }


    @RequestMapping("/insert")
    public Result insert(Map<String, Object> reqBody) {

        int roleId = Integer.parseInt(reqBody.get("roleId").toString()) ;
        String str = reqBody.get("userIds").toString();

        String getStr = str.substring(str.indexOf("[") + 1, str.indexOf("]"));
        String[] userIds = getStr.split(",");
 
        List<String> userIdList =new ArrayList<String>();
        String failUserId="";
        int failUserCount =0;
        int okUserCount =0;
        
        //检查不相容角色
        String[] conflictedRoleIds = getConflictedRoleIds(roleId);
        for(String idStr: userIds)
        {
        	int userId = Integer.parseInt(idStr);
        	if(checkConflictedRoles(conflictedRoleIds,userId))
        	{
        		failUserId += idStr+",";
        		failUserCount++;
        	}
        	else
        	{
        		userIdList.add(idStr);
        		okUserCount++;
        	}
        }
  
        if(okUserCount==0)
        {
        	return new Result("角色冲突无法设置,用户"+failUserCount+'个', 200, 0);
        }
        
        String selectSql = "select * from sys_role_user where role_id = ? ";

        simpleJDBC.update("delete from sys_role_user where role_id=?", roleId);

        int rSet = 0;
        //new
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        reqBody.put("role_id",roleId);
        reqBody.put("user_id", userIdList);
        rSet = sqlUtil.setTable("sys_role_user")
                .setFields("role_id","user_id")
                .insert();
        
        Result result = new Result("ok", 200, okUserCount);
        if(failUserCount>0)
        {
        	result.setMsg("设置用户" + okUserCount+", 角色冲突无法设置的用户"+ failUserCount+"个");
        }

        String audit = "insert into sercuityAudit (Cusername,username,ip,content,type,time,level) value (?,?,?,?,?,?,?)";
        String Cusername = UserService.getCurrentUserName();
        String username = UserService.getCurrentUserAccount();
        String user_name = reqBody.get("user_name").toString();
        String role_name = reqBody.get("role_name").toString();
        String ip = UserService.getCurrentUserIp();
        String content = "授权"+user_name+"为"+role_name;
        String type = "用户授权";
        LocalDateTime currentDateTime = LocalDateTime.now();
        String time = currentDateTime.toString();
        System.out.println(username);
        System.out.println(ip);
        System.out.println(content);
        System.out.println(type);
        System.out.println(user_name);
        System.out.println(role_name);
        simpleJDBC.update(audit,Cusername,username,ip,content,type,time,"中");


        return result;
    }

    //获取跟当前角色冲突的角色Id
    private String[] getConflictedRoleIds(int roleId) {
    	
		String sql ="select config_value from sys_config where config_item =?";
    	String conflictedRoleIds = simpleJDBC.selectForOneString(sql, "role.conflicted");
    	if(conflictedRoleIds==null || conflictedRoleIds.trim().isEmpty())
    		return null;
    	
    	String[] ids = conflictedRoleIds.trim().split(",");
    	for(String id: ids)
    	{
    		if(id.equals(String.valueOf(roleId)))
    			return ids;
    	}
    	
    	return null;
    }
    
    //检查不相容角色,冲突角色
    private boolean checkConflictedRoles(String[] conflictedRoleIds,int userId) {
    	
    	if(conflictedRoleIds==null || conflictedRoleIds.length==0)
    		return false;
    	
    	//查找用户已经分配的角色Ids
		String sql ="select role_id from sys_role_user where user_id=?";
    	List<Object> idList = simpleJDBC.selectForList(sql, userId);
    	if(idList==null || idList.size() ==0)
    		return false;
    	
    	for(Object id: idList)
    	{
    		for(String newId: conflictedRoleIds)
    		{
    			if(newId.equals(id.toString()))
    				return true;
    		}
    	}
    	
    	return false;
    }
}
