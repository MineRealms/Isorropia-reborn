package fr.wind_blade.isorropia.common.entities;

import dev.tc4port.thaumcraft.api.ThaumcraftContent;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.essentia.EssentiaContainerApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
//? if forge {
import net.minecraftforge.network.NetworkHooks;
//?}
import org.jetbrains.annotations.Nullable;

/** A Thaumcraft jar label stuck to the side of any block, showing its aspect. */
public class EntityHangingLabel extends HangingEntity implements IEntityAdditionalSpawnData {
    @Nullable
    private AspectId aspect;

    public EntityHangingLabel(EntityType<? extends EntityHangingLabel> type, Level level) {
        super(type, level);
    }

    public EntityHangingLabel(Level level, BlockPos pos, Direction facing, @Nullable AspectId aspect) {
        super(EntitiesIS.HANGING_LABEL.get(), level, pos);
        this.aspect = aspect;
        this.setDirection(facing);
    }

    // A label has no synced state of its own; 1.21's HangingEntity no longer supplies the empty definition.
    //? if neoforge {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }
    *///?}

    @Nullable
    public AspectId getAspect() {
        return this.aspect;
    }

    // 1.21 replaced the getWidth/getHeight hooks with one abstract bounding-box method.
    //? if forge {
    @Override
    public int getWidth() {
        return 16;
    }

    @Override
    public int getHeight() {
        return 16;
    }
    //?} else {
    /*@Override
    protected net.minecraft.world.phys.AABB calculateBoundingBox(net.minecraft.core.BlockPos pos, Direction direction) {
        // 16 px square: the same maths as a 1x1 painting.
        net.minecraft.world.phys.Vec3 anchor = net.minecraft.world.phys.Vec3.atCenterOf(pos).relative(direction, -0.46875);
        Direction.Axis axis = direction.getAxis();
        double across = axis == Direction.Axis.X ? 0.0625 : 1.0;
        double along = axis == Direction.Axis.Z ? 0.0625 : 1.0;
        return net.minecraft.world.phys.AABB.ofSize(anchor, across, 1.0, along);
    }
    *///?}

    public static ItemStack labelStack(@Nullable AspectId aspect) {
        ItemStack label = new ItemStack(ThaumcraftContent.item("jar_label"));
        return aspect == null ? label : EssentiaContainerApi.withFilter(label, aspect);
    }

    @Override
    public void dropItem(@Nullable Entity breaker) {
        if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.playPlacementSound();
            this.spawnAtLocation(labelStack(this.aspect));
        }
    }

    @Override
    public void playPlacementSound() {
        BlockPos support = this.pos.relative(this.direction.getOpposite());
        BlockState state = this.level().getBlockState(support);
        this.playSound(state.getSoundType(this.level(), support, this).getHitSound(), 1.5F, 1.0F);
    }

    @Override
    public ItemStack getPickResult() {
        return labelStack(this.aspect);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 16.0D * 64.0D * getViewScale();
        return distance < range * range;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Facing", (byte) this.direction.get2DDataValue());
        if (this.aspect != null) {
            tag.putString("Aspect", this.aspect.toString());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.aspect = parse(tag.getString("Aspect"));
        this.setDirection(Direction.from2DDataValue(tag.getByte("Facing")));
    }

    //? if forge {
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    //?}
    // NeoForge sends the complex-spawn payload for IEntityWithComplexSpawn itself; the
    // payload methods take a RegistryFriendlyByteBuf there.
    //? if forge {
    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeByte(this.direction.get2DDataValue());
        buffer.writeUtf(this.aspect == null ? "" : this.aspect.toString());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        Direction facing = Direction.from2DDataValue(buffer.readByte());
        this.aspect = parse(buffer.readUtf());
        this.setDirection(facing);
    }
    //?} else {
    /*@Override
    public void writeSpawnData(net.minecraft.network.RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeByte(this.direction.get2DDataValue());
        buffer.writeUtf(this.aspect == null ? "" : this.aspect.toString());
    }

    @Override
    public void readSpawnData(net.minecraft.network.RegistryFriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        Direction facing = Direction.from2DDataValue(buffer.readByte());
        this.aspect = parse(buffer.readUtf());
        this.setDirection(facing);
    }
    *///?}

    @Nullable
    private static AspectId parse(String value) {
        if (value.isEmpty()) {
            return null;
        }
        try {
            return AspectId.parse(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
