package fr.wind_blade.isorropia.common.tiles;

/** What the dynamo renderer needs from either dynamo: how its rings move, and what lightning it shows. */
public interface DynamoVisual {
    DynamoMotion motion();

    /** True while the dynamo is feeding the network; the lightning only shows then. */
    boolean supplying();

    /** The colour of the lightning between the rings, as RGB. */
    int glowColour();
}
