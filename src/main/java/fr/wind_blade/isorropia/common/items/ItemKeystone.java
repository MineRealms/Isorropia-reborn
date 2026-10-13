package fr.wind_blade.isorropia.common.items;

import fr.wind_blade.isorropia.common.tiles.TileVortex;
import fr.wind_blade.isorropia.common.world.PocketPlane;
import fr.wind_blade.isorropia.common.world.PocketPlaneBuilder;
import fr.wind_blade.isorropia.common.world.PocketPlaneStore;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Thaumic Horizons' planar keystone, {@code ItemKeystone}: a key to one pocket plane. A blank keystone is imprinted by
 * right-clicking a rift inside a plane; once imprinted it can be slotted into a gateway receptacle, which is what lets
 * the gateway open onto that plane.
 *
 * <p>TH kept only the plane's number on the item and looked the name and colour up in its global plane list, which does
 * not exist on a client here. The imprint therefore carries the plane's name and colour along with its number, so the
 * tooltip and the tint are the same on both sides.</p>
 */
public class ItemKeystone extends Item {
    private static final String PLANE = "plane";
    private static final String NAME = "planeName";
    private static final String COLOUR = "planeColour";

    public ItemKeystone(Properties properties) {
        super(properties);
    }

    /** The plane this keystone is bound to, or -1 while it is still blank. */
    public static int boundPlane(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(PLANE) ? tag.getInt(PLANE) : -1;
    }

    /** The bound plane's colour, or white while blank. */
    public static int boundColour(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(COLOUR) ? tag.getInt(COLOUR) : 0xFFFFFF;
    }

    /** A keystone bound to one plane, as the receptacle drops when it is taken back out. */
    public static ItemStack of(int planeId, String name, int colour) {
        ItemStack stack = new ItemStack(ItemsIS.KEYSTONE.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(PLANE, planeId);
        tag.putString(NAME, name);
        tag.putInt(COLOUR, colour);
        return stack;
    }

    /** The keystone for a recorded plane, carrying the name and colour the plane was made with. */
    public static ItemStack of(PocketPlaneStore.Plane plane) {
        return of(plane.id(), plane.name(), PocketPlaneBuilder.colourOf(plane.aspects()));
    }

    /** A blank keystone, as the infusion makes. */
    public static ItemStack blank() {
        return new ItemStack(ItemsIS.KEYSTONE.get());
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return boundPlane(stack) >= 0 ? "item.isorropia.keystone_bound" : "item.isorropia.keystone_blank";
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NAME)) {
            tooltip.add(Component.literal(tag.getString(NAME)).withStyle(ChatFormatting.AQUA));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    /** TH's imprint gesture: right-click a rift inside a pocket plane with a blank keystone. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide || boundPlane(stack) >= 0 || !PocketPlane.LEVEL.equals(level.dimension())) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof TileVortex vortex) || vortex.planeId() < 0) {
            return InteractionResult.PASS;
        }
        PocketPlaneStore.Plane plane = PocketPlaneStore.get(level.getServer()).plane(vortex.planeId());
        if (plane == null) {
            return InteractionResult.PASS;
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(PLANE, plane.id());
        tag.putString(NAME, plane.name());
        tag.putInt(COLOUR, PocketPlaneBuilder.colourOf(plane.aspects()));
        return InteractionResult.SUCCESS;
    }
}
