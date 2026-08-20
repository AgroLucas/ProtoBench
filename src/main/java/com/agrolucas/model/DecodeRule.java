package com.agrolucas.model;

/**
 * A step applied to the raw bits of a Field to turn them into a meaningful value.
 * The rules of a Field are applied in the order they were added.
 */
public sealed interface DecodeRule permits XorRule, ReverseBitsRule, CrcRule {

    /**
     * Transform a value read from a packet
     * @param value, the value so far
     * @param bitWidth, how many bits the Field covers, needed by rules that depend on the width
     * @return the transformed value
     */
    long apply(long value, int bitWidth);

    /**
     * Short human readable form, shown in the message type section
     */
    String describe();
}
