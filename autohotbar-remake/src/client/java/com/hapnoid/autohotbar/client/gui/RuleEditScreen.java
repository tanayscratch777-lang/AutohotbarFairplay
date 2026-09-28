package com.hapnoid.autohotbar.client.gui;

import com.hapnoid.autohotbar.config.ConfigManager;
import com.hapnoid.autohotbar.rule.Category;
import com.hapnoid.autohotbar.rule.ConditionalAction;
import com.hapnoid.autohotbar.rule.EnchantRequirement;
import com.hapnoid.autohotbar.rule.Rule;
import com.hapnoid.autohotbar.rule.RuleKind;
import com.hapnoid.autohotbar.rule.Variant;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * NOTE ON GUI RISK: this screen only uses Button.builder(...).build() and
 * EditBox with addRenderableWidget(...), and deliberately never overrides
 * render()/extractRenderState() or any AbstractWidget draw method - those are
 * exactly the methods a real mod's 26.1.2 upgrade changelog reported as
 * renamed (Screen.render() -> extractRenderState(), AbstractButton
 * .renderContents() -> extractContents()). By sticking to construction +
 * layout only and letting the base Screen class render everything for us,
 * this file avoids that entire risk area.
 */
public class RuleEditScreen extends Screen {
    private static final int FIELD_WIDTH = 260;
    private static final int FIELD_HEIGHT = 20;
    private static final int SPACING = 4;

    private final Screen parent;
    private final int slotNumber;
    private final Rule rule;
    private int y;

    private EditBox itemIdBox;
    private EditBox priorityBox;
    private EditBox requiredEnchantsBox;
    private EditBox alsoFillSlotsBox;
    private EditBox ifSlotBox, ifRuleBox, thenSlotBox, thenRuleBox, elseSlotBox, elseRuleBox;

    public RuleEditScreen(Screen parent, int slotNumber, Rule rule) {
        super(Component.literal("Edit rule"));
        this.parent = parent;
        this.slotNumber = slotNumber;
        this.rule = rule;
    }

