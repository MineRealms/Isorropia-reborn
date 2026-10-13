package fr.wind_blade.isorropia.common.entities;

import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.entities.projectile.EntityEmber;
import fr.wind_blade.isorropia.common.entities.projectile.EntityIncubatedEgg;
import fr.wind_blade.isorropia.common.entities.projectile.EntityBlastPhial;
import fr.wind_blade.isorropia.common.entities.projectile.EntitySyringe;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Spider;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EntitiesIS {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Isorropia.MODID);

    public static final RegistryObject<EntityType<EntityIncubatedEgg>> INCUBATED_EGG = register("incubated_egg",
            EntityType.Builder.<EntityIncubatedEgg>of(EntityIncubatedEgg::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10));
    public static final RegistryObject<EntityType<EntityTaintPig>> TAINT_PIG = register("taint_pig",
            EntityType.Builder.of(EntityTaintPig::new, MobCategory.CREATURE).sized(0.9F, 0.9F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<EntityGravekeeper>> GRAVEKEEPER = register("gravekeeper",
            EntityType.Builder.of(EntityGravekeeper::new, MobCategory.CREATURE).sized(0.6F, 0.7F).clientTrackingRange(8));
    public static final RegistryObject<EntityType<EntitySheeder>> SHEEDER = register("sheeder",
            EntityType.Builder.of(EntitySheeder::new, MobCategory.CREATURE).sized(1.4F, 0.9F).clientTrackingRange(8));
    public static final RegistryObject<EntityType<EntityChromaticSheep>> CHROMATIC_SHEEP = register("chromatic_sheep",
            EntityType.Builder.of(EntityChromaticSheep::new, MobCategory.CREATURE).sized(0.9F, 1.3F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<EntityScholarChicken>> SCHOLAR_CHICKEN = register("scholar_chicken",
            EntityType.Builder.of(EntityScholarChicken::new, MobCategory.CREATURE).sized(0.4F, 0.7F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<EntityOrePig>> ORE_PIG = register("ore_pig",
            EntityType.Builder.of(EntityOrePig::new, MobCategory.CREATURE).sized(0.9F, 0.9F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<EntityJellyRabbit>> JELLY_RABBIT = register("jelly_rabbit",
            EntityType.Builder.of(EntityJellyRabbit::new, MobCategory.CREATURE).sized(0.4F, 0.5F).clientTrackingRange(8));
    public static final RegistryObject<EntityType<EntityHangingLabel>> HANGING_LABEL = register("hanging_label",
            EntityType.Builder.<EntityHangingLabel>of(EntityHangingLabel::new, MobCategory.MISC).sized(0.5F, 0.5F)
                    .clientTrackingRange(10).updateInterval(Integer.MAX_VALUE));
    public static final RegistryObject<EntityType<EntityChocow>> CHOCOLATE_COW = register("chocolate_cow",
            EntityType.Builder.of(EntityChocow::new, MobCategory.CREATURE).sized(0.9F, 1.4F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<EntityGoldenChicken>> GOLDEN_CHICKEN = register("golden_chicken",
            EntityType.Builder.of(EntityGoldenChicken::new, MobCategory.CREATURE).sized(0.4F, 0.7F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<EntityGuardianPanther>> GUARDIAN_PANTHER = register("guardian_panther",
            EntityType.Builder.of(EntityGuardianPanther::new, MobCategory.CREATURE).sized(1.2F, 1.6F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<EntityHellHound>> NETHER_HOUND = register("nether_hound",
            EntityType.Builder.of(EntityHellHound::new, MobCategory.CREATURE).sized(0.6F, 0.85F).fireImmune().clientTrackingRange(10));
    public static final RegistryObject<EntityType<EntityEmber>> EMBER = register("ember",
            EntityType.Builder.<EntityEmber>of(EntityEmber::new, MobCategory.MISC).sized(0.25F, 0.25F).fireImmune()
                    .clientTrackingRange(4).updateInterval(10));
    public static final RegistryObject<EntityType<EntityAnimatedBlock>> ANIMATED_BLOCK = register("animated_block",
            EntityType.Builder.of(EntityAnimatedBlock::new, MobCategory.CREATURE).sized(0.75F, 1.4F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<EntityAlchemitePrimed>> ALCHEMITE_PRIMED = register("alchemite_primed",
            EntityType.Builder.<EntityAlchemitePrimed>of(EntityAlchemitePrimed::new, MobCategory.MISC).sized(0.98F, 0.98F)
                    .clientTrackingRange(10).updateInterval(10));
    /** TH's thrown syringe, flown like an arrow. */
    public static final RegistryObject<EntityType<EntitySyringe>> SYRINGE = register("syringe",
            EntityType.Builder.<EntitySyringe>of(EntitySyringe::new, MobCategory.MISC).sized(0.5F, 0.5F)
                    .clientTrackingRange(4).updateInterval(20));
    /** TH's thrown blast phial, a splash potion in all but name. */
    public static final RegistryObject<EntityType<EntityBlastPhial>> BLAST_PHIAL = register("blast_phial",
            EntityType.Builder.<EntityBlastPhial>of(EntityBlastPhial::new, MobCategory.MISC).sized(0.5F, 0.5F)
                    .clientTrackingRange(4).updateInterval(10));
    /** TH's working golem: a block raised into a construct that keeps its maker's material. */
    public static final RegistryObject<EntityType<EntityGolemTH>> GOLEM_TH = register("golem_th",
            EntityType.Builder.of(EntityGolemTH::new, MobCategory.CREATURE).sized(0.75F, 1.4F).clientTrackingRange(10));

    private EntitiesIS() {
    }

    private static <T extends Entity> RegistryObject<EntityType<T>> register(String name, EntityType.Builder<T> builder) {
        return ENTITIES.register(name, () -> builder.build(Isorropia.MODID + ":" + name));
    }

    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TAINT_PIG.get(), Pig.createAttributes().add(Attributes.ATTACK_DAMAGE, 4.0D).build());
        event.put(GRAVEKEEPER.get(), Cat.createAttributes().build());
        event.put(SHEEDER.get(), Spider.createAttributes().build());
        event.put(CHROMATIC_SHEEP.get(), Sheep.createAttributes().build());
        event.put(SCHOLAR_CHICKEN.get(), Chicken.createAttributes().build());
        event.put(ORE_PIG.get(), Pig.createAttributes().build());
        event.put(JELLY_RABBIT.get(), Rabbit.createAttributes().build());
        event.put(CHOCOLATE_COW.get(), Cow.createAttributes().build());
        event.put(GOLDEN_CHICKEN.get(), Chicken.createAttributes().build());
        event.put(GUARDIAN_PANTHER.get(), guardianPanther());
        event.put(NETHER_HOUND.get(), Wolf.createAttributes().build());
        event.put(ANIMATED_BLOCK.get(), EntityAnimatedBlock.createAttributes().build());
        event.put(GOLEM_TH.get(), EntityGolemTH.createAttributes().build());
    }

    private static AttributeSupplier guardianPanther() {
        return Cat.createAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .build();
    }
}
