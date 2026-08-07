package com.agrolucas.model;

public class CrcConfig {

    private int poly;
    private int init;
    private int payloadStartPosition;
    private int payloadEndPosition;
    private Integer refIn;
    private Integer refOut;


    public CrcConfig(int poly, int init, int payloadStartPosition, int payloadEndPosition, int refIn, int refOut) {
        this.poly = poly;
        this.init = init;
        this.payloadStartPosition = payloadStartPosition;
        this.payloadEndPosition = payloadEndPosition;
        this.refIn = refIn;
        this.refOut = refOut;
    }

    public CrcConfig(int poly, int init, int payloadStartPosition, int payloadEndPosition) {
        this.poly = poly;
        this.init = init;
        this.payloadStartPosition = payloadStartPosition;
        this.payloadEndPosition = payloadEndPosition;
    }


    public int getPoly() {
        return poly;
    }

    public void setPoly(int poly) {
        this.poly = poly;
    }

    public int getInit() {
        return init;
    }

    public void setInit(int init) {
        this.init = init;
    }

    public int getPayloadStartPosition() {
        return payloadStartPosition;
    }

    public void setPayloadStartPosition(int payloadStartPosition) {
        this.payloadStartPosition = payloadStartPosition;
    }

    public int getPayloadEndPosition() {
        return payloadEndPosition;
    }

    public void setPayloadEndPosition(int payloadEndPosition) {
        this.payloadEndPosition = payloadEndPosition;
    }

    public int getRefIn() {
        return refIn;
    }

    public void setRefIn(int refIn) {
        this.refIn = refIn;
    }

    public int getRefOut() {
        return refOut;
    }

    public void setRefOut(int refOut) {
        this.refOut = refOut;
    }
}
