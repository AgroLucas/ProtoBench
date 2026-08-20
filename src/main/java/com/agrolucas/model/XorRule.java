package com.agrolucas.model;

import java.util.Locale;

/**
 * Exclusive-or the field value with a fixed mask, the usual way a simple protocol obfuscates a value
 *
 * @param mask, the value to xor with
 */
public record XorRule(long mask) implements DecodeRule {

    @Override
    public long apply(long value, int bitWidth) {
        return (value ^ mask) & widthMask(bitWidth);
    }

    @Override
    public String describe() {
        return String.format(Locale.ROOT, "XOR 0x%X", mask);
    }

    /**
     * Keeps a result inside the width of the field, so a mask wider than the field cannot leak extra bits
     */
    static long widthMask(int bitWidth) {
        return bitWidth >= 64 ? -1L : (1L << bitWidth) - 1;
    }
}
