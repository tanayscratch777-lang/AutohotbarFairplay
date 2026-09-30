package com.hapnoid.autohotbar.client.gui;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * ============================================================================
 *  RISK NOTE - the riskiest single file in this mod
 * ============================================================================
 * This screen draws real item icons and handles clicks on a hand-laid-out
 * grid instead of using vanilla Button widgets for each cell (a real Button
 * per cell would draw its own button-texture border around every item,
 * which doesn't match the screenshots at all). That means it does two
 * things nothing else in this mod does:
 *   1. Calls graphics.renderItem(stack, x, y) / renderItemDecorations(...)
 *      to draw the icon itself - best-guess method names, not yet confirmed
 *      against 26.1.2 docs the way fill/outline/text were.
 *   2. Overrides mouseClicked/mouseScrolled directly (with @Override, so a
 *      wrong signature is a clean compile error, not a silently-dead
 *      method) instead of leaning on Button's built-in click handling like
 *      the rest of this mod's screens do.
 * If the build fails here, that's expected on the first pass - it's the one
 * part of this session's changes I had the least to verify against.
 * ============================================================================
 */
public class ItemPickerScreen extends Screen {
    private static final int CELL = 24;
    private static final int ICON_SIZE = 16;

    private final Screen parent;
    private final Consumer<String> onPick;

    private final List<ItemEntry> allItems = new ArrayList<>();
    private List<ItemEntry> filtered = new ArrayList<>();

    private EditBox searchBox;
    private int gridLeft, gridTop, gridWidth, gridHeight, columns;
    private int scrollRows = 0;
    private int hoveredIndex = -1;

    private record ItemEntry(String id, String displayName, ItemStack stack) {
    }

    public ItemPickerScreen(Screen parent, String initialQuery, Consumer<String> onPick) {
        super(Component.literal("Pick an item"));
        this.parent = parent;
        this.onPick = onPick;
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            if (id.equals("minecraft:air")) continue;
            String name;
            try {
                name = stack.getHoverName().getString();
            } catch (Throwable t) {
                name = id;
            }
            allItems.add(new ItemEntry(id, name, stack));
        }
        allItems.sort((a, b) -> a.id.compareTo(b.id));
    }

    @Override
    protected void init() {
        int panelLeft = this.width / 4;
        int panelTop = 20;
        int panelWidth = this.width / 2;
        int panelHeight = this.height - 40;

        searchBox = new EditBox(this.font, panelLeft + 8, panelTop + 24, panelWidth - 16, 16, Component.literal("Search"));
        searchBox.setHint(Component.literal("Search items..."));
        searchBox.setResponder(query -> {
            applyFilter(query);
            scrollRows = 0;
        });
        this.addRenderableWidget(searchBox);
        this.setInitialFocus(searchBox);

        gridLeft = panelLeft + 8;
        gridTop = panelTop + 48;
        gridWidth = panelWidth - 16;
        gridHeight = panelHeight - 68;
        columns = Math.max(1, gridWidth / CELL);

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.minecraft.setScreen(parent))
                .bounds(panelLeft + 8, panelTop + panelHeight - 20, panelWidth - 16, 16).build());

        applyFilter("");

        int pLeft = panelLeft, pTop = panelTop, pW = panelWidth, pH = panelHeight;
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (screen != this) return;
            ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickDelta) ->
                    draw(graphics, mouseX, mouseY, pLeft, pTop, pW, pH));
        });
    }

    private void applyFilter(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        filtered = new ArrayList<>();
        for (ItemEntry e : allItems) {
            if (q.isEmpty() || e.id.toLowerCase(Locale.ROOT).contains(q) || e.displayName.toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(e);
            }
        }
    }

    private void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, int panelLeft, int panelTop, int panelWidth, int panelHeight) {
        GlassPanel.panel(g, panelLeft, panelTop, panelWidth, panelHeight);
        g.text(this.font, "Pick an item", panelLeft + 8, panelTop + 8, GlassPanel.TEXT, true);
        g.text(this.font, filtered.size() + " of " + allItems.size() + " items",
                panelLeft + panelWidth - 8 - this.font.width(filtered.size() + " of " + allItems.size() + " items"),
                panelTop + 8, GlassPanel.TEXT_DIM, false);

        hoveredIndex = -1;
        int rows = (gridHeight) / CELL;
        int firstIndex = scrollRows * columns;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int index = firstIndex + row * columns + col;
                if (index >= filtered.size()) continue;
                int x = gridLeft + col * CELL;
                int y = gridTop + row * CELL;
                boolean hovered = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
                if (hovered) hoveredIndex = index;
                GlassPanel.row(g, x, y, CELL, CELL, hovered);

                ItemStack stack = filtered.get(index).stack;
                try {
                    g.renderItem(stack, x + (CELL - ICON_SIZE) / 2, y + (CELL - ICON_SIZE) / 2);
                    g.renderItemDecorations(this.font, stack, x + (CELL - ICON_SIZE) / 2, y + (CELL - ICON_SIZE) / 2);
                } catch (Throwable t) {
                    g.text(this.font, "?", x + CELL / 2 - 2, y + CELL / 2 - 4, GlassPanel.TEXT_DIM, false);
                }
            }
        }

        if (hoveredIndex >= 0 && hoveredIndex < filtered.size()) {
            String name = filtered.get(hoveredIndex).displayName;
            g.text(this.font, name, gridLeft, panelTop + panelHeight - 32, GlassPanel.TEXT, true);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoveredIndex >= 0 && hoveredIndex < filtered.size()
                && mouseX >= gridLeft && mouseX < gridLeft + gridWidth
                && mouseY >= gridTop && mouseY < gridTop + gridHeight) {
            onPick.accept(filtered.get(hoveredIndex).id);
            this.minecraft.setScreen(parent);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= gridLeft && mouseX < gridLeft + gridWidth && mouseY >= gridTop && mouseY < gridTop + gridHeight) {
            int maxRows = Math.max(0, (filtered.size() + columns - 1) / columns - gridHeight / CELL);
            scrollRows = Math.max(0, Math.min(maxRows, scrollRows - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
