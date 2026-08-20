package com.agrolucas.model;

public class HexPacket {
    private final byte[] bytes;

    public HexPacket(String hex) {
        hex = hex.replaceAll("\\s+", ""); // allow spaces like "C4 3D 54"

        this.bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
    }

    public int length() {
        return bytes.length;
    }

    public int bitLength() {
        return bytes.length * 8;
    }

    /**
     * The value of a single bit, counting from the start of the packet.
     * Bit 0 is the most significant bit of the first byte, which is the order the capture grid displays.
     * @param bitIndex, the bit to read
     * @return 0 or 1, and 0 for any position outside the packet
     */
    public int bitAt(int bitIndex) {
        if (bitIndex < 0 || bitIndex >= bitLength())
            return 0;

        int bitInsideByte = 7 - (bitIndex % 8);
        return (bytes[bitIndex / 8] >> bitInsideByte) & 1;
    }

    /**
     * Read a range of bits as a single number, first bit being the most significant one
     * @param startBit, first bit of the range (inclusive)
     * @param endBit, last bit of the range (inclusive)
     * @return the value held by those bits
     * @throws IllegalArgumentException if the range covers more than 64 bits, which no long could hold
     */
    public long extractBits(int startBit, int endBit) {
        int bitCount = endBit - startBit + 1;
        if (bitCount > 64)
            throw new IllegalArgumentException("Cannot extract " + bitCount + " bits into a long");

        long value = 0;
        for (int bit = startBit; bit <= endBit; bit++)
            value = (value << 1) | bitAt(bit);

        return value;
    }

    /**
     * Read a range of bits as bytes, which is what a CRC is computed over.
     * A range that is not a whole number of bytes is padded with zero bits on the right.
     * @param startBit, first bit of the range (inclusive)
     * @param endBit, last bit of the range (inclusive)
     */
    public byte[] extractBytes(int startBit, int endBit) {
        int bitCount = Math.max(0, endBit - startBit + 1);
        byte[] extracted = new byte[(bitCount + 7) / 8];

        for (int i = 0; i < bitCount; i++) {
            if (bitAt(startBit + i) == 1)
                extracted[i / 8] |= (byte) (1 << (7 - (i % 8)));
        }
        return extracted;
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