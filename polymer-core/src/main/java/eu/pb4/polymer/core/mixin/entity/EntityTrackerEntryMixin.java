package eu.pb4.polymer.core.mixin.entity;

import com.mojang.datafixers.util.Pair;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.impl.interfaces.EntityAttachedPacket;
import eu.pb4.polymer.core.impl.interfaces.EntityTrackerUpdateS2CPacketExt;
import eu.pb4.polymer.core.impl.interfaces.MetaConsumer;
import eu.pb4.polymer.core.impl.networking.PolymerServerProtocol;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.EntityTrackingListener;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Mixin(EntityTrackerEntry.class)
public abstract class EntityTrackerEntryMixin {
    @Shadow @Final private Entity entity;
    @Shadow @Final private Consumer<Packet<?>> receiver;

    @Shadow @Nullable private List<DataTracker.SerializedEntry<?>> changedEntries;

    @ModifyVariable(method = "sendPackets", at = @At("HEAD"))
    private Consumer<Packet<?>> polymer$packetWrap(Consumer<Packet<?>> packetConsumer) {
        if (this.entity instanceof PolymerEntity polymerEntity) {
            return (packet) -> polymerEntity.onEntityPacketSent(packetConsumer, EntityAttachedPacket.setIfEmpty(packet, this.entity));
        } else {
            return (packet) -> packetConsumer.accept(EntityAttachedPacket.setIfEmpty(packet, this.entity));
        }
    }

    @ModifyArg(method = "sendPackets", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", ordinal = 1))
    private Object polymer$markAsInitial(Object obj) {
        ((EntityTrackerUpdateS2CPacketExt) obj).polymer$setInitial();
        return obj;
    }

    @Inject(method = "sendPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;createSpawnPacket()Lnet/minecraft/network/packet/Packet;"))
    private void polymer$sendPacketsBeforeSpawning(ServerPlayerEntity player, Consumer<Packet<?>> sender, CallbackInfo ci) {
        if (this.entity instanceof PolymerEntity virtualEntity) {
            try {
                virtualEntity.onBeforeSpawnPacket(player, sender);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Inject(method = "startTracking", at = @At("TAIL"))
    private void polymer$sendEntityInfo(ServerPlayerEntity player, CallbackInfo ci) {
        if (this.entity instanceof PolymerEntity polymerEntity && polymerEntity.canSynchronizeToPolymerClient(player)) {
            PolymerServerProtocol.sendEntityInfo(player.networkHandler, this.entity);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void polymer$tickHead(CallbackInfo ci) {
        if (this.entity instanceof PolymerEntity polymerEntity && this.receiver instanceof MetaConsumer receiver) {
            polymerEntity.beforeEntityTrackerTick(Collections.unmodifiableSet((Set<EntityTrackingListener>) receiver.getAttached()));
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void polymer$tick(CallbackInfo ci) {
        if (this.entity instanceof PolymerEntity polymerEntity && this.receiver instanceof MetaConsumer receiver) {
            polymerEntity.onEntityTrackerTick(Collections.unmodifiableSet((Set<EntityTrackingListener>) receiver.getAttached()));
        }
    }

    @Inject(method = "sendPackets", at = @At("TAIL"))
    private void polymer$modifyCreationData(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> sender, CallbackInfo ci) {
        if (this.entity instanceof PolymerEntity polymerEntity) {
            if (polymerEntity.sendEmptyTrackerUpdates(player) && this.changedEntries == null) {
                var x = new EntityTrackerUpdateS2CPacket(this.entity.getId(), List.of());
                ((EntityTrackerUpdateS2CPacketExt) (Object) x).polymer$setInitial();
                sender.accept(x);
            }

            try {
                if (this.entity instanceof LivingEntity livingEntity) {
                    var list = new ArrayList<Pair<EquipmentSlot, ItemStack>>();

                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        ItemStack stack = livingEntity.getEquippedStack(slot);
                        if (!stack.isEmpty()) {
                            list.add(new Pair<>(slot, stack));
                        }
                    }

                    sender.accept(new EntityEquipmentUpdateS2CPacket(this.entity.getId(), list));
                } else {
                    sender.accept(new EntityEquipmentUpdateS2CPacket(this.entity.getId(), new ArrayList<>()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
