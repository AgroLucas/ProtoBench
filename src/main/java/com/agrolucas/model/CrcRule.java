package com.agrolucas.model;

import java.util.Locale;

/**
 * Marks a Field as holding a CRC over part of the packet.
 * Unlike the other rules this does not change the value read from the packet, it says how the expected
 * value is computed so the stored one can be checked against it, see {@link FieldDecoder#checkCrc}.
 *
 * @param config, the polynomial, starting value and payload range of the CRC
 */
public record CrcRule(CrcConfig config) implements DecodeRule {

    @Override
    public long apply(long value, int bitWidth) {
        return value; // a CRC is verified, not transformed
    }

    @Override
    public String describe() {
        return String.format(Locale.ROOT, "CRC poly=0x%X init=0x%X bits %d-%d",
                config.poly(), config.init(), config.payloadStartPosition(), config.payloadEndPosition());
    }
}
