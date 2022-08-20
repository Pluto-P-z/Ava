package com.asoul.ava.functions;
//过滤函数
public interface FilterFunction extends AbstractFunction {
    public Boolean predicate(String key, String value);
}
