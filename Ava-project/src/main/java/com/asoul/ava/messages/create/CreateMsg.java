package com.asoul.ava.messages.create;

import akka.actor.Address;

import java.io.Serializable;
//创建Worker的通知信息基类
public abstract class CreateMsg implements Serializable {
    protected String name;
    protected String color;
    protected int posStage;
    protected boolean isLocal;
    protected Address address;


    public CreateMsg(String name,int posStage, boolean isLocal, Address address) {
        this.name = name;

        this.posStage = posStage;
        this.isLocal = isLocal;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getColor() { return color; }

    public int getPosStage() { return posStage; }

    public boolean isLocal() {
        return isLocal;
    }

    public Address getAddress() {
        return address;
    }
}
