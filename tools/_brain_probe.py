"""Ad-hoc third-person shot helper: place the player with a camera type, then grab.

    python tools/_brain_probe.py <BACK|FRONT> <name> <x> <y> <z> <yaw> <pitch>

The camera type is applied in the place step (a frame before the grab), because Screenshot.grab
captures the already-rendered frame.
"""
import sys
import time
from pathlib import Path

import client_probe as cp

PLACE3 = """
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
mc.execute(() -> {
    mc.setScreen(null);
    mc.mouseHandler.releaseMouse();
    mc.options.hideGui = true;
    mc.options.setCameraType(net.minecraft.client.CameraType.%(camera)s);
});
player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
player.getAbilities().flying = true;
player.onUpdateAbilities();
player.connection.teleport(%(x)s, %(y)s, %(z)s, %(yaw)sF, %(pitch)sF);
var level = server.overworld();
level.getChunkSource().move(player);
level.setDayTime(6000L);
level.setWeatherParameters(6000, 0, false, false);
return "placed cam=" + mc.options.getCameraType();
"""

GRAB3 = """
var mc = net.minecraft.client.Minecraft.getInstance();
var done = new java.util.concurrent.CompletableFuture<String>();
mc.execute(() -> {
    try {
        mc.setScreen(null);
        mc.player.setPos(%(x)s, %(y)s, %(z)s);
        mc.player.xo = %(x)s; mc.player.yo = %(y)s; mc.player.zo = %(z)s;
        mc.player.setYRot(%(yaw)sF);
        mc.player.setXRot(%(pitch)sF);
        mc.player.yRotO = %(yaw)sF;
        mc.player.xRotO = %(pitch)sF;
        mc.player.setYHeadRot(%(yaw)sF);
        mc.options.hideGui = true;
        net.minecraft.client.Screenshot.grab(mc.gameDirectory, "%(name)s.png", mc.getMainRenderTarget(),
                message -> done.complete(message.getString()));
    } catch (Throwable t) {
        done.complete("FAILED " + t);
    }
});
String message = done.get(20, java.util.concurrent.TimeUnit.SECONDS);
return message + " at " + mc.player.position() + " cam=" + mc.options.getCameraType();
"""


def main():
    camera, name, x, y, z, yaw, pitch = sys.argv[1:8]
    camera = "THIRD_PERSON_" + camera.upper()
    values = dict(name=name, x=x, y=y, z=z, yaw=yaw, pitch=pitch, camera=camera)
    print(cp.exec_body(PLACE3 % values, "place"))
    time.sleep(3)
    print(cp.exec_body(GRAB3 % values, "grab"))
    print("file:", cp.RUN / "screenshots" / (name + ".png"))


if __name__ == "__main__":
    main()
