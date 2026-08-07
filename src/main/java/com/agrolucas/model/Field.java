package com.agrolucas.model;

public class Field {

    private String name;
    private int startPosition;
    private int endPosition;
    private FieldDisplay fieldDisplay;
    private CrcConfig crcConfig;

    // TODO add the color
    // TODO add the Decode Formula (new class)


    public Field(String name, int startPosition, int endPosition, FieldDisplay fieldDisplay, CrcConfig crcConfig) {
        this.name = name;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.fieldDisplay = fieldDisplay;
        this.crcConfig = crcConfig;
    }

    public Field(String name, int startPosition, int endPosition, FieldDisplay fieldDisplay) {
        this.name = name;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.fieldDisplay = fieldDisplay;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

    public int getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }

    public FieldDisplay getFieldDisplay() {
        return fieldDisplay;
    }

    public void setFieldDisplay(FieldDisplay fieldDisplay) {
        this.fieldDisplay = fieldDisplay;
    }

    public CrcConfig getCrcConfig() {
        return crcConfig;
    }

    public void setCrcConfig(CrcConfig crcConfig) {
        this.crcConfig = crcConfig;
    }
}
