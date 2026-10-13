// Syringes: prove the five items and both projectile entities are registered, that both arcane recipes and both
// research entries load, and that the two runtime behaviours work server-side - a warded syringe drawing the wizard's
// own blood, and an injection handing its potion effects to a creature it is struck against.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var sb = new StringBuilder();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());

// --- registration ---
for (var id : java.util.List.of("syringe_empty", "syringe_blood", "syringe_blood_sample", "syringe_injection",
        "blast_phial")) {
    var item = net.minecraft.core.registries.BuiltInRegistries.ITEM
            .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", id));
    sb.append(id).append('=').append(item != null && item != net.minecraft.world.level.block.Blocks.AIR.asItem())
            .append('\n');
}
sb.append("syringeEntity=").append(fr.wind_blade.isorropia.common.entities.EntitiesIS.SYRINGE.isPresent()).append('\n');
sb.append("blastPhialEntity=").append(fr.wind_blade.isorropia.common.entities.EntitiesIS.BLAST_PHIAL.isPresent())
        .append('\n');

// --- recipes ---
for (var name : java.util.List.of("syringe_empty", "blast_phial")) {
    var rl = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "arcane/" + name);
    var recipe = server.getRecipeManager().byKey(rl);
    sb.append("recipe_").append(name).append('=').append(recipe.isPresent()).append('\n');
    if (recipe.isPresent()) {
        sb.append("recipeType_").append(name).append('=').append(recipe.get().getType()).append('\n');
    }
}

// --- research ---
for (var key : java.util.List.of("essentiaInjection", "blastPhial")) {
    var research = dev.tc4port.thaumcraft.research.ResearchCatalog
            .get(new dev.tc4port.thaumcraft.api.research.ResearchKey("isorropia", key));
    sb.append("research_").append(key).append('=').append(research != null).append('\n');
    if (research != null) {
        sb.append("parents_").append(key).append('=').append(research.parents()).append('\n');
        sb.append("pages_").append(key).append('=').append(research.pages().size()).append('\n');
    }
}

// --- behaviour: a warded syringe draws the wizard's own blood ---
var empty = new net.minecraft.world.item.ItemStack(fr.wind_blade.isorropia.common.items.ItemsIS.SYRINGE_EMPTY.get());
var before = player.getInventory().countItem(fr.wind_blade.isorropia.common.items.ItemsIS.SYRINGE_BLOOD.get());
var useResult = empty.use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
var after = player.getInventory().countItem(fr.wind_blade.isorropia.common.items.ItemsIS.SYRINGE_BLOOD.get());
sb.append("drewBlood=").append(after > before).append('\n');
sb.append("useResult=").append(useResult.getResult()).append('\n');

// --- behaviour: an injection hands its effects to whatever it strikes ---
var pig = new net.minecraft.world.entity.animal.Pig(net.minecraft.world.entity.EntityType.PIG, level);
pig.moveTo(player.getX() + 2.0, player.getY(), player.getZ());
level.addFreshEntity(pig);
var effects = new net.minecraft.nbt.ListTag();
var effect = new net.minecraft.nbt.CompoundTag();
effect.putString("id", "minecraft:speed");
effect.putInt("duration", 200);
effect.putInt("amplifier", 1);
effects.add(effect);
var tag = new net.minecraft.nbt.CompoundTag();
tag.put("CustomPotionEffects", effects);
tag.putInt("CustomPotionColor", 0x00FF00);
var injection = new net.minecraft.world.item.ItemStack(
        fr.wind_blade.isorropia.common.items.ItemsIS.SYRINGE_INJECTION.get());
injection.setTag(tag);
var injected = injection.hurtEnemy(pig, player);
sb.append("injectedPig=").append(pig.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED)).append('\n');
sb.append("syringeSpent=").append(injection.isEmpty()).append('\n');
sb.append("hurtEnemyResult=").append(injected).append('\n');

pig.discard();
return sb.toString();
