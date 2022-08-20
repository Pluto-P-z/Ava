package com.asoul.ava.operators;

import com.asoul.ava.functions.AggregateFunction;

//聚合算子
public class AggregateOperator extends Operator {
    public AggregateFunction fun;


    public AggregateOperator(String name, AggregateFunction fun) {
        super(name);
        this.fun = fun;
    }

    @Override
    public AggregateOperator clone() {
        return new AggregateOperator(this.name, this.fun);
    }
}