package com.agrolucas.model;

import java.util.Locale;

/**
 * Reads the value a Field holds inside a packet: pulls out its bits, runs its decode rules,
 * and formats the result the way the Field asks to be displayed.
 */
public final class FieldDecoder {

    private FieldDecoder() {
    }

    /**
     * The result of checking a CRC field against the payload it covers
     * @param valid, whether the stored value matches the computed one
     * @param stored, the value actually present in the packet
     * @param expected, the value computed from the payload
     */
    public record CrcCheck(boolean valid, long stored, long expected) {
    }

    public static int bitWidth(Field field) {
        return field.getEndPosition() - field.getStartPosition() + 1;
    }

    /**
     * The bits of the field exactly as they appear in the packet, before any rule is applied
     */
    public static long rawValue(HexPacket packet, Field field) {
        return packet.extractBits(field.getStartPosition(), field.getEndPosition());
    }

    /**
     * The value of the field once every decode rule has been applied, in order
     */
    public static long decodedValue(HexPacket packet, Field field) {
        int width = bitWidth(field);
        long value = rawValue(packet, field);

        for (DecodeRule rule : field.getDecodeRules())
            value = rule.apply(value, width);

        return value;
    }

    /**
     * The decoded value written out the way the Field is meant to be displayed
     */
    public static String formattedValue(HexPacket packet, Field field) {
        if (!fitsInPacket(packet, field))
            return "-"; // this packet is too short to contain the field

        long value = decodedValue(packet, field);
        int width = bitWidth(field);

        return switch (field.getFieldDisplay()) {
            case HEX -> String.format(Locale.ROOT, "0x%0" + Math.max(1, (width + 3) / 4) + "X", value);
            case BINARY -> String.format(Locale.ROOT, "%" + width + "s", Long.toBinaryString(value)).replace(' ', '0');
            case ASCII -> toAscii(value, width);
        };
    }

    /**
     * Check the CRC of a field against the payload it covers
     * @return the check result, or null when the field is not a CRC field or the packet is too short
     */
    public static CrcCheck checkCrc(HexPacket packet, Field field) {
        CrcRule crcRule = field.getDecodeRules().stream()
                .filter(CrcRule.class::isInstance)
                .map(CrcRule.class::cast)
                .findFirst()
                .orElse(null);

        if (crcRule == null || !fitsInPacket(packet, field))
            return null;

        CrcConfig config = crcRule.config();
        if (config.payloadEndPosition() >= packet.bitLength() || config.payloadStartPosition() < 0)
            return null; // the payload range does not exist in this packet

        int width = bitWidth(field);
        if (width < 8)
            return null; // CrcCalculator cannot work below a byte

        byte[] payload = packet.extractBytes(config.payloadStartPosition(), config.payloadEndPosition());
        long expected = CrcCalculator.compute(payload, width, config.poly(), config.init(),
                config.reflectIn(), config.reflectOut());
        expected = (expected ^ config.xorOut()) & XorRule.widthMask(width);
        long stored = decodedValue(packet, field);

        return new CrcCheck(stored == expected, stored, expected);
    }

    /**
     * Whether the packet is long enough to actually hold the field
     */
    public static boolean fitsInPacket(HexPacket packet, Field field) {
        return field.getStartPosition() >= 0 && field.getEndPosition() < packet.bitLength();
    }

    private static String toAscii(long value, int width) {
        StringBuilder text = new StringBuilder();
        for (int shift = ((width + 7) / 8 - 1) * 8; shift >= 0; shift -= 8) {
            int character = (int) ((value >>> shift) & 0xFF);
            text.append(character >= 32 && character <= 126 ? (char) character : '.');
        }
        return text.toString();
    }
}
