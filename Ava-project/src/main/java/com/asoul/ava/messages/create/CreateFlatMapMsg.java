package com.asoul.ava.messages.create;

import akka.actor.Address;
import com.asoul.ava.functions.FlatMapFunction;
//创建FlatMapWorker的通知信息
public class CreateFlatMapMsg extends CreateMsg {
        private FlatMapFunction fun;

        public CreateFlatMapMsg(String name,  int posStage, boolean isLocal,
                                Address address,  final FlatMapFunction fun) {
            super(name,posStage, isLocal, address);
            this.fun = fun;
        }

        public FlatMapFunction getFun() {
            return fun;
        }

        @Override
        public String toString() {
            return "";
        }

}
