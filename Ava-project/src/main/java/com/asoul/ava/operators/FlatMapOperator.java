package com.asoul.ava.operators;

import com.asoul.ava.functions.FlatMapFunction;

//展平映射算子
public class FlatMapOperator extends Operator {
    public FlatMapFunction fun;

    public FlatMapOperator(String name, FlatMapFunction fun) {
        super(name);
        this.fun = fun;
    }

    @Override
    public FlatMapOperator clone() {
        return new FlatMapOperator(this.name, this.fun);
    }
}
