package com.asoul.ava.functions;

import com.asoul.ava.messages.Message;
//映射函数
public interface MapFunction extends AbstractFunction {
    public Message process(String key, String value);
}
