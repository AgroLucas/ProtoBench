package com.agrolucas.model;

/**
 * Bit by bit CRC, the generic form that covers the usual CRC-8 / CRC-16 / CRC-32 variants
 * once the polynomial, starting value and reflection settings are given.
 */
public final class CrcCalculator {

    private CrcCalculator() {
    }

    /**
     * Compute a CRC over some bytes
     * @param data, the payload
     * @param width, the width of the CRC in bits, must be at least 8 (a CRC narrower than a byte is not supported)
     * @param poly, the generator polynomial without its top bit
     * @param init, the value the register starts from
     * @param reflectIn, whether each input byte is bit-reversed before being fed in
     * @param reflectOut, whether the final value is bit-reversed
     * @return the CRC, already masked to the requested width
     */
    public static long compute(byte[] data, int width, long poly, long init, boolean reflectIn, boolean reflectOut) {
        if (width < 8)
            throw new IllegalArgumentException("CRC width must be at least 8 bits, got " + width);

        long widthMask = XorRule.widthMask(width);
        long topBit = 1L << (width - 1);
        long crc = init & widthMask;

        for (byte rawByte : data) {
            int currentByte = rawByte & 0xFF;
            if (reflectIn)
                currentByte = reverseByte(currentByte);

            crc ^= ((long) currentByte) << (width - 8);

            for (int bit = 0; bit < 8; bit++) {
                boolean topBitSet = (crc & topBit) != 0;
                crc <<= 1;
                if (topBitSet)
                    crc ^= poly;
                crc &= widthMask;
            }
        }

        if (reflectOut)
            crc = reverse(crc, width);

        return crc & widthMask;
    }

    private static int reverseByte(int value) {
        return (int) reverse(value & 0xFF, 8);
    }

    private static long reverse(long value, int width) {
        long reversed = 0;
        for (int i = 0; i < width; i++)
            reversed = (reversed << 1) | ((value >>> i) & 1);

        return reversed;
    }
}
