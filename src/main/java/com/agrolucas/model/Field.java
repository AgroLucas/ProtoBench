package com.agrolucas.model;

import java.util.ArrayList;
import java.util.List;

public class Field {

    /** Used when no colour is given, so a Field always has one */
    public static final String DEFAULT_COLOR = "#8b949e";

    private String name;
    private int startPosition;
    private int endPosition;
    private FieldDisplay fieldDisplay;
    private String color; // hex string such as "#e0b341", kept as text so the model stays free of JavaFX types

    // applied in order to turn the raw bits into a value, see FieldDecoder.
    // A CRC is one of these rules, which is why there is no separate CrcConfig on the Field itself.
    private final List<DecodeRule> decodeRules = new ArrayList<>();


    public Field(String name, int startPosition, int endPosition, FieldDisplay fieldDisplay, String color) {
        this.name = name;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.fieldDisplay = fieldDisplay;
        this.color = color == null ? DEFAULT_COLOR : color;
    }

    public Field(String name, int startPosition, int endPosition, FieldDisplay fieldDisplay) {
        this(name, startPosition, endPosition, fieldDisplay, DEFAULT_COLOR);
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

    /**
     * The decode rules of this Field, in the order they are applied. Mutable, add and remove directly.
     */
    public List<DecodeRule> getDecodeRules() {
        return decodeRules;
    }

    /**
     * Whether this Field holds a CRC, meaning it can be checked against the payload it covers
     */
    public boolean hasCrc() {
        return decodeRules.stream().anyMatch(CrcRule.class::isInstance);
    }
}
