package com.agrolucas.model;

/**
 * How to compute the CRC that a Field holds.
 * The positions are bit positions inside the packet, like a Field's own start and end.
 *
 * @param poly, the generator polynomial, without its top bit (e.g. 0x1021 for CRC-16/CCITT)
 * @param init, the value the register starts from
 * @param payloadStartPosition, first bit the CRC is computed over (inclusive)
 * @param payloadEndPosition, last bit the CRC is computed over (inclusive)
 * @param reflectIn, whether every input byte is bit-reversed before being fed in
 * @param reflectOut, whether the final value is bit-reversed
 * @param xorOut, value the result is xored with at the very end. 0 for CRC-16/CCITT, but
 *                0xFFFFFFFF for CRC-32, without which that variant comes out wrong
 */
public record CrcConfig(long poly,
                        long init,
                        int payloadStartPosition,
                        int payloadEndPosition,
                        boolean reflectIn,
                        boolean reflectOut,
                        long xorOut) {

    public CrcConfig(long poly, long init, int payloadStartPosition, int payloadEndPosition) {
        this(poly, init, payloadStartPosition, payloadEndPosition, false, false, 0);
    }
}
