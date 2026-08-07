package com.agrolucas.model;

public class HexPacket {
    private final byte[] bytes;

    public HexPacket(String hex) {
        hex = hex.replaceAll("\\s+", ""); // allow spaces like "C4 3D 54"
        if (hex.length() % 2 != 0)
            throw new IllegalArgumentException("Hex string must have even length");

        this.bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
    }

    public int length() {
        return bytes.length;
    }

    public byte[] getBytes() {
        return bytes.clone();
    }

    public String toHexString() {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    public String toBinaryString(boolean addSpace) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
            if (addSpace)
                sb.append(' '); // space between bytes for readability
        }
        return sb.toString().trim();
    }

    public String toAsciiString() {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int v = b & 0xFF;
            sb.append((v >= 32 && v <= 126) ? (char) v : '.'); // printable range only
        }
        return sb.toString();
    }

}