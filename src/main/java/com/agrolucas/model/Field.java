package com.agrolucas.model;

public class Field {

    /** Used when no colour is given, so a Field always has one */
    public static final String DEFAULT_COLOR = "#8b949e";

    private String name;
    private int startPosition;
    private int endPosition;
    private FieldDisplay fieldDisplay;
    private String color; // hex string such as "#e0b341", kept as text so the model stays free of JavaFX types
    private CrcConfig crcConfig;

    // TODO add the Decode Formula (new class)


    public Field(String name, int startPosition, int endPosition, FieldDisplay fieldDisplay, String color, CrcConfig crcConfig) {
        this.name = name;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.fieldDisplay = fieldDisplay;
        this.color = color == null ? DEFAULT_COLOR : color;
        this.crcConfig = crcConfig;
    }

    public Field(String name, int startPosition, int endPosition, FieldDisplay fieldDisplay, String color) {
        this(name, startPosition, endPosition, fieldDisplay, color, null);
    }

    public Field(String name, int startPosition, int endPosition, FieldDisplay fieldDisplay) {
        this(name, startPosition, endPosition, fieldDisplay, DEFAULT_COLOR, null);
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color == null ? DEFAULT_COLOR : color;
    }

    public CrcConfig getCrcConfig() {
        return crcConfig;
    }

    public void setCrcConfig(CrcConfig crcConfig) {
        this.crcConfig = crcConfig;
    }
}
