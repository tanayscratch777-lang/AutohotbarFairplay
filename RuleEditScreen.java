package com.hapnoid.autohotbar.client.gui;

import com.hapnoid.autohotbar.config.ConfigManager;
import com.hapnoid.autohotbar.rule.Category;
import com.hapnoid.autohotbar.rule.ConditionalAction;
import com.hapnoid.autohotbar.rule.EnchantRequirement;
import com.hapnoid.autohotbar.rule.Rule;
import com.hapnoid.autohotbar.rule.RuleKind;
import com.hapnoid.autohotbar.rule.Variant;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Rebuilt to match the original screenshots: real tabs with an underline,
 * a "Pick item..." button that opens the actual item grid instead of a text
 * box, a required-enchants list with +/-/x rows fed by the enchant picker
 * instead of typed "id:level" text, and a relabeled Conditional tab. See
 * ItemPickerScreen/EnchantPickerScreen for the riskiest parts of this.
 */
public class RuleEditScreen extends Screen {
    private static final int FIELD_WIDTH = 280;
    private static final int FIELD_HEIGHT = 20;
    private static final int SPACING = 4;
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private final Screen parent;
    private final int slotNumber;
    private final Rule rule;
    private int y;
    private int panelLeft, panelTop, panelWidth, panelHeight;

    private EditBox priorityBox;
    private EditBox alsoFillSlotsBox;
    private EditBox ifSlotBox, ifRuleBox, thenSlotBox, thenRuleBox, elseSlotBox, elseRuleBox;

    // (label text, y position) pairs for the required-enchant rows, drawn externally.
    private final List<int[]> enchantRowYs = new ArrayList<>();
    private List<EnchantRequirement> activeEnchantList; // whichever list (specific or type) is being edited this build

    public RuleEditScreen(Screen parent, int slotNumber, Rule rule) {
        super(Component.literal("Edit rule"));
        this.parent = parent;
        this.slotNumber = slotNumber;
        this.rule = rule;
    }

