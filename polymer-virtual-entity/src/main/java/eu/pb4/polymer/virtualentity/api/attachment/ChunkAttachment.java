package eu.pb4.polymer.virtualentity.api.attachment;

import eu.pb4.polymer.common.impl.CommonImpl;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.impl.HolderAttachmentHolder;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("ClassCanBeRecord")
public class ChunkAttachment implements HolderAttachment {
    private final ElementHolder holder;
    private final WorldChunk chunk;
    private final Vec3d pos;
    private final boolean autoTick;

    public ChunkAttachment(ElementHolder holder, WorldChunk chunk, Vec3d position, boolean autoTick) {
        this.chunk = chunk;
        this.pos = position;
        this.holder = holder;
        this.attach();
        this.autoTick = autoTick;
    }

    protected void attach() {
        ((HolderAttachmentHolder) chunk).polymerVE$addHolder(this);
        this.holder.setAttachment(this);
    }

    public static HolderAttachment of(ElementHolder holder, ServerWorld world, BlockPos pos) {
        return of(holder, world, Vec3d.ofCenter(pos));
    }

    public static HolderAttachment ofTicking(ElementHolder holder, ServerWorld world, BlockPos pos) {
        return ofTicking(holder, world, Vec3d.ofCenter(pos));
    }

    public static HolderAttachment of(ElementHolder holder, ServerWorld world, Vec3d pos) {
        var chunk = world.getChunk(BlockPos.ofFloored(pos));

        if (chunk instanceof WorldChunk chunk1) {
            return new ChunkAttachment(holder, chunk1, pos, false);
        } else {
            CommonImpl.LOGGER.warn("Some mod tried to attach to chunk at " + BlockPos.ofFloored(pos).toShortString() + ", but it isn't loaded!", new NullPointerException());
            return new ManualAttachment(holder, world, () -> pos);
        }
    }

    public static HolderAttachment ofTicking(ElementHolder holder, ServerWorld world, Vec3d pos) {
        var chunk = world.getChunk(BlockPos.ofFloored(pos));

        if (chunk instanceof WorldChunk chunk1) {
            return new ChunkAttachment(holder, chunk1, pos, true);
        } else {
            CommonImpl.LOGGER.warn("Some mod tried to attach to chunk at " + BlockPos.ofFloored(pos).toShortString() + ", but it isn't loaded!", new NullPointerException());
            return new ManualAttachment(holder, world, () -> pos);
        }
    }

    @Override
    public ElementHolder holder() {
        return this.holder;
    }

    @Override
    public void destroy() {
        if (this.holder.getAttachment() == this) {
            this.holder.setAttachment(null);
        }
        ((HolderAttachmentHolder) chunk).polymerVE$removeHolder(this);
    }

    @Override
    public void tick() {
        if (this.autoTick) {
            this.holder().tick();
        }
    }

    @Override
    public boolean shouldTick() {
        return this.autoTick;
    }

    @Override
    public void updateCurrentlyTracking(Collection<ServerPlayNetworkHandler> currentlyTracking) {
        List<ServerPlayNetworkHandler> watching = new ArrayList<>();
        for (ServerPlayerEntity x : ((ServerChunkManager) this.chunk.getWorld().getChunkManager()).threadedAnvilChunkStorage.getPlayersWatchingChunk(this.chunk.getPos(), false)) {
            ServerPlayNetworkHandler networkHandler = x.networkHandler;
            watching.add(networkHandler);
        }

        for (var player : currentlyTracking) {
            if (!watching.contains(player)) {
                this.holder.stopWatching(player);
            }
        }

        for (var x : watching) {
            this.holder.startWatching(x.getPlayer().networkHandler);
        }
    }

    @Override
    public void updateTracking(ServerPlayNetworkHandler tracking) {
        //System.out.println("UPDATE-WATCHING: " + this.chunk.getWorld().getRegistryKey().getValue() + " | " + this.pos + " | " + tracking.player.isDead() + " | " + StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
        if (tracking.player.isDead() || !VirtualEntityUtils.isPlayerTracking(tracking.getPlayer(), this.chunk)) {
            VirtualEntityUtils.wrapCallWithContext(this.getWorld(), () -> this.stopWatching(tracking));
        }
    }
    /*

       @Override
    public void startWatching(ServerPlayNetworkHandler tracking) {
        System.out.println("START-WATCHING: " + this.chunk.getWorld().getRegistryKey().getValue() + " | " + this.pos + " | " + tracking.player.isDead() + " | " + StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
        HolderAttachment.super.startWatching(tracking);
    }

    @Override
    public void stopWatching(ServerPlayNetworkHandler handler) {
        System.out.println("STOP-WATCHING: " + this.chunk.getWorld().getRegistryKey().getValue() + " | " + this.pos + " | " + handler.player.isDead() + " | " + StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
        HolderAttachment.super.stopWatching(handler);
    }*/

    @Override
    public Vec3d getPos() {
        return this.pos;
    }

    @Override
    public ServerWorld getWorld() {
        return (ServerWorld) this.chunk.getWorld();
    }
}
