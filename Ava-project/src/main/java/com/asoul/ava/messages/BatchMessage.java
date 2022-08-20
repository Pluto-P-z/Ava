package com.asoul.ava.messages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//一批消息，包含由一个List的若干条消息，对应一个窗口的数据
public class BatchMessage implements Serializable {
    private static final long serialVersionUID = -5844532433791505868L;
    private List<Message> messages = new ArrayList<>();
    private boolean recovered = false;
    String batchInfo;

    public String getBatchInfo() {
        return batchInfo;
    }

    public void setBatchInfo(String batchInfo) {
        this.batchInfo = batchInfo;
    }

    public BatchMessage(List<Message> messages, String batchInfo) {
        super();
        this.messages.addAll(messages);
        this.batchInfo = batchInfo;
    }

    public final List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public boolean isRecovered() {
        return recovered;
    }

    public void setRecovered(boolean recovered) {
        this.recovered = recovered;
    }

    @Override
    public final String toString() {
        String repr = "[";
        Iterator<Message> itr = messages.iterator();
        while (itr.hasNext()) {
            Message msg = itr.next();
            repr = repr.concat("(" + msg.getKey() + ", " + msg.getVal() + ")");

            if (itr.hasNext()) {
                repr = repr.concat(",");
            }
        }
        repr = repr.concat("]");

        return repr;
    }
}
