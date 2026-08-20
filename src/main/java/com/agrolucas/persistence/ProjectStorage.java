package com.agrolucas.persistence;

import com.agrolucas.model.Capture;
import com.agrolucas.model.CrcConfig;
import com.agrolucas.model.CrcRule;
import com.agrolucas.model.DecodeRule;
import com.agrolucas.model.Field;
import com.agrolucas.model.FieldDisplay;
import com.agrolucas.model.HexPacket;
import com.agrolucas.model.MessageType;
import com.agrolucas.model.ReverseBitsRule;
import com.agrolucas.model.XorRule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes a project as JSON.
 * <p>
 * The mapping is written out by hand rather than letting Gson reflect over the model classes, for two
 * reasons: the model would otherwise need serialization annotations on it, and DecodeRule is a sealed
 * interface whose implementations Gson cannot pick between without being told how.
 */
public final class ProjectStorage {

    private static final int FORMAT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ProjectStorage() {
    }

    public static void save(Path file, ProjectData project) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("version", FORMAT_VERSION);
        root.addProperty("referenceIndex", project.referenceIndex());
        root.addProperty("viewedMessageTypeIndex", project.viewedMessageTypeIndex());

        JsonArray captures = new JsonArray();
        for (Capture capture : project.captures()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", capture.getName());
            entry.addProperty("hex", capture.getHexPacket().toHexString());
            captures.add(entry);
        }
        root.add("captures", captures);

        JsonArray messageTypes = new JsonArray();
        for (MessageType messageType : project.messageTypes())
            messageTypes.add(toJson(messageType));
        root.add("messageTypes", messageTypes);

        if (file.getParent() != null)
            Files.createDirectories(file.getParent());

        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    public static ProjectData load(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null)
                throw new IOException("The project file is empty");

            List<Capture> captures = new ArrayList<>();
            for (var element : optionalArray(root, "captures")) {
                JsonObject entry = element.getAsJsonObject();
                captures.add(new Capture(
                        entry.get("name").getAsString(),
                        new HexPacket(entry.get("hex").getAsString())));
            }

            List<MessageType> messageTypes = new ArrayList<>();
            for (var element : optionalArray(root, "messageTypes"))
                messageTypes.add(messageTypeFromJson(element.getAsJsonObject()));

            return new ProjectData(captures, messageTypes,
                    optionalInt(root, "referenceIndex", -1),
                    optionalInt(root, "viewedMessageTypeIndex", -1));
        } catch (RuntimeException e) {
            // anything malformed surfaces as one readable failure rather than a Gson specific exception
            throw new IOException("The project file could not be read: " + e.getMessage(), e);
        }
    }

    private static JsonObject toJson(MessageType messageType) {
        JsonObject entry = new JsonObject();
        entry.addProperty("name", messageType.getName());

        JsonArray fields = new JsonArray();
        for (Field field : messageType.getFields())
            fields.add(toJson(field));
        entry.add("fields", fields);

        return entry;
    }

    private static JsonObject toJson(Field field) {
        JsonObject entry = new JsonObject();
        entry.addProperty("name", field.getName());
        entry.addProperty("start", field.getStartPosition());
        entry.addProperty("end", field.getEndPosition());
        entry.addProperty("display", field.getFieldDisplay().name());
        entry.addProperty("color", field.getColor());

        JsonArray rules = new JsonArray();
        for (DecodeRule rule : field.getDecodeRules())
            rules.add(toJson(rule));
        entry.add("rules", rules);

        return entry;
    }

    /**
     * Each rule writes a "type" telling {@link #ruleFromJson} which one to rebuild
     */
    private static JsonObject toJson(DecodeRule rule) {
        JsonObject entry = new JsonObject();

        switch (rule) {
            case XorRule xorRule -> {
                entry.addProperty("type", "XOR");
                entry.addProperty("mask", xorRule.mask());
            }
            case ReverseBitsRule ignored -> entry.addProperty("type", "REVERSE");
            case CrcRule crcRule -> {
                CrcConfig config = crcRule.config();
                entry.addProperty("type", "CRC");
                entry.addProperty("poly", config.poly());
                entry.addProperty("init", config.init());
                entry.addProperty("payloadStart", config.payloadStartPosition());
                entry.addProperty("payloadEnd", config.payloadEndPosition());
                entry.addProperty("reflectIn", config.reflectIn());
                entry.addProperty("reflectOut", config.reflectOut());
                entry.addProperty("xorOut", config.xorOut());
            }
        }

        return entry;
    }

    private static MessageType messageTypeFromJson(JsonObject entry) {
        List<Field> fields = new ArrayList<>();
        for (var element : optionalArray(entry, "fields"))
            fields.add(fieldFromJson(element.getAsJsonObject()));

        return new MessageType(entry.get("name").getAsString(), fields);
    }

    private static Field fieldFromJson(JsonObject entry) {
        Field field = new Field(
                entry.get("name").getAsString(),
                entry.get("start").getAsInt(),
                entry.get("end").getAsInt(),
                FieldDisplay.valueOf(entry.get("display").getAsString()),
                entry.has("color") ? entry.get("color").getAsString() : Field.DEFAULT_COLOR);

        for (var element : optionalArray(entry, "rules")) {
            DecodeRule rule = ruleFromJson(element.getAsJsonObject());
            if (rule != null)
                field.getDecodeRules().add(rule);
        }

        return field;
    }

    /**
     * @return the rule, or null when the type is one this version does not know about
     */
    private static DecodeRule ruleFromJson(JsonObject entry) {
        return switch (entry.get("type").getAsString()) {
            case "XOR" -> new XorRule(entry.get("mask").getAsLong());
            case "REVERSE" -> new ReverseBitsRule();
            case "CRC" -> new CrcRule(new CrcConfig(
                    entry.get("poly").getAsLong(),
                    entry.get("init").getAsLong(),
                    entry.get("payloadStart").getAsInt(),
                    entry.get("payloadEnd").getAsInt(),
                    entry.get("reflectIn").getAsBoolean(),
                    entry.get("reflectOut").getAsBoolean(),
                    entry.has("xorOut") ? entry.get("xorOut").getAsLong() : 0));
            default -> null;
        };
    }

    private static JsonArray optionalArray(JsonObject parent, String name) {
        return parent.has(name) && parent.get(name).isJsonArray()
                ? parent.getAsJsonArray(name)
                : new JsonArray();
    }

    private static int optionalInt(JsonObject parent, String name, int fallback) {
        return parent.has(name) ? parent.get(name).getAsInt() : fallback;
    }
}
