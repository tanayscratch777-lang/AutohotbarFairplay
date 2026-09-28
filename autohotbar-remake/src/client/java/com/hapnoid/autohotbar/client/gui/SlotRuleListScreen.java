package com.hapnoid.autohotbar.client.gui;

import com.hapnoid.autohotbar.config.ConfigManager;
import com.hapnoid.autohotbar.config.SlotConfig;
import com.hapnoid.autohotbar.rule.Rule;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class SlotRuleListScreen extends Screen {
    private final Screen parent;
    private final int slotNumber;

    public SlotRuleListScreen(Screen parent, int slotNumber) {
        super(Component.literal("Slot " + slotNumber));
        this.parent = parent;
        this.slotNumber = slotNumber;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int width = 260;
        int height = 20;
        int spacing = 4;
        int y = 20;

        SlotConfig slotConfig = ConfigManager.get().slot(slotNumber);
        List<Rule> rules = slotConfig.rulesByPriority();

        for (Rule rule : rules) {
            String label = "#" + rule.priority + "  " + rule.kind + summarize(rule);
            this.addRenderableWidget(Button.builder(Component.literal(label), button ->
                    this.minecraft.setScreen(new RuleEditScreen(this, slotNumber, rule))
            ).bounds(centerX - width / 2, y, width - 24, height).build());

            this.addRenderableWidget(Button.builder(Component.literal("X"), button -> {
                slotConfig.rules.remove(rule);
                this.minecraft.setScreen(new SlotRuleListScreen(parent, slotNumber));
            }).bounds(centerX + width / 2 - 20, y, 20, height).build());

            y += height + spacing;
        }

        this.addRenderableWidget(Button.builder(Component.literal("+ Add rule"), button -> {
            int nextPriority = rules.isEmpty() ? 1 : rules.get(rules.size() - 1).priority + 1;
            Rule newRule = Rule.empty(nextPriority);
            slotConfig.rules.add(newRule);
            this.minecraft.setScreen(new RuleEditScreen(this, slotNumber, newRule));
        }).bounds(centerX - width / 2, y, width, height).build());

        y += height + spacing * 3;
        this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
            ConfigManager.save();
            this.minecraft.setScreen(parent);
        }).bounds(centerX - width / 2, y, width, height).build());
    }

    private static String summarize(Rule rule) {
        return switch (rule.kind) {
            case SPECIFIC -> ": " + (rule.specificItemId == null ? "(no item)" : rule.specificItemId);
            case TYPE -> ": " + rule.category + " / " + rule.variant;
            case CONDITIONAL -> ": if slot " + rule.ifSlot + " rule " + rule.ifRule;
            case EMPTY -> "";
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
