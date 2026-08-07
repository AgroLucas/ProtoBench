package com.agrolucas.model;

public class Capture {

    private String name;
    private HexPacket hexPacket;


    public Capture(String name, HexPacket hexPacket) {
        this.name = name;
        this.hexPacket = hexPacket;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HexPacket getHexPacket() {
        return hexPacket;
    }

    public void setHexPacket(HexPacket hexPacket) {
        this.hexPacket = hexPacket;
    }


}
