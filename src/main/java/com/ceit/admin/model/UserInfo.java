package com.ceit.admin.model;

import com.ceit.admin.common.utils.NodeTreeUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserInfo implements Serializable {
    
    public int id;
    public String account;
    public String name;
    public String clientIp;

    //用户的角色信息，可能多个，字符串隔开
    private String roleIds;
    private String roleNames;

    public String getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(String roleIds) {
        this.roleIds = roleIds;
    }

    public String getRoleNames() {
        return roleNames;
    }

    public void setRoleNames(String roleNames) {
        this.roleNames = roleNames;
    }

    //用户部门信息
    private int orgId;
    private String orgName;

    public int getOrgId() {
        return orgId;
    }

    public void setOrgId(int orgId) {
        this.orgId = orgId;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }
 
    public List<String> allAuthPolicy;
    public List<String> okAuthPolicy;
    public List<String> unAuthPolicy;
    public Node node;
 
    //认证过程相关

    //需要的认证方法
    //public Map<String, String> auth_method;
    //已经成功的
    //还需要认证的
    //认证结果

    public Node getNode() {
        return node;
    }

    public void setNode(String methods){
        NodeTreeUtils nodeTreeUtils = new NodeTreeUtils();
        node = nodeTreeUtils.createNodeTree(methods);
    }


    public UserInfo() {
        allAuthPolicy = new ArrayList<>();
        okAuthPolicy = new ArrayList<>();
        unAuthPolicy = new ArrayList<>();
    }

    public void setAllAuthMethod(String methods){
        methods = methods.replace("(", "").replace(")", "").replace(" ","");
        String[] split = methods.split("[|,&]");//按| 和 &分割
        for (String method: split) {
            if (!allAuthPolicy.contains(method)){
                allAuthPolicy.add(method);
                unAuthPolicy.add(method);
            }
        }

    }

    public void setOkAuthPolicy(String method){
        okAuthPolicy.add(method);
        unAuthPolicy.remove(method);
    }

    public boolean checkAuth(){
        boolean res = false;
        if (node.getValue().equals("|")){
            for (Node secNode: node.getChildren()) {
                res = checkSecLevel(secNode);
                if (res == true){
                    break;
                }
            }
        } else {
            res = checkSecLevel(node);
        }
        return res;
    }


    public boolean checkSecLevel(Node node){
        boolean res = true;
        if (node.getValue().equals("&")){
            for (Node thirdNode: node.getChildren()) {
                if (!okAuthPolicy.contains(thirdNode.getValue())){
                    res = false;
                    break;
                }
            }
        }else {
            if (!okAuthPolicy.contains(node.getValue())){
                res = false;
            }
        }
        return res;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "id=" + id +
                ", account='" + account + '\'' +
                ", allAuthPolicy=" + allAuthPolicy +
                ", okAuthPolicy=" + okAuthPolicy +
                ", unAuthPolicy=" + unAuthPolicy +
                '}';
    }
}
