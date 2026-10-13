package fr.wind_blade.isorropia.common.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.function.Function;

/**
 * Thaumic Horizons' {@code GatewayTeleporter}: a teleporter that simply drops the traveller on a fixed spot facing a
 * fixed way, instead of hunting for a matching portal on the far side. Both ends of a planar gateway already know
 * exactly where their twin stands - the world-side receptacle records the plane it opens onto, and the plane-side
 * portal records the receptacle it came from - so there is nothing left to search for.
 *
 * <p>TH extended vanilla's {@code Teleporter} and overrode only {@code placeInPortal}; this is the same idea against
 * Forge's {@link ITeleporter}, which is what {@code Entity#changeDimension} expects on 1.20.1.</p>
 */
public class GatewayTeleporter implements ITeleporter {
    private final Vec3 arrival;
    private final float yaw;

    public GatewayTeleporter(Vec3 arrival, float yaw) {
        this.arrival = arrival;
        this.yaw = yaw;
    }

    @Override
    public PortalInfo getPortalInfo(Entity traveller, ServerLevel level,
            Function<ServerLevel, PortalInfo> defaultPortalInfo) {
        return new PortalInfo(this.arrival, Vec3.ZERO, this.yaw, traveller.getXRot());
    }

    @Override
    public Entity placeEntity(Entity traveller, ServerLevel oldLevel, ServerLevel newLevel, float yaw,
            Function<Boolean, Entity> repositionEntity) {
        Entity placed = repositionEntity.apply(false);
        placed.moveTo(this.arrival.x, this.arrival.y, this.arrival.z, this.yaw, traveller.getXRot());
        return placed;
    }
}
