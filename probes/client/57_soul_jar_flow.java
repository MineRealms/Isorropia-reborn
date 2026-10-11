// The soul jar, as upstream: an empty warded jar takes a creature out of the vat, the jar is named after what it
// holds, a full jar puts the creature back (and returns the warded jar), and a wand on a placed jar lets the
// creature out where the jar stood. Survival, so the jars are really spent.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
player.getInventory().clearContent();
var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
var warded = dev.tc4port.thaumcraft.api.ThaumcraftContent.item("warded_jar");
var sb = new StringBuilder();

var master = new net.minecraft.core.BlockPos(64, 300, 64);
level.setChunkForced(master.getX() >> 4, master.getZ() >> 4, true);
fr.wind_blade.isorropia.common.vat.VatStructure.form(level, master);
var vat = (fr.wind_blade.isorropia.common.tiles.TileVat) level.getBlockEntity(master);
vat.abortInfusion();
if (vat.getContained() != null) {
    vat.setContained(null);
}
var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
pig.moveTo(master.getX() + 0.5, master.getY() + 1.0, master.getZ() + 0.5, 0.0F, 0.0F);
level.addFreshEntity(pig);
vat.setContained(pig);
sb.append("vat holds ").append(vat.getContained() == null ? "nothing" : vat.getContained().getType().toShortString());

// 1. An empty warded jar in hand on the vat: the pig goes into a soul jar.
player.setItemInHand(hand, new net.minecraft.world.item.ItemStack(warded));
var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(master),
        net.minecraft.core.Direction.UP, master, false);
var result = level.getBlockState(master).use(level, player, hand, hit);
net.minecraft.world.item.ItemStack full = net.minecraft.world.item.ItemStack.EMPTY;
for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
    var stack = player.getInventory().getItem(slot);
    if (stack.is(fr.wind_blade.isorropia.common.items.ItemsIS.JAR_SOUL.get())) {
        full = stack;
    }
}
sb.append("\n1 jar from vat: use=").append(result).append(" vatEmpty=").append(vat.getContained() == null)
  .append(" soulJar=").append(full.getHoverName().getString())
  .append(" customName=").append(full.hasCustomHoverName())
  .append(" noAiInJar=").append(full.isEmpty() ? "-" : full.getTag().getCompound("EntityData").getBoolean("NoAI"))
  .append(" wardedLeft=").append(player.getInventory().countItem(warded));

// 2. The full jar back on the vat: the pig returns and a warded jar comes back.
player.setItemInHand(hand, full.copy());
player.getInventory().removeItem(full);
var result2 = level.getBlockState(master).use(level, player, hand, hit);
sb.append("\n2 jar into vat: use=").append(result2).append(" vatHolds=")
  .append(vat.getContained() == null ? "nothing" : vat.getContained().getType().toShortString())
  .append(" wardedBack=").append(player.getInventory().countItem(warded));

// 3. A placed full jar opened with a wand: the creature appears where the jar stood.
var jarPos = master.offset(4, 0, 0);
level.setBlockAndUpdate(jarPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.JAR_SOUL.get().defaultBlockState());
var jarTile = (fr.wind_blade.isorropia.common.tiles.TileJarSoul) level.getBlockEntity(jarPos);
var cow = net.minecraft.world.entity.EntityType.COW.create(level);
var cowData = new net.minecraft.nbt.CompoundTag();
cow.save(cowData);
jarTile.setEntityData(cowData);
player.setItemInHand(hand, dev.tc4port.thaumcraft.api.wand.WandApi.createFullyCharged(
        dev.tc4port.thaumcraft.api.wand.WandMaterialId.WOOD, dev.tc4port.thaumcraft.api.wand.WandMaterialId.IRON));
var jarHit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(jarPos),
        net.minecraft.core.Direction.UP, jarPos, false);
var result3 = level.getBlockState(jarPos).use(level, player, hand, jarHit);
var cows = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Cow.class,
        new net.minecraft.world.phys.AABB(jarPos).inflate(0.5));
sb.append("\n3 wand on jar: use=").append(result3).append(" jarGone=").append(level.getBlockState(jarPos).isAir())
  .append(" cowAt=").append(cows.isEmpty() ? "none" : cows.get(0).blockPosition().toShortString())
  .append(" jarPos=").append(jarPos.toShortString());
return sb.toString();
