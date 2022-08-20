package com.asoul.ava.functions;

import com.asoul.ava.messages.Message;

import java.util.List;
//聚合函数
public interface AggregateFunction extends AbstractFunction {
    public Message process(String key, List<String> values);

}
