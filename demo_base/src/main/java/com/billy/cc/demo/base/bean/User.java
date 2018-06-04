package com.billy.cc.demo.base.bean;

/**
 * 演示传递自定义类型
 * @author billy.qi
 * @since 18/5/28 19:43
 */
public class User {
    private String userName;
    private int id;

    public User(int id, String userName) {
        this.id = id;
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
