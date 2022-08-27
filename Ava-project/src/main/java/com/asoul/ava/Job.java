package com.asoul.ava;

import com.asoul.ava.functions.AggregateFunction;
import com.asoul.ava.functions.FilterFunction;
import com.asoul.ava.functions.MapFunction;
import com.asoul.ava.messages.Message;
import com.asoul.ava.operators.*;


import java.io.Serializable;
import java.util.*;

public class Job implements Serializable {
    private static final long serialVersionUID = -4035435954110471507L;

    private List<Operator> operators;
    private String name;

    public Job(List<Operator> operators, String name) {
        this.operators = operators;
        this.name = name;
    }

    public List<Operator> getOperators() {
        return operators;
    }

    public String getName() {
        return name;
    }
    //一个聚合函数，wordcountJob
    public static final Job jobOne = new Job(Arrays.asList(
            //先全部变成小写
            new MapOperator("Map",(MapFunction & Serializable)(String k,String v)->{
                String newKey = k.toLowerCase();
                return new Message(newKey,v);
                }),
            //保留h开头的key的kv对
            new FilterOperator("Filter",(FilterFunction & Serializable) (String k,String v)-> {
                if(k.startsWith("h")){
                    return true;
                }
                else {
                    return false;
                }
            }),
            //聚合
            new AggregateOperator(  "Aggregate",(AggregateFunction & Serializable) (String k, List<String> vs)->{
                int sum = 0;
                for(int i=0;i<vs.size();i++){
                    sum+=Integer.parseInt(vs.get(i));
                }
                return new Message(k,sum+"");
            }
            )
            ), "jobOne");
    public static final Job jobTwo = new Job(Arrays.asList(
            //先全部变成小写
            new MapOperator("Map",(MapFunction & Serializable)(String k,String v)->{
                String newKey = k.toLowerCase();
                return new Message(newKey,v);
            }),
            //聚合
            new AggregateOperator(  "Aggregate",(AggregateFunction & Serializable) (String k, List<String> vs)->{
                int sum = 0;
                for(int i=0;i<vs.size();i++){
                    sum+=Integer.parseInt(vs.get(i));
                }
                return new Message(k,sum+"");
            }
            )
    ), "jobTwo");

}


