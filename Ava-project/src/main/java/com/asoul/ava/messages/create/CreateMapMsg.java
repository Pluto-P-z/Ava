package com.asoul.ava.messages.create;

import akka.actor.Address;
import com.asoul.ava.functions.MapFunction;
//创建MapWorker的通知信息

public class CreateMapMsg extends CreateMsg {
    private static final long serialVersionUID = 3169449768254646491L;
    private MapFunction fun;

    public CreateMapMsg(String name,  int posStage, boolean isLocal,
                        Address address, final MapFunction fun) {
        super(name,  posStage, isLocal, address);
        this.fun = fun;
    }

    public MapFunction getFun() {
        return fun;
    }

    @Override
    public String toString() {
        return ""; //TODO
    }

}

