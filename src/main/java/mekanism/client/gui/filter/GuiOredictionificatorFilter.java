package mekanism.client.gui.filter;

import java.util.Collections;
import java.util.List;
import mekanism.client.gui.element.GuiTextField;
import mekanism.client.gui.element.button.MekanismImageButton;
import mekanism.client.gui.element.button.TranslationButton;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.common.Mekanism;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.tile.filter.OredictionificatorFilterContainer;
import mekanism.common.network.PacketEditFilter;
import mekanism.common.network.PacketGuiButtonPress.ClickedTileButton;
import mekanism.common.network.PacketNewFilter;
import mekanism.common.tile.machine.TileEntityOredictionificator;
import mekanism.common.tile.machine.TileEntityOredictionificator.OredictionificatorFilter;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class GuiOredictionificatorFilter extends GuiTextFilterBase<OredictionificatorFilter, TileEntityOredictionificator, OredictionificatorFilterContainer> {

    private ItemStack renderStack = ItemStack.EMPTY;

    public GuiOredictionificatorFilter(OredictionificatorFilterContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        origFilter = container.getOrigFilter();
        filter = container.getFilter();
        isNew = container.isNew();
        updateRenderStack();
    }

    @Override
    protected boolean wasTextboxKey(char c) {
        return super.wasTextboxKey(c) || c == '_' || c == ':' || c == '/';
    }

    @Override
    protected void addButtons() {
        addButton(new GuiSlot(SlotType.NORMAL, this, 44, 18));
        addButton(saveButton = new TranslationButton(this, getGuiLeft() + 31, getGuiTop() + 62, 54, 20, MekanismLang.BUTTON_SAVE, () -> {
            if (!text.getText().isEmpty()) {
                setText();
            }
            if (filter.hasFilter()) {
                if (isNew) {
                    Mekanism.packetHandler.sendToServer(new PacketNewFilter(tile.getPos(), filter));
                } else {
                    Mekanism.packetHandler.sendToServer(new PacketEditFilter(tile.getPos(), false, origFilter, filter));
                }
                sendPacketToServer(ClickedTileButton.BACK_BUTTON);
            }
        }));
        addButton(deleteButton = new TranslationButton(this, getGuiLeft() + 89, getGuiTop() + 62, 54, 20, MekanismLang.BUTTON_DELETE, () -> {
            Mekanism.packetHandler.sendToServer(new PacketEditFilter(tile.getPos(), true, origFilter, null));
            sendPacketToServer(ClickedTileButton.BACK_BUTTON);
        }));
        addButton(new MekanismImageButton(this, getGuiLeft() + 5, getGuiTop() + 5, 11, 14, getButtonLocation("back"),
              () -> sendPacketToServer(ClickedTileButton.BACK_BUTTON)));
        addButton(new MekanismImageButton(this, getGuiLeft() + 31, getGuiTop() + 21, 12, getButtonLocation("left"), () -> {
            if (filter.hasFilter()) {
                filter.previous();
                updateRenderStack();
            }
        }, getOnHover(MekanismLang.LAST_ITEM)));
        addButton(new MekanismImageButton(this, getGuiLeft() + 63, getGuiTop() + 21, 12, getButtonLocation("right"), () -> {
            if (filter.hasFilter()) {
                filter.next();
                updateRenderStack();
            }
        }, getOnHover(MekanismLang.NEXT_ITEM)));
    }

    @Override
    public void setText() {
        String newFilter = text.getText().toLowerCase();
        String modid = "forge";
        if (newFilter.contains(":")) {
            String[] split = newFilter.split(":");
            modid = split[0];
            newFilter = split[1];
        }
        List<String> possibleFilters = TileEntityOredictionificator.possibleFilters.getOrDefault(modid, Collections.emptyList());
        if (possibleFilters.stream().anyMatch(newFilter::startsWith)) {
            filter.setFilter(new ResourceLocation(modid, newFilter));
            text.setText("");
            updateRenderStack();
        }
        updateButtons();
    }

    private void updateButtons() {
        saveButton.active = filter.hasFilter();
        deleteButton.active = !isNew;
    }

    @Override
    protected GuiTextField createTextField() {
        return new GuiTextField(this, 33, 48, 109, 12);
    }

    @Override
    public void init() {
        super.init();
        updateButtons();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        drawCenteredText((isNew ? MekanismLang.FILTER_NEW : MekanismLang.FILTER_EDIT).translate(MekanismLang.FILTER), 0, getXSize(), 6, titleTextColor());
        drawString(MekanismLang.FILTER_INDEX.translate(filter.getIndex()), 79, 23, titleTextColor());
        if (filter.hasFilter()) {
            drawTextScaledBound(filter.getFilterText(), 32, 38, titleTextColor(), 111);
        }
        renderItem(renderStack, 45, 19);
        int xAxis = mouseX - getGuiLeft();
        int yAxis = mouseY - getGuiTop();
        if (text.isMouseOver(mouseX, mouseY)) {
            displayTooltip(MekanismLang.TAG_COMPAT.translate(), xAxis, yAxis);
        } else if (xAxis >= 45 && xAxis <= 61 && yAxis >= 19 && yAxis <= 35 && !renderStack.isEmpty()) {
            displayTooltip(MekanismLang.GENERIC_WITH_PARENTHESIS.translate(renderStack, renderStack.getItem().getRegistryName().getNamespace()), xAxis, yAxis);
        }
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        text.mouseClicked(mouseX, mouseY, button);
        return true;
    }

    private void updateRenderStack() {
        renderStack = filter.getResult();
    }
}