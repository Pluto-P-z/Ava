package com.asoul.ava.messages.create;

import akka.actor.Address;
import com.asoul.ava.functions.FilterFunction;
//创建FilterWorker的通知信息
public class CreateFilterMsg extends CreateMsg {
    private FilterFunction fun;

    public CreateFilterMsg(String name,int posStage, boolean isLocal,
                           Address address,  final FilterFunction fun,int machineNumber,int shuffleFlag) {
        super(name,posStage, isLocal, address,machineNumber,shuffleFlag);
        this.fun = fun;
    }

    public FilterFunction getFun() {
        return fun;
    }

    @Override
    public String toString() {
        return "";
    }

}
