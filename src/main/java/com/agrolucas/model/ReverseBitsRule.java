package com.agrolucas.model;

/**
 * Reverse the bit order of the field, for protocols that send a value least significant bit first
 */
public record ReverseBitsRule() implements DecodeRule {

    @Override
    public long apply(long value, int bitWidth) {
        long reversed = 0;
        for (int i = 0; i < bitWidth; i++) {
            reversed = (reversed << 1) | ((value >>> i) & 1);
        }
        return reversed & XorRule.widthMask(bitWidth);
    }

    @Override
    public String describe() {
        return "Reverse bits";
    }
}
