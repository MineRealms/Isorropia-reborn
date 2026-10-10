package fr.wind_blade.isorropia.client.model;

import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectDefinition;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import fr.wind_blade.isorropia.common.IsorropiaAspects;
import fr.wind_blade.isorropia.common.entities.EntityJellyRabbit;
import org.jetbrains.annotations.Nullable;

/** Jelly tint: the aspect colour, falling back to sensus (as upstream) and then white. */
public final class JellyColors {
    public static final float ALPHA = 0.7F;

    private JellyColors() {
    }

    public static int of(EntityJellyRabbit rabbit) {
        int color = rabbit.getAspectColor();
        return color >= 0 ? color : of((AspectId) null);
    }

    public static int of(@Nullable AspectId aspect) {
        AspectDefinition definition = AspectApi.get(aspect != null ? aspect : IsorropiaAspects.SENSUS);
        if (definition == null && aspect != null) {
            definition = AspectApi.get(IsorropiaAspects.SENSUS);
        }
        return definition == null ? 0xFFFFFF : definition.color();
    }
}
