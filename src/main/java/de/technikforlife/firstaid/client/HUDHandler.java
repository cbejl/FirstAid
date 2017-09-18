package de.technikforlife.firstaid.client;

import de.technikforlife.firstaid.FirstAidConfig;
import de.technikforlife.firstaid.damagesystem.DamageablePart;
import de.technikforlife.firstaid.damagesystem.PlayerDamageModel;
import de.technikforlife.firstaid.damagesystem.capability.CapabilityExtendedHealthSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Locale;
import java.util.Objects;

@SideOnly(Side.CLIENT)
public class HUDHandler {

    public static void renderOverlay(ScaledResolution scaledResolution) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!FirstAidConfig.overlay.showOverlay || mc.player == null || GuiApplyHealthItem.isOpen || mc.player.isCreative())
            return;
        PlayerDamageModel damageModel = mc.player.getCapability(CapabilityExtendedHealthSystem.INSTANCE, null);
        Objects.requireNonNull(damageModel);
        mc.getTextureManager().bindTexture(Gui.ICONS);
        Gui gui = mc.ingameGUI;
        GlStateManager.pushMatrix();
        int xOffset = FirstAidConfig.overlay.xOffset;
        int yOffset = FirstAidConfig.overlay.yOffset;
        switch (FirstAidConfig.overlay.position) {
            case 0:
                break;
            case 1:
                xOffset += scaledResolution.getScaledWidth();
                break;
            case 2:
                yOffset += scaledResolution.getScaledHeight();
                break;
            case 3:
                xOffset += scaledResolution.getScaledWidth();
                yOffset += scaledResolution.getScaledHeight();
                break;
            default:
                throw new RuntimeException("Invalid config option for position: " + FirstAidConfig.overlay.position);
        }
        GlStateManager.translate(xOffset, yOffset, 0F);
        for (DamageablePart part : damageModel) {
            mc.fontRenderer.drawString(I18n.format("gui." + part.part.toString().toLowerCase(Locale.ENGLISH)), 0, 0, 0xFFFFFF);
            mc.getTextureManager().bindTexture(Gui.ICONS);
            RenderUtils.drawHealth(part, 60, 0, gui, false);
            GlStateManager.translate(0, 10F, 0F);
        }
        GlStateManager.popMatrix();
    }
}
