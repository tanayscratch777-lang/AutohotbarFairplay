package com.hapnoid.autohotbar.client.gui;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * RISK NOTE: enchantments are a "data-driven" registry, meaning the list of
 * them only exists once you're connected to a world (it comes from that
 * world's data, not a fixed built-in list like items). If this is opened
 * with no world loaded, it shows an empty list with a message instead of
 * crashing - open it from in-game (which is the only way this mod's config
 * screen is normally reached anyway) rather than the main menu.
 * Also see ItemPickerScreen's risk note - same renderItem-family caveat
 * doesn't apply here (no icons drawn), but the same "no built-in widget
 * matches this layout" reasoning applies to the row list below.
 */
public class EnchantPickerScreen extends Screen {
    private static final int ROW_HEIGHT = 20;

    private final Screen parent;
    private final BiConsumer<String, Integer> onPick;
    private final List<EnchantEntry> all = new ArrayList<>();
    private List<EnchantEntry> filtered = new ArrayList<>();

    private EditBox searchBox;
    private int listLeft, listTop, listWidth, listHeight;
    private int scrollRows = 0;
    private int hoveredIndex = -1;
    private int currentLevel = 1;

    private record EnchantEntry(String id, String displayName, int maxLevel) {
    }

    public EnchantPickerScreen(Screen parent, BiConsumer<String, Integer> onPick) {
        super(Component.literal("Pick an enchantment"));
        this.parent = parent;
        this.onPick = onPick;
        loadEnchantments();
    }

    private void loadEnchantments() {
        try {
            var access = this.minecraft.level != null ? this.minecraft.level.registryAccess()
                    : this.minecraft.getConnection() != null ? this.minecraft.getConnection().registryAccess() : null;
            if (access == null) return;
            Registry<Enchantment> registry = access.lookupOrThrow(Registries.ENCHANTMENT);
            for (Holder<Enchantment> holder : registry.listElements().toList()) {
                Enchantment ench = holder.value();
                String id = holder.unwrapKey().map(k -> k.location().toString()).orElse("unknown");
                String name = titleCase(id.contains(":") ? id.substring(id.indexOf(':') + 1) : id);
                int max;
                try {
                    max = ench.getMaxLevel();
                } catch (Throwable t) {
                    max = 5;
                }
                all.add(new EnchantEntry(id, name, max));
            }
            all.sort((a, b) -> a.displayName.compareTo(b.displayName));
        } catch (Throwable t) {
            // No world loaded yet, or this lookup path doesn't match this build -
            // leave the list empty rather than crash; see class-level risk note.
        }
    }

    private static String titleCase(String snakeCase) {
        String[] parts = snakeCase.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    @Override
    protected void init() {
        int panelLeft = this.width / 4;
        int panelTop = 20;
        int panelWidth = this.width / 2;
        int panelHeight = this.height - 40;

        searchBox = new EditBox(this.font, panelLeft + 8, panelTop + 24, panelWidth - 16, 16, Component.literal("Search"));
        searchBox.setHint(Component.literal("Search enchantments..."));
        searchBox.setResponder(q -> {
            applyFilter(q);
            scrollRows = 0;
        });
        this.addRenderableWidget(searchBox);
        this.setInitialFocus(searchBox);

        listLeft = panelLeft + 8;
        listTop = panelTop + 48;
        listWidth = panelWidth - 16;
        listHeight = panelHeight - 92;

        int footerY = panelTop + panelHeight - 40;
        this.addRenderableWidget(Button.builder(Component.literal("-"), b -> currentLevel = Math.max(1, currentLevel - 1))
                .bounds(panelLeft + 8, footerY, 20, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), b -> currentLevel = Math.min(10, currentLevel + 1))
                .bounds(panelLeft + 34, footerY, 20, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.minecraft.setScreen(parent))
                .bounds(panelLeft + panelWidth - 88, panelTop + panelHeight - 20, 80, 16).build());

        applyFilter("");

        int pLeft = panelLeft, pTop = panelTop, pW = panelWidth, pH = panelHeight, fY = footerY;
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (screen != this) return;
            ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickDelta) ->
                    draw(graphics, mouseX, mouseY, pLeft, pTop, pW, pH, fY));
        });
    }

    private void applyFilter(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        filtered = new ArrayList<>();
        for (EnchantEntry e : all) {
            if (q.isEmpty() || e.displayName.toLowerCase(Locale.ROOT).contains(q) || e.id.toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(e);
            }
        }
    }

    private void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, int panelLeft, int panelTop, int panelWidth, int panelHeight, int footerY) {
        GlassPanel.panel(g, panelLeft, panelTop, panelWidth, panelHeight);
        g.text(this.font, "Pick an enchantment", panelLeft + 8, panelTop + 8, GlassPanel.TEXT, true);
        String countText = filtered.size() + " of " + all.size();
        g.text(this.font, countText, panelLeft + panelWidth - 8 - this.font.width(countText), panelTop + 8, GlassPanel.TEXT_DIM, false);

        if (all.isEmpty()) {
            g.text(this.font, "No enchantments found - open this from in-game, not the main menu.",
                    listLeft, listTop, GlassPanel.TEXT_DIM, false);
        }

        hoveredIndex = -1;
        int rows = listHeight / ROW_HEIGHT;
        for (int row = 0; row < rows; row++) {
            int index = scrollRows + row;
            if (index >= filtered.size()) continue;
            int y = listTop + row * ROW_HEIGHT;
            boolean hovered = mouseX >= listLeft && mouseX < listLeft + listWidth && mouseY >= y && mouseY < y + ROW_HEIGHT;
            if (hovered) hoveredIndex = index;
            GlassPanel.row(g, listLeft, y, listWidth, ROW_HEIGHT, hovered);

            EnchantEntry entry = filtered.get(index);
            g.text(this.font, entry.displayName, listLeft + 4, y + 6, GlassPanel.TEXT, false);
            String maxText = "max " + entry.maxLevel;
            g.text(this.font, maxText, listLeft + listWidth - 8 - this.font.width(maxText), y + 6, GlassPanel.TEXT_DIM, false);
        }

        g.text(this.font, "Lv " + currentLevel, panelLeft + 60, footerY + 4, GlassPanel.TEXT, true);
        g.text(this.font, "Click a row to add it at this level", panelLeft + 100, footerY + 4, GlassPanel.TEXT_DIM, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoveredIndex >= 0 && hoveredIndex < filtered.size()) {
            EnchantEntry entry = filtered.get(hoveredIndex);
            onPick.accept(entry.id, Math.min(currentLevel, Math.max(1, entry.maxLevel)));
            this.minecraft.setScreen(parent);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= listLeft && mouseX < listLeft + listWidth && mouseY >= listTop && mouseY < listTop + listHeight) {
            int rows = listHeight / ROW_HEIGHT;
            int maxScroll = Math.max(0, filtered.size() - rows);
            scrollRows = Math.max(0, Math.min(maxScroll, scrollRows - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
