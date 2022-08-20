package com.asoul.ava.functions;

import com.asoul.ava.messages.Message;

import java.util.List;
//展平映射函数
public interface  FlatMapFunction extends AbstractFunction {
    public List<Message> process(String key, String value);
}
