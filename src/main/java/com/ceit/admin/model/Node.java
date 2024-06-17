package com.ceit.admin.model;

import java.io.Serializable;
import java.util.List;

/**
 * 策略节点
 */
public class Node  implements Serializable  {

    private String value; //节点名称
    private List<Node> children;


    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
    }

    public Node() {
    }

    public Node(String value, List<Node> children) {
        this.value = value;
        this.children = children;
    }

    @Override
    public String toString() {
        return "Node{" +
                "value='" + value + '\'' +
                ", children=" + children +
                '}';
    }
}
