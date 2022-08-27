package com.asoul.ava.mailboxes;

import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.dispatch.PriorityGenerator;
import akka.dispatch.UnboundedStablePriorityMailbox;
import com.asoul.ava.messages.BatchMessage;
import com.asoul.ava.messages.Message;
import com.typesafe.config.Config;
public class RecoverMailbox extends UnboundedStablePriorityMailbox {

    public RecoverMailbox(ActorSystem.Settings settings, Config config) {

        //创建一个新的PriorityGenerator，较低的优先级意味着更重要
        super(
                new PriorityGenerator() {
                    @Override
                    public int gen(Object message) {

                       if (message instanceof BatchMessage) {
                            if(((BatchMessage) message).isRecovered()) {
                                return 0;
                            }
                            else return 1;
                        }
                      return 1; // 其他消息
                    }
                });
    }
}
