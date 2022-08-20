package com.asoul.ava.messages;
import java.io.Serializable;


//一条消息，包含一个kv对
public class  Message implements Serializable {
    private final String key;
    private final String val;
    private boolean recovered = false;

    public Message(final String key, final String val) {
        super();
        this.key = key;
        this.val = val;
    }

    public final String getKey() {
        return key;
    }

    public final String getVal() {
        return val;
    }

    public boolean isRecovered() {
        return recovered;
    }

    public void setRecovered(boolean recovered) {
        this.recovered = recovered;
    }

    @Override
    public final String toString() {
        return "(" + key + ", " + val + ")";
    }

}
