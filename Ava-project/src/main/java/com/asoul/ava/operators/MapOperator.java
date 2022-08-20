package com.asoul.ava.operators;

import com.asoul.ava.functions.MapFunction;
//映射算子
public class  MapOperator extends Operator {
    public MapFunction fun;

    public MapOperator(String name,  MapFunction fun) {
        super(name);
        this.fun = fun;
    }

    @Override
    public MapOperator clone() {
        return new MapOperator(this.name, this.fun);
    }
}