    @Override
    protected void init() {
        y = 16;
        int centerX = this.width / 2;
        int left = centerX - FIELD_WIDTH / 2;

        addCycleButton("Kind: " + rule.kind, left, () -> {
            RuleKind[] values = RuleKind.values();
            rule.kind = values[(rule.kind.ordinal() + 1) % values.length];
            this.minecraft.setScreen(new RuleEditScreen(parent, slotNumber, rule));
        });

        priorityBox = addLabeledEditBox("Priority", left, String.valueOf(rule.priority));

        switch (rule.kind) {
            case SPECIFIC -> initSpecific(left);
            case TYPE -> initType(left);
            case CONDITIONAL -> initConditional(left);
            case EMPTY -> {
                // nothing extra to configure
            }
        }

        alsoFillSlotsBox = addLabeledEditBox("Also fill slots (comma-separated, e.g. 2,3)", left,
                joinInts(rule.alsoFillSlots));

        y += SPACING * 2;
        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
            applyAndSave();
            this.minecraft.setScreen(new SlotRuleListScreen(parent, slotNumber));
        }).bounds(left, y, FIELD_WIDTH / 2 - 2, FIELD_HEIGHT).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b ->
                this.minecraft.setScreen(new SlotRuleListScreen(parent, slotNumber))
        ).bounds(left + FIELD_WIDTH / 2 + 2, y, FIELD_WIDTH / 2 - 2, FIELD_HEIGHT).build());
    }

    private void initSpecific(int left) {
        itemIdBox = addLabeledEditBox("Item id (e.g. minecraft:stone)", left,
                rule.specificItemId == null ? "" : rule.specificItemId);

        addCycleButton("Must be enchanted: " + rule.mustBeEnchanted, left, () -> {
            rule.mustBeEnchanted = !rule.mustBeEnchanted;
            this.minecraft.setScreen(new RuleEditScreen(parent, slotNumber, rule));
        });

        requiredEnchantsBox = addLabeledEditBox("Required enchants (id:level, comma-separated)", left,
                joinEnchants(rule.requiredEnchants));
    }

    private void initType(int left) {
        addCycleButton("Category: " + (rule.category == null ? Category.SWORD : rule.category), left, () -> {
            Category[] values = Category.values();
            int next = (rule.category == null ? -1 : rule.category.ordinal()) + 1;
            rule.category = values[next % values.length];
            this.minecraft.setScreen(new RuleEditScreen(parent, slotNumber, rule));
        });

        addCycleButton("Variant: " + (rule.variant == null ? Variant.BEST_MATERIAL : rule.variant), left, () -> {
            Variant[] values = Variant.values();
            int next = (rule.variant == null ? -1 : rule.variant.ordinal()) + 1;
            rule.variant = values[next % values.length];
            this.minecraft.setScreen(new RuleEditScreen(parent, slotNumber, rule));
        });

        requiredEnchantsBox = addLabeledEditBox("Required enchants for ranking (id:level, comma-separated)", left,
                joinEnchants(rule.typeEnchantFilter));
    }

    private void initConditional(int left) {
        ifSlotBox = addLabeledEditBox("IF slot", left, String.valueOf(rule.ifSlot));
        ifRuleBox = addLabeledEditBox("IF rule #", left, String.valueOf(rule.ifRule));

        addCycleButton("THEN: " + rule.thenAction, left, () -> {
            ConditionalAction[] values = ConditionalAction.values();
            rule.thenAction = values[(rule.thenAction.ordinal() + 1) % values.length];
            this.minecraft.setScreen(new RuleEditScreen(parent, slotNumber, rule));
        });
        thenSlotBox = addLabeledEditBox("THEN slot", left, String.valueOf(rule.thenSlot));
        thenRuleBox = addLabeledEditBox("THEN rule #", left, String.valueOf(rule.thenRule));

        addCycleButton("ELSE: " + rule.elseAction, left, () -> {
            ConditionalAction[] values = ConditionalAction.values();
            rule.elseAction = values[(rule.elseAction.ordinal() + 1) % values.length];
            this.minecraft.setScreen(new RuleEditScreen(parent, slotNumber, rule));
        });
        elseSlotBox = addLabeledEditBox("ELSE slot", left, String.valueOf(rule.elseSlot));
        elseRuleBox = addLabeledEditBox("ELSE rule #", left, String.valueOf(rule.elseRule));
    }

    private void applyAndSave() {
        rule.priority = parseIntSafe(priorityBox.getValue(), rule.priority);
        rule.alsoFillSlots = parseIntList(alsoFillSlotsBox.getValue());

        switch (rule.kind) {
            case SPECIFIC -> {
                rule.specificItemId = itemIdBox.getValue().trim();
                rule.requiredEnchants = parseEnchants(requiredEnchantsBox.getValue());
            }
            case TYPE -> {
                if (rule.category == null) rule.category = Category.SWORD;
                if (rule.variant == null) rule.variant = Variant.BEST_MATERIAL;
                rule.typeEnchantFilter = parseEnchants(requiredEnchantsBox.getValue());
            }
            case CONDITIONAL -> {
                rule.ifSlot = parseIntSafe(ifSlotBox.getValue(), rule.ifSlot);
                rule.ifRule = parseIntSafe(ifRuleBox.getValue(), rule.ifRule);
                rule.thenSlot = parseIntSafe(thenSlotBox.getValue(), rule.thenSlot);
                rule.thenRule = parseIntSafe(thenRuleBox.getValue(), rule.thenRule);
                rule.elseSlot = parseIntSafe(elseSlotBox.getValue(), rule.elseSlot);
                rule.elseRule = parseIntSafe(elseRuleBox.getValue(), rule.elseRule);
            }
            case EMPTY -> {
                // nothing to apply
            }
        }
        ConfigManager.save();
    }

    // ---- small layout helpers ----

    private void addCycleButton(String label, int left, Runnable onPress) {
        this.addRenderableWidget(Button.builder(Component.literal(label), b -> onPress.run())
                .bounds(left, y, FIELD_WIDTH, FIELD_HEIGHT).build());
        y += FIELD_HEIGHT + SPACING;
    }

    private EditBox addLabeledEditBox(String label, int left, String initialValue) {
        EditBox box = new EditBox(this.font, left, y, FIELD_WIDTH, FIELD_HEIGHT, Component.literal(label));
        box.setMaxLength(256);
        box.setValue(initialValue);
        box.setHint(Component.literal(label));
        this.addRenderableWidget(box);
        y += FIELD_HEIGHT + SPACING;
        return box;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- parsing helpers ----

    private static int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static List<Integer> parseIntList(String s) {
        List<Integer> result = new ArrayList<>();
        if (s == null || s.isBlank()) return result;
        for (String part : s.split(",")) {
            try {
                result.add(Integer.parseInt(part.trim()));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private static String joinInts(List<Integer> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(values.get(i));
        }
        return sb.toString();
    }

    private static List<EnchantRequirement> parseEnchants(String s) {
        List<EnchantRequirement> result = new ArrayList<>();
        if (s == null || s.isBlank()) return result;
        for (String part : s.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            String id = trimmed;
            int level = 1;
            int lastColon = trimmed.lastIndexOf(':');
            if (lastColon > 0) {
                String tail = trimmed.substring(lastColon + 1).trim();
                if (tail.matches("\\d+")) {
                    id = trimmed.substring(0, lastColon).trim();
                    level = Integer.parseInt(tail);
                }
            }
            if (!id.contains(":")) {
                id = "minecraft:" + id; // allow typing "sharpness:5"
            }
            result.add(new EnchantRequirement(id, level));
        }
        return result;
    }

    private static String joinEnchants(List<EnchantRequirement> reqs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reqs.size(); i++) {
            if (i > 0) sb.append(",");
            EnchantRequirement r = reqs.get(i);
            sb.append(r.enchantmentId).append(":").append(r.minLevel);
        }
        return sb.toString();
    }
}
