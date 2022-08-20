package com.asoul.ava.mailboxes;

import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.dispatch.PriorityGenerator;
import akka.dispatch.UnboundedStablePriorityMailbox;
import com.asoul.ava.messages.BatchMessage;
import com.asoul.ava.messages.Message;
import com.typesafe.config.Config;
//改，Message直接弃用了
public class RecoverMailbox extends UnboundedStablePriorityMailbox {

    public RecoverMailbox(ActorSystem.Settings settings, Config config) {

        // Create a new PriorityGenerator, lower priority means more important
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
                      return 1; // Other messages
                    }
                });
    }
}