    @Override
    protected void init() {
        panelLeft = this.width / 2 - FIELD_WIDTH / 2 - 10;
        panelTop = 10;
        panelWidth = FIELD_WIDTH + 20;
        int left = panelLeft + 10;
        y = panelTop + 8;
        enchantRowYs.clear();
        activeEnchantList = null;

        addTabRow(left);
        y += 4;

        priorityBox = addLabeledEditBox("Priority", left, String.valueOf(rule.priority));

        switch (rule.kind) {
            case SPECIFIC -> initSpecific(left);
            case TYPE -> initType(left);
            case CONDITIONAL -> initConditional(left);
            case EMPTY -> y += 2;
        }

        alsoFillSlotsBox = addLabeledEditBox("Also fill slots (e.g. 2,3)", left, joinInts(rule.alsoFillSlots));

        y += SPACING * 2;
        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
            applyFields();
            this.minecraft.setScreen(new SlotRuleListScreen(parent, slotNumber));
        }).bounds(left, y, FIELD_WIDTH / 2 - 2, FIELD_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b ->
                this.minecraft.setScreen(new SlotRuleListScreen(parent, slotNumber))
        ).bounds(left + FIELD_WIDTH / 2 + 2, y, FIELD_WIDTH / 2 - 2, FIELD_HEIGHT).build());

        panelHeight = (y + FIELD_HEIGHT + 10) - panelTop;

        int pl = panelLeft, pt = panelTop, pw = panelWidth, ph = panelHeight;
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (screen != this) return;
            ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickDelta) -> drawChrome(graphics, pl, pt, pw, ph));
        });
    }

    private void addTabRow(int left) {
        RuleKind[] kinds = RuleKind.values();
        int tabWidth = FIELD_WIDTH / kinds.length;
        for (int i = 0; i < kinds.length; i++) {
            RuleKind kind = kinds[i];
            int tx = left + i * tabWidth;
            boolean selected = rule.kind == kind;
            this.addRenderableWidget(Button.builder(Component.literal(label(kind)), b -> {
                applyFields();
                rule.kind = kind;
                this.minecraft.setScreen(new RuleEditScreen(parent, slotNumber, rule));
            }).bounds(tx, y, tabWidth - 1, FIELD_HEIGHT).build());
            int fy = y;
            if (selected) {
                ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
                    if (screen != this) return;
                    ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickDelta) ->
                            GlassPanel.tabUnderline(graphics, tx, fy + FIELD_HEIGHT, tabWidth - 1, true));
                });
            }
        }
        y += FIELD_HEIGHT + SPACING;
    }

    private static String label(RuleKind kind) {
        return switch (kind) {
            case SPECIFIC -> "Specific";
            case TYPE -> "Type";
            case CONDITIONAL -> "Conditional";
            case EMPTY -> "Empty";
        };
    }

    private void initSpecific(int left) {
        String itemLabel = rule.specificItemId == null || rule.specificItemId.isEmpty() ? "Pick item..." : rule.specificItemId;
        this.addRenderableWidget(Button.builder(Component.literal(itemLabel), b -> {
            applyFields();
            this.minecraft.setScreen(new ItemPickerScreen(this, "", id -> {
                rule.specificItemId = id;
                this.minecraft.setScreen(new RuleEditScreen(parent, slotNumber, rule));
            }));
        }).bounds(left, y, FIELD_WIDTH, FIELD_HEIGHT).build());
        y += FIELD_HEIGHT + SPACING;

        addCycleButton("Must be enchanted: " + rule.mustBeEnchanted, left, () -> rule.mustBeEnchanted = !rule.mustBeEnchanted);

        activeEnchantList = rule.requiredEnchants;
        addEnchantList(left, "Required enchants");
    }

    private void initType(int left) {
        addCycleButton("Category: " + (rule.category == null ? Category.SWORD : rule.category), left, () -> {
            Category[] values = Category.values();
            int next = (rule.category == null ? -1 : rule.category.ordinal()) + 1;
            rule.category = values[next % values.length];
        });
        addCycleButton("Variant: " + (rule.variant == null ? Variant.BEST_MATERIAL : rule.variant), left, () -> {
            Variant[] values = Variant.values();
            int next = (rule.variant == null ? -1 : rule.variant.ordinal()) + 1;
            rule.variant = values[next % values.length];
        });

        activeEnchantList = rule.typeEnchantFilter;
        addEnchantList(left, "Required enchants for ranking");
    }

    /** Cycle buttons apply+rebuild immediately, so they don't need a separate "apply" step. */
    private void addCycleButton(String label, int left, Runnable mutate) {
        this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
            applyFields();
            mutate.run();
            this.minecraft.setScreen(new RuleEditScreen(parent, slotNumber, rule));
        }).bounds(left, y, FIELD_WIDTH, FIELD_HEIGHT).build());
        y += FIELD_HEIGHT + SPACING;
    }

    private void addEnchantList(int left, String heading) {
        y += 10; // room for the heading text, drawn externally
        List<EnchantRequirement> list = activeEnchantList;
        for (int i = 0; i < list.size(); i++) {
            int index = i;
            EnchantRequirement req = list.get(i);
            int rowY = y;
            enchantRowYs.add(new int[]{rowY});

            this.addRenderableWidget(Button.builder(Component.literal("-"), b -> {
                req.minLevel = Math.max(1, req.minLevel - 1);
                this.minecraft.setScreen(new RuleEditScreen(parent, slotNumber, rule));
            }).bounds(left + FIELD_WIDTH - 66, rowY, 18, 16).build());
            this.addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                req.minLevel = Math.min(10, req.minLevel + 1);
                this.minecraft.setScreen(new RuleEditScreen(parent, slotNumber, rule));
            }).bounds(left + FIELD_WIDTH - 46, rowY, 18, 16).build());
            this.addRenderableWidget(Button.builder(Component.literal("X"), b -> {
                list.remove(index);
                this.minecraft.setScreen(new RuleEditScreen(parent, slotNumber, rule));
            }).bounds(left + FIELD_WIDTH - 20, rowY, 20, 16).build());

            y += 18;
        }

        this.addRenderableWidget(Button.builder(Component.literal("+ Add required enchant"), b -> {
            applyFields();
            this.minecraft.setScreen(new EnchantPickerScreen(this, (id, level) -> {
                list.add(new EnchantRequirement(id, level));
                this.minecraft.setScreen(new RuleEditScreen(parent, slotNumber, rule));
            }));
        }).bounds(left, y, FIELD_WIDTH, FIELD_HEIGHT).build());
        y += FIELD_HEIGHT + SPACING;

        String headingText = heading;
        int hy = y - (list.size() * 18) - FIELD_HEIGHT - SPACING - 10;
        int fLeft = left;
        List<EnchantRequirement> fList = list;
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (screen != this) return;
            ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickDelta) -> {
                graphics.text(this.font, headingText, fLeft, hy, GlassPanel.TEXT_DIM, false);
                for (int i = 0; i < fList.size(); i++) {
                    EnchantRequirement req = fList.get(i);
                    int rowTop = hy + 10 + i * 18;
                    String levelRoman = req.minLevel < ROMAN.length ? ROMAN[req.minLevel] : String.valueOf(req.minLevel);
                    String niceName = req.enchantmentId.contains(":") ? req.enchantmentId.substring(req.enchantmentId.indexOf(':') + 1) : req.enchantmentId;
                    niceName = niceName.replace('_', ' ');
                    graphics.text(this.font, capitalize(niceName) + " " + levelRoman, fLeft, rowTop + 4, GlassPanel.TEXT, false);
                }
            });
        });
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void initConditional(int left) {
        y += 10;
        int headingY = y;
        y += 10;

        ifSlotBox = addLabeledEditBox("IF slot", left, String.valueOf(rule.ifSlot));
        ifRuleBox = addLabeledEditBox("IF rule #", left, String.valueOf(rule.ifRule));

        y += 6;
        addCycleButton("THEN: " + rule.thenAction, left, () -> {
            ConditionalAction[] values = ConditionalAction.values();
            rule.thenAction = values[(rule.thenAction.ordinal() + 1) % values.length];
        });
        thenSlotBox = addLabeledEditBox("THEN slot", left, String.valueOf(rule.thenSlot));
        thenRuleBox = addLabeledEditBox("THEN rule #", left, String.valueOf(rule.thenRule));

        y += 6;
        addCycleButton("ELSE: " + rule.elseAction, left, () -> {
            ConditionalAction[] values = ConditionalAction.values();
            rule.elseAction = values[(rule.elseAction.ordinal() + 1) % values.length];
        });
        elseSlotBox = addLabeledEditBox("ELSE slot", left, String.valueOf(rule.elseSlot));
        elseRuleBox = addLabeledEditBox("ELSE rule #", left, String.valueOf(rule.elseRule));

        y += 4;
        int legendY = y;
        int fLeft = left;
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (screen != this) return;
            ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickDelta) -> {
                graphics.text(this.font, "Branch on whether another rule found an item:", fLeft, headingY, GlassPanel.TEXT_DIM, false);
                graphics.text(this.font, "USE = pull that rule into this slot.", fLeft, legendY, GlassPanel.TEXT_DIM, false);
                graphics.text(this.font, "NEXT = fall to next rule. STOP = leave empty.", fLeft, legendY + 10, GlassPanel.TEXT_DIM, false);
            });
        });
        y += 24;
    }

    private void applyFields() {
        rule.priority = parseIntSafe(priorityBox.getValue(), rule.priority);
        rule.alsoFillSlots = parseIntList(alsoFillSlotsBox.getValue());

        if (rule.kind == RuleKind.CONDITIONAL) {
            rule.ifSlot = parseIntSafe(ifSlotBox.getValue(), rule.ifSlot);
            rule.ifRule = parseIntSafe(ifRuleBox.getValue(), rule.ifRule);
            rule.thenSlot = parseIntSafe(thenSlotBox.getValue(), rule.thenSlot);
            rule.thenRule = parseIntSafe(thenRuleBox.getValue(), rule.thenRule);
            rule.elseSlot = parseIntSafe(elseSlotBox.getValue(), rule.elseSlot);
            rule.elseRule = parseIntSafe(elseRuleBox.getValue(), rule.elseRule);
        }
        if (rule.kind == RuleKind.TYPE) {
            if (rule.category == null) rule.category = Category.SWORD;
            if (rule.variant == null) rule.variant = Variant.BEST_MATERIAL;
        }
        ConfigManager.save();
    }

    private void drawChrome(GuiGraphicsExtractor g, int left, int top, int w, int h) {
        GlassPanel.panel(g, left, top, w, h);
    }

    private EditBox addLabeledEditBox(String label, int left, String initialValue) {
        EditBox box = new EditBox(this.font, left, y + 10, FIELD_WIDTH, FIELD_HEIGHT, Component.literal(label));
        box.setMaxLength(256);
        box.setValue(initialValue);
        box.setHint(Component.literal(label));
        this.addRenderableWidget(box);

        int labelY = y;
        String labelText = label;
        int fLeft = left;
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (screen != this) return;
            ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickDelta) ->
                    graphics.text(this.font, labelText, fLeft, labelY, GlassPanel.TEXT_DIM, false));
        });

        y += 10 + FIELD_HEIGHT + SPACING;
        return box;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

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
}
