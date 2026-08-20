package com.agrolucas.view;

import com.agrolucas.model.CrcConfig;
import com.agrolucas.model.CrcRule;
import com.agrolucas.model.DecodeRule;
import com.agrolucas.model.Field;
import com.agrolucas.model.ReverseBitsRule;
import com.agrolucas.model.XorRule;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;

/**
 * Adds, edits, reorders and removes the decode rules of one Field.
 * Rules are applied top to bottom, so the first row is the first step.
 */
public class DecodeRulesDialog extends Dialog<Void> {

    private enum RuleType {
        XOR("Xor with a mask"),
        REVERSE("Reverse the bits"),
        CRC("Crc check");

        private final String label;

        RuleType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final Field field;
    private final VBox ruleList = new VBox(6);
    private final VBox parameterBox = new VBox(8);
    private final ComboBox<RuleType> ruleTypeComboBox = new ComboBox<>();
    private final Button submitButton = new Button("Add rule");
    private final Button cancelEditButton = new Button("Cancel edit");

    // index of the rule being edited, -1 when the form is adding a new one instead
    private int editingIndex = -1;

    // parameters, only the ones belonging to the selected rule type are shown
    private final TextField xorMaskField = new TextField("0xFF");
    private final TextField crcPolyField = new TextField("0x1021");
    private final TextField crcInitField = new TextField("0xFFFF");
    private final TextField crcPayloadStartField = new TextField("0");
    private final TextField crcPayloadEndField = new TextField("0");
    private final TextField crcXorOutField = new TextField("0x0");
    private final CheckBox crcReflectInBox = new CheckBox("Reflect in");
    private final CheckBox crcReflectOutBox = new CheckBox("Reflect out");

    public DecodeRulesDialog(Field field) {
        this.field = field;

        setTitle("Decode rules");
        setHeaderText(null);
        setResizable(true); // the CRC form is tall, let the window be grown
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        getDialogPane().getStyleClass().add("rules-dialog");

        Label fieldLabel = new Label(field.getName() + "  ·  bits " + field.getStartPosition() + "-" + field.getEndPosition());
        fieldLabel.getStyleClass().add("viewed-type-name");

        Label currentTitle = new Label("CURRENT RULES");
        currentTitle.getStyleClass().add("card-title");

        Label orderHint = new Label("Applied from top to bottom, the first row runs first.");
        orderHint.getStyleClass().add("card-hint");

        Label addTitle = new Label("ADD A RULE");
        addTitle.getStyleClass().add("card-title");

        ruleTypeComboBox.getItems().addAll(RuleType.values());
        ruleTypeComboBox.setValue(RuleType.XOR);
        ruleTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> showParametersFor(newVal));

        submitButton.getStyleClass().add("accent");
        submitButton.setOnAction(e -> submitRule());

        cancelEditButton.getStyleClass().add("ghost");
        cancelEditButton.setOnAction(e -> stopEditing());
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);

        HBox addRow = new HBox(10, ruleTypeComboBox, submitButton, cancelEditButton);
        addRow.setAlignment(Pos.CENTER_LEFT);

        // the payload range defaults to everything before the field itself, the usual case for a trailing CRC
        crcPayloadEndField.setText(String.valueOf(Math.max(0, field.getStartPosition() - 1)));

        showParametersFor(RuleType.XOR);
        refreshRuleList();

        VBox content = new VBox(12, fieldLabel, currentTitle, orderHint, ruleList, addTitle, addRow, parameterBox);
        content.setPadding(new Insets(18));

