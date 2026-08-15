package com.agrolucas.model;

import java.util.List;

public class MessageType {

    private String name;
    private List<Field> fields;


    public MessageType(String name, List<Field> fields) {
        this.name = name;
        this.fields = fields;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    @Override
    public String toString() {
        return name;
    }
}
