package com.asoul.ava.messages.create;

import akka.actor.Address;

import java.io.Serializable;
//创建Worker的通知信息基类
public abstract class CreateMsg implements Serializable {
    protected String name;
    protected int posStage;
    protected boolean isLocal;
    protected Address address;

    protected int machineNumber;
    protected int shuffleFlag;
    public CreateMsg(String name,int posStage, boolean isLocal, Address address,int machineNumber,int shuffleFlag) {
        this.name = name;
        this.posStage = posStage;
        this.isLocal = isLocal;
        this.address = address;
        this.machineNumber = machineNumber;
        this.shuffleFlag = shuffleFlag;
    }

    public String getName() {
        return name;
    }
    public int getShuffleFlag(){
        return this.shuffleFlag;
    }
    public int getMachineNumber(){
        return this.machineNumber;
    }
    public int getPosStage() { return posStage; }

    public boolean isLocal() {
        return isLocal;
    }

    public Address getAddress() {
        return address;
    }
}
