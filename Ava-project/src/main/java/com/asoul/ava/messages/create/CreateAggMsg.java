package com.asoul.ava.messages.create;

import akka.actor.Address;
import com.asoul.ava.functions.AggregateFunction;
//创建AggWorker的通知信息
public class CreateAggMsg extends CreateMsg {
    private AggregateFunction fun;


    public CreateAggMsg(String name, int posStage, boolean isLocal,
                        Address address,  final AggregateFunction fun
                        ) {
        super(name,  posStage, isLocal, address);
        this.fun = fun;
    }

    public AggregateFunction getFun() { return fun; }

    @Override
    public String toString() {
        return "";
    }

}
