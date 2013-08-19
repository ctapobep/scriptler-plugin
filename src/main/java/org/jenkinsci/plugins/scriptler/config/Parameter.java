package org.jenkinsci.plugins.scriptler.config;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Parameter implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String value;

    public Parameter() {
        super();
    }

    public Parameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }


    public static Map<String, String> toMap(Parameter[] params) {
        if(params == null){
            return new HashMap<String, String>();
        }
        Map<String, String> output = new HashMap<String, String>(params.length);
        for(Parameter parameter: params){
            output.put(parameter.name, parameter.value);
        }
        return output;
    }
}