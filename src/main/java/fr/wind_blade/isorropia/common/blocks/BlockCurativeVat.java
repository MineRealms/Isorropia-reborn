package fr.wind_blade.isorropia.common.blocks;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Every block of a formed vat. The 1.12 metadata variants become a blockstate property: TOP holds the
 * master tile, BOTTOM the slave, CONNECTOR the essentia ports, PLACEHOLDER the walls.
 */
public class BlockCurativeVat extends Block {
    public static final EnumProperty<Type> VARIANT = EnumProperty.create("variant", Type.class);

    public BlockCurativeVat(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, Type.PLACEHOLDER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    public enum Type implements StringRepresentable {
        PLACEHOLDER("placeholder"),
        TOP("top"),
        BOTTOM("bottom"),
        CONNECTOR("connector");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
