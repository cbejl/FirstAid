package de.technikforlife.firstaid.network;

import de.technikforlife.firstaid.damagesystem.DamageablePart;
import de.technikforlife.firstaid.damagesystem.PlayerDamageModel;
import de.technikforlife.firstaid.damagesystem.capability.CapabilityExtendedHealthSystem;
import de.technikforlife.firstaid.damagesystem.enums.EnumPlayerPart;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Objects;

public class MessageReceiveDamage implements IMessage {

    private EnumPlayerPart part;
    private float damageAmount;

    public MessageReceiveDamage() {}

    public MessageReceiveDamage(EnumPlayerPart part, float damageAmount) {

        this.part = part;
        this.damageAmount = damageAmount;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        part = EnumPlayerPart.fromID(buf.readByte());
        damageAmount = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(part.id);
        buf.writeFloat(damageAmount);
    }

    public static class Handler implements IMessageHandler<MessageReceiveDamage, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(MessageReceiveDamage message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
               PlayerDamageModel damageModel = Minecraft.getMinecraft().player.getCapability(CapabilityExtendedHealthSystem.INSTANCE, null);
                Objects.requireNonNull(damageModel);
                DamageablePart part = damageModel.getFromEnum(message.part);
                part.damage(message.damageAmount);
            });
            return null;
        }
    }
}
