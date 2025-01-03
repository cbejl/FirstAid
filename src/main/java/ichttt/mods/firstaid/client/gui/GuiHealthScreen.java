/*
 * FirstAid
 * Copyright (C) 2017-2024
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ichttt.mods.firstaid.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import ichttt.mods.firstaid.FirstAid;
import ichttt.mods.firstaid.FirstAidConfig;
import ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart;
import ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel;
import ichttt.mods.firstaid.api.enums.EnumPlayerPart;
import ichttt.mods.firstaid.api.healing.ItemHealing;
import ichttt.mods.firstaid.client.ClientHooks;
import ichttt.mods.firstaid.client.util.EventCalendar;
import ichttt.mods.firstaid.client.util.HealthRenderUtils;
import ichttt.mods.firstaid.common.network.MessageApplyHealingItem;
import ichttt.mods.firstaid.common.network.MessageClientRequest;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiHealthScreen extends Screen {
    public static final int xSize = 344;
    public static final int ySize = 148;
    public static final int buttonWidth = 80;
    public static final ItemStack BED_ITEMSTACK = new ItemStack(Items.RED_BED);
    private static final DecimalFormat FORMAT = new DecimalFormat("##.#");
    private static final int xHeartsOffset = 22;

    public static GuiHealthScreen INSTANCE;
    public static boolean isOpen = false;
    private static int funTicks = 0; // mod 500

    private final AbstractPlayerDamageModel damageModel;
    private final List<GuiHoldButton> holdButtons = new ArrayList<>();
    private final boolean disableButtons;

    public int guiLeft;
    public int guiTop;
    public AbstractButton cancelButton;
    private AbstractButton head, leftArm, leftLeg, leftFoot, body, rightArm, rightLeg, rightFoot;
    private InteractionHand activeHand;

    public GuiHealthScreen(AbstractPlayerDamageModel damageModel) {
        super(Component.translatable("firstaid.gui.healthscreen"));
        this.damageModel = damageModel;
        disableButtons = true;
    }

    public GuiHealthScreen(AbstractPlayerDamageModel damageModel, InteractionHand activeHand) {
        super(Component.translatable("firstaid.gui.healthscreen"));
        this.damageModel = damageModel;
        this.activeHand = activeHand;
        disableButtons = false;
    }

    public static void tickFun() {
        funTicks++;
        if (funTicks > 500) {
            funTicks = (int) (Math.random() * 100);
        }
    }

    @Override
    public void init() {
        isOpen = true;
        this.guiLeft = (this.width - xSize) / 2;
        this.guiTop = (this.height - ySize) / 2;

        head = new GuiHoldButton(1, this.guiLeft + 10, this.guiTop + 14, buttonWidth, 16, Component.translatable("firstaid.gui.head"), false);
        addRenderableWidget(head);

        leftArm = new GuiHoldButton(2, this.guiLeft + 10, this.guiTop + 40, buttonWidth, 16, Component.translatable("firstaid.gui.left_arm"), false);
        addRenderableWidget(leftArm);
        leftLeg = new GuiHoldButton(3, this.guiLeft + 10, this.guiTop + 66, buttonWidth, 16, Component.translatable("firstaid.gui.left_leg"), false);
        addRenderableWidget(leftLeg);
        leftFoot = new GuiHoldButton(4, this.guiLeft + 10, this.guiTop + 92, buttonWidth, 16, Component.translatable("firstaid.gui.left_foot"), false);
        addRenderableWidget(leftFoot);

        body = new GuiHoldButton(5, this.guiLeft + 334 - buttonWidth, this.guiTop + 14, buttonWidth, 16, Component.translatable("firstaid.gui.body"), true);
        addRenderableWidget(body);

        rightArm = new GuiHoldButton(6, this.guiLeft + 334 - buttonWidth, this.guiTop + 40, buttonWidth, 16, Component.translatable("firstaid.gui.right_arm"), true);
        addRenderableWidget(rightArm);
        rightLeg = new GuiHoldButton(7, this.guiLeft + 334 - buttonWidth, this.guiTop + 66, buttonWidth, 16, Component.translatable("firstaid.gui.right_leg"), true);
        addRenderableWidget(rightLeg);
        rightFoot = new GuiHoldButton(8, this.guiLeft + 334 - buttonWidth, this.guiTop + 92, buttonWidth, 16, Component.translatable("firstaid.gui.right_foot"), true);
        addRenderableWidget(rightFoot);

        if (disableButtons) {
            head.active = false;
            leftArm.active = false;
            leftLeg.active = false;
            leftFoot.active = false;
            body.active = false;
            rightArm.active = false;
            rightLeg.active = false;
            rightFoot.active = false;
        }

        cancelButton = Button.builder(Component.translatable(disableButtons ? "gui.done" : "gui.cancel"), button -> onClose())
                .bounds(this.width / 2 - 100, this.height - 50, 200, 20)
                .build();
        addRenderableWidget(cancelButton);

        if (this.minecraft.options.renderDebug) {
            Button refresh = Button.builder(Component.literal("resync"), button -> {
                FirstAid.NETWORKING.sendToServer(new MessageClientRequest(MessageClientRequest.Type.REQUEST_REFRESH));
                FirstAid.LOGGER.info("Requesting refresh");
                minecraft.player.displayClientMessage(Component.literal("Re-downloading health data from server..."), true);
                onClose();
            }).bounds(this.guiLeft + 218, this.guiTop + 115, 36, 20).build();
            addRenderableWidget(refresh);
        }

        holdButtons.clear();
        for (AbstractWidget button : this.getButtons()) {
            if (button instanceof GuiHoldButton holdButton) {
                int holdTime = Integer.MAX_VALUE;
                if (activeHand != null) {
                    ItemStack itemInHand = minecraft.player.getItemInHand(activeHand);
                    if (itemInHand.getItem() instanceof ItemHealing itemHealing) {
                        holdTime = itemHealing.getApplyTime(itemInHand);
                    }
                }
                holdButton.setup(holdTime);
                holdButtons.add(holdButton);
            }
        }

        super.init();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        //Setup background
        this.renderBackground(guiGraphics);
        guiGraphics.fillGradient(this.guiLeft, this.guiTop, this.guiLeft + xSize, this.guiTop + ySize, -16777216, -16777216);
        guiGraphics.blit(HealthRenderUtils.SHOW_WOUNDS_LOCATION, this.guiLeft, this.guiTop, 0, 0, 256, 148);
        guiGraphics.blit(HealthRenderUtils.SHOW_WOUNDS_LOCATION, this.guiLeft + 256, this.guiTop, 0, ySize, 88, 108);
        guiGraphics.blit(HealthRenderUtils.SHOW_WOUNDS_LOCATION, this.guiLeft + 256, this.guiTop + 108, 88, ySize, 88, 40);
        //Player
        int entityLookX = this.guiLeft + (xSize / 2) - mouseX;
        int entityLookY = this.guiTop + 20 - mouseY;
        if (EventCalendar.isGuiFun()) {
            if (EventCalendar.isHalloween()) {
                // Make it spoooky
                if ((funTicks > 250 && funTicks < 270) || (funTicks > 330 && funTicks < 340)) {
                    entityLookX = 0;
                    entityLookY = 0;
                } else if ((funTicks > 480 && funTicks < 500) || (funTicks > 340 && funTicks < 350 )) {
                    entityLookX = -entityLookX;
                    entityLookY = -entityLookY;
                }
            } else {
                entityLookX = -entityLookX;
                entityLookY = -entityLookY;
            }
        }
        InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, this.width / 2, this.height / 2 + 32, 45, entityLookX, entityLookY, minecraft.player);

        //Button
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        //Text info
        int morphineTicks = damageModel.getMorphineTicks();
        if (morphineTicks > 0)
            guiGraphics.drawCenteredString(this.minecraft.font, I18n.get("firstaid.gui.morphine_left", StringUtil.formatTickDuration(morphineTicks)), this.guiLeft + (xSize / 2), this.guiTop + ySize - (this.activeHand == null ? 21 : 29), 0xFFFFFF);
        if (this.activeHand != null)
            guiGraphics.drawCenteredString(this.minecraft.font, I18n.get("firstaid.gui.apply_hint"), this.guiLeft + (xSize / 2), this.guiTop + ySize - (morphineTicks == 0 ? 21 : 11), 0xFFFFFF);

        //Health
        drawHealth(guiGraphics, damageModel.HEAD, false, 18);
        drawHealth(guiGraphics, damageModel.LEFT_ARM, false, 44);
        drawHealth(guiGraphics, damageModel.LEFT_LEG, false, 70);
        drawHealth(guiGraphics, damageModel.LEFT_FOOT, false, 96);
        drawHealth(guiGraphics, damageModel.BODY, true, 18);
        drawHealth(guiGraphics, damageModel.RIGHT_ARM, true, 44);
        drawHealth(guiGraphics, damageModel.RIGHT_LEG, true, 70);
        drawHealth(guiGraphics, damageModel.RIGHT_FOOT, true, 96);

        //Tooltip
        guiGraphics.pose().pushPose();
        tooltipButton(guiGraphics, head, damageModel.HEAD, mouseX, mouseY);
        tooltipButton(guiGraphics, leftArm, damageModel.LEFT_ARM, mouseX, mouseY);
        tooltipButton(guiGraphics, leftLeg, damageModel.LEFT_LEG, mouseX, mouseY);
        tooltipButton(guiGraphics, leftFoot, damageModel.LEFT_FOOT, mouseX, mouseY);
        tooltipButton(guiGraphics, body, damageModel.BODY, mouseX, mouseY);
        tooltipButton(guiGraphics, rightArm, damageModel.RIGHT_ARM, mouseX, mouseY);
        tooltipButton(guiGraphics, rightLeg, damageModel.RIGHT_LEG, mouseX, mouseY);
        tooltipButton(guiGraphics, rightFoot, damageModel.RIGHT_FOOT, mouseX, mouseY);
        guiGraphics.pose().popPose();

        //Sleep info setup
        double sleepHealing = FirstAidConfig.SERVER.sleepHealPercentage.get();
        int bedX = guiLeft + 8;
        int bedY = (guiTop + ySize) - 28;

        //Sleep info icon
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        guiGraphics.renderItem(BED_ITEMSTACK, bedX, bedY);
        modelViewStack.popPose();

        //Sleep info tooltip
        if (mouseX >= bedX && mouseY >= bedY && mouseX < bedX + 16 && mouseY < bedY + 16) {
            Component s = sleepHealing == 0D ? Component.translatable("firstaid.gui.no_sleep_heal") : Component.translatable("firstaid.gui.sleep_heal_amount", FORMAT.format(sleepHealing * 100));
            guiGraphics.renderTooltip(font, s, mouseX, mouseY);
        }

        holdButtonMouseCallback(guiGraphics); //callback: check if buttons are finish
    }

    private void tooltipButton(GuiGraphics guiGraphics, AbstractButton button, AbstractDamageablePart part, int mouseX, int mouseY) {
        boolean enabled = part.activeHealer == null;
        if (!enabled && button.isHoveredOrFocused()) {
            guiGraphics.renderComponentTooltip(font, Arrays.asList(Component.literal(I18n.get("firstaid.gui.active_item") + ": " + I18n.get(part.activeHealer.stack.getDescriptionId())), Component.translatable("firstaid.gui.next_heal", Math.round((part.activeHealer.ticksPerHeal.getAsInt() - part.activeHealer.getTicksPassed()) / 20F))), mouseX, mouseY);
        }
        if (!disableButtons) button.active = enabled;
    }

    public void drawHealth(GuiGraphics guiGraphics, AbstractDamageablePart damageablePart, boolean right, int yOffset) {
        PoseStack stack = guiGraphics.pose();
        stack.pushPose();
        int xTranslation = guiLeft + (right ? getRightOffset(damageablePart) : 70 + xHeartsOffset);
        HealthRenderUtils.drawHealth(guiGraphics, this.minecraft.font, damageablePart, xTranslation, guiTop + yOffset, true);
        stack.popPose();
    }

    private static int getRightOffset(AbstractDamageablePart damageablePart) {
        if (HealthRenderUtils.drawAsString(damageablePart, true)) return 276 - 40
                - xHeartsOffset;
        return 276 - Math.min(40, HealthRenderUtils.getMaxHearts(damageablePart.getMaxHealth()) * 9 + HealthRenderUtils.getMaxHearts(damageablePart.getAbsorption()) * 9 + 2)
                - xHeartsOffset;
    }

    @Override
    public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
        if (super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_))
            return true;
        if (ClientHooks.SHOW_WOUNDS.isActiveAndMatches(InputConstants.getKey(p_keyPressed_1_, p_keyPressed_2_))) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double p_mouseReleased_1_, double p_mouseReleased_3_, int p_mouseReleased_5_) {
        holdButtonMouseCallback(null);
        return super.mouseReleased(p_mouseReleased_1_, p_mouseReleased_3_, p_mouseReleased_5_);
    }

    @Override
    public void mouseMoved(double xPos, double yPos) {
        for (GuiHoldButton holdButton : this.holdButtons) {
            holdButton.mouseMoved(xPos, yPos);
        }
    }

    protected void holdButtonMouseCallback(GuiGraphics guiGraphics) {
        for (GuiHoldButton button : this.holdButtons) {
            int timeLeft = button.getTimeLeft();
            if (timeLeft == 0) {
                //We are officially done
                button.reset();
                EnumPlayerPart playerPart = EnumPlayerPart.VALUES[button.id - 1];
                FirstAid.NETWORKING.sendToServer(new MessageApplyHealingItem(playerPart, activeHand));
                AbstractDamageablePart part = damageModel.getFromEnum(playerPart);
                ItemStack itemInHand = minecraft.player.getItemInHand(this.activeHand);
                if (itemInHand.getItem() instanceof ItemHealing itemHealing) {
                    part.activeHealer = itemHealing.createNewHealer(itemInHand);
                }
                onClose();
            } else if (guiGraphics == null) {
                button.reset();
            } else if (timeLeft != -1) {
                float timeInSecs = (timeLeft / 1000F);
                if (timeInSecs < 0F) timeInSecs = 0F;
                guiGraphics.blit(HealthRenderUtils.SHOW_WOUNDS_LOCATION, button.getX() + (button.isRightSide ? 56 + 27 : -25), button.getY() - 2, button.isRightSide ? 2 : 0, 169, 22, 24);
                guiGraphics.drawString(font, HealthRenderUtils.TEXT_FORMAT.format(timeInSecs), button.getX() + (button.isRightSide ? 60 + 27 : -20), button.getY() + 6, 0xFFFFFF);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        INSTANCE = null;
        isOpen = false;
        super.onClose();
    }

    public List<AbstractWidget> getButtons() {
        return (List<AbstractWidget>) (Object) this.renderables;
    }
}