        // without this the dialog sizes itself to the shortest form and clips the CRC one
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("rules-scroll");
        scrollPane.setPrefViewportWidth(640); // wide enough for a rule description plus its buttons
        scrollPane.setPrefViewportHeight(520); // tall enough that the CRC form is never clipped
        getDialogPane().setContent(scrollPane);
    }

    /**
     * Redraw the list of rules currently on the field, each with buttons to move, edit and remove it
     */
    private void refreshRuleList() {
        ruleList.getChildren().clear();

        List<DecodeRule> rules = field.getDecodeRules();
        if (rules.isEmpty()) {
            Label empty = new Label("No rule yet, the raw bits are used as they are.");
            empty.getStyleClass().add("card-hint");
            ruleList.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < rules.size(); i++) {
            int index = i; // captured by the handlers below, so it must not change

            Label step = new Label((i + 1) + ".");
            step.getStyleClass().add("inline-label");

            // the description is the only part allowed to shrink, so the buttons always stay readable
            Label description = new Label(rules.get(i).describe());
            description.setTooltip(new Tooltip(rules.get(i).describe()));
            description.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(description, Priority.ALWAYS);

            Button moveUpButton = iconButton("↑", "Run this rule earlier");
            moveUpButton.setDisable(index == 0);
            moveUpButton.setOnAction(e -> swapRules(index, index - 1));

            Button moveDownButton = iconButton("↓", "Run this rule later");
            moveDownButton.setDisable(index == rules.size() - 1);
            moveDownButton.setOnAction(e -> swapRules(index, index + 1));

            Button editButton = iconButton("Edit", "Load this rule into the form below");
            editButton.setOnAction(e -> startEditing(index));

            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().add("danger");
            removeButton.setMinWidth(Region.USE_PREF_SIZE);
            removeButton.setOnAction(e -> {
                field.getDecodeRules().remove(index);
                stopEditing(); // the edited index may no longer point at the same rule
            });

            HBox row = new HBox(8, step, description, moveUpButton, moveDownButton, editButton, removeButton);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("rule-row");
            if (index == editingIndex)
                row.getStyleClass().add("rule-row-editing");

            ruleList.getChildren().add(row);
        }
    }

    private Button iconButton(String text, String tooltip) {
        Button button = new Button(text);
        button.getStyleClass().add("ghost");
        button.setTooltip(new Tooltip(tooltip));
        button.setMinWidth(Region.USE_PREF_SIZE); // never let the row squeeze the label into "..."
        return button;
    }

    /**
     * Swap two rules, which changes the order they are applied in
     */
    private void swapRules(int first, int second) {
        List<DecodeRule> rules = field.getDecodeRules();
        if (first < 0 || second < 0 || first >= rules.size() || second >= rules.size())
            return;

        DecodeRule moved = rules.get(first);
        rules.set(first, rules.get(second));
        rules.set(second, moved);
        stopEditing();
    }

    /**
     * Load an existing rule into the form so it can be changed, instead of adding a new one
     */
    private void startEditing(int index) {
        DecodeRule rule = field.getDecodeRules().get(index);
        editingIndex = index;

        switch (rule) {
            case XorRule xorRule -> {
                ruleTypeComboBox.setValue(RuleType.XOR);
                xorMaskField.setText(String.format(Locale.ROOT, "0x%X", xorRule.mask()));
            }
            case ReverseBitsRule ignored -> ruleTypeComboBox.setValue(RuleType.REVERSE);
            case CrcRule crcRule -> {
                ruleTypeComboBox.setValue(RuleType.CRC);
                CrcConfig config = crcRule.config();
                crcPolyField.setText(String.format(Locale.ROOT, "0x%X", config.poly()));
                crcInitField.setText(String.format(Locale.ROOT, "0x%X", config.init()));
                crcPayloadStartField.setText(String.valueOf(config.payloadStartPosition()));
                crcPayloadEndField.setText(String.valueOf(config.payloadEndPosition()));
                crcXorOutField.setText(String.format(Locale.ROOT, "0x%X", config.xorOut()));
                crcReflectInBox.setSelected(config.reflectIn());
                crcReflectOutBox.setSelected(config.reflectOut());
            }
        }

        submitButton.setText("Save changes");
        cancelEditButton.setVisible(true);
        cancelEditButton.setManaged(true);
        refreshRuleList();
    }

    /**
     * Leave edit mode and put the form back to adding a new rule
     */
    private void stopEditing() {
        editingIndex = -1;
        submitButton.setText("Add rule");
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);
        refreshRuleList();
    }

    /**
     * Show only the parameters that the chosen rule type needs
     */
    private void showParametersFor(RuleType ruleType) {
        parameterBox.getChildren().clear();

        switch (ruleType) {
            case XOR -> parameterBox.getChildren().add(labelled("Mask", xorMaskField));
            case REVERSE -> {
                Label none = new Label("Reverses the bit order of the field, nothing to configure.");
                none.getStyleClass().add("card-hint");
                parameterBox.getChildren().add(none);
            }
            case CRC -> {
                parameterBox.getChildren().addAll(
                        labelled("Polynomial", crcPolyField),
                        labelled("Init value", crcInitField),
                        labelled("Payload first bit", crcPayloadStartField),
                        labelled("Payload last bit", crcPayloadEndField),
                        labelled("Final XOR", crcXorOutField),
                        new HBox(14, crcReflectInBox, crcReflectOutBox));

                Label hint = new Label("Values accept 0x hex or plain decimal. The CRC width is the width of the field itself.");
                hint.getStyleClass().add("card-hint");
                hint.setWrapText(true);
                parameterBox.getChildren().add(hint);
            }
        }
    }

    private HBox labelled(String text, TextField input) {
        Label label = new Label(text);
        label.getStyleClass().add("inline-label");
        label.setMinWidth(110);
        input.setPrefWidth(160);

        HBox row = new HBox(10, label, input);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /**
     * Build a rule from the parameters on screen, then either append it or replace the one being edited.
     * Unparseable numbers simply abort rather than throwing.
     */
    private void submitRule() {
        DecodeRule rule = buildRule();
        if (rule == null)
            return;

        if (editingIndex >= 0 && editingIndex < field.getDecodeRules().size())
            field.getDecodeRules().set(editingIndex, rule);
        else
            field.getDecodeRules().add(rule);

        stopEditing();
    }

    /**
     * @return the rule described by the form, or null when a value could not be read
     */
    private DecodeRule buildRule() {
        return switch (ruleTypeComboBox.getValue()) {
            case XOR -> {
                Long mask = parseNumber(xorMaskField.getText());
                yield mask == null ? null : new XorRule(mask);
            }
            case REVERSE -> new ReverseBitsRule();
            case CRC -> {
                Long poly = parseNumber(crcPolyField.getText());
                Long init = parseNumber(crcInitField.getText());
                Long payloadStart = parseNumber(crcPayloadStartField.getText());
                Long payloadEnd = parseNumber(crcPayloadEndField.getText());
                Long xorOut = parseNumber(crcXorOutField.getText());

                if (poly == null || init == null || payloadStart == null || payloadEnd == null || xorOut == null)
                    yield null;

                yield new CrcRule(new CrcConfig(poly, init,
                        payloadStart.intValue(), payloadEnd.intValue(),
                        crcReflectInBox.isSelected(), crcReflectOutBox.isSelected(), xorOut));
            }
        };
    }

    /**
     * Read a number written either as 0x hex or as plain decimal
     * @return the value, or null when it cannot be read
     */
    private Long parseNumber(String text) {
        if (text == null || text.isBlank())
            return null;

        String cleaned = text.trim().toLowerCase(Locale.ROOT);
        try {
            return cleaned.startsWith("0x")
                    ? Long.parseLong(cleaned.substring(2), 16)
                    : Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
