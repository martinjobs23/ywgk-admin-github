package com.ceit.admin.common.utils;

import com.ceit.admin.model.Node;

import java.util.ArrayList;

/**
 * 创建策略树结构
 */
public class NodeTreeUtils {

    /**
     * 创建整个树
     * @param methods 策略字符串
     * @return
     */
    public Node createNodeTree(String methods){
        String[] orSplit = methods.split("\\|"); //顶级分割
        Node root = new Node();
        if (orSplit.length > 1){
            root.setValue("|");
            root.setChildren(new ArrayList<>());
            for (String secondStr: orSplit) {
                Node secNode = createSecNode(secondStr);
                root.getChildren().add(secNode);
            }
        } else {
            Node secNode = createSecNode(methods);
            root = secNode;
        }
        return root;
    }


    /**
     * 创建二级节点
     * @param methods
     * @return
     */
    public Node createSecNode(String methods){
        Node secNode = new Node();
        String[] andSplit = methods.split("&");  //二级分割按 "&" 分割
        if (andSplit.length > 1){
            secNode.setValue("&");
            secNode.setChildren(new ArrayList<>());
            for (String thirdStr: andSplit) {
                Node thirdNode = new Node();
                thirdStr = thirdStr.replace("(","").replace(")","").replace(" ","");
                thirdNode.setValue(thirdStr);
                secNode.getChildren().add(thirdNode);
            }
        }else {
            secNode.setValue(methods.replace("(","").replace(")","").replace(" ",""));
        }
        return secNode;
    }
}
