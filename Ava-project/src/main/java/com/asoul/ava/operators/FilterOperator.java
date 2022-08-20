package com.asoul.ava.operators;

import com.asoul.ava.functions.FilterFunction;

//过滤算子
public class FilterOperator extends Operator {
    public FilterFunction fun;

    public FilterOperator(String name, FilterFunction fun) {
        super(name);
        this.fun = fun;
    }

    @Override
    public FilterOperator clone() {
        return new FilterOperator(this.name, this.fun);
    }
}
