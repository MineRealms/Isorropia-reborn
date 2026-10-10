"""Drive the dev client for render checks: launch it into a probe world, run Rosetta probes, take screenshots.

    python tools/client_probe.py start            # copy the probe world if needed, launch, wait for the bridge
    python tools/client_probe.py exec <probe.java>
    python tools/client_probe.py shot <name> <x> <y> <z> <yaw> <pitch> [settle_seconds]
    python tools/client_probe.py stop

Screenshots land in run-client/screenshots/<name>.png. The bridge only starts once a world is joined
(ServerStartedEvent on the integrated server), which is why the client quick-plays a world.
"""
import json
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RUN = ROOT / "run-client"
WORLD = "iso-probe"
PORT = 48791
LOG = ROOT / "build" / "client-run.log"
BRIDGE = ROOT.parent / "RainJava-work" / "RosettaRemoteDebugBridge"
REMOTE = BRIDGE / "legacy" / "tools" / "rosetta_remote.py"


def token():
    return (RUN / "RosettaRemoteDebugBridge" / "remote-token.txt").read_text().strip()


def exec_body(body, name="probe"):
    path = ROOT / "build" / ("client_" + name + ".java")
    path.write_text(body, encoding="utf-8")
    return exec_file(path)


def exec_file(path):
    out = subprocess.run([sys.executable, str(REMOTE), "--port", str(PORT), "--token", token(), "exec", str(path)],
                         capture_output=True, text=True, encoding="utf-8", errors="replace")
    text = out.stdout.strip() or out.stderr.strip()
    try:
        return json.loads(text).get("result", text)
    except ValueError:
        return text


def start():
    saves = RUN / "saves"
    saves.mkdir(parents=True, exist_ok=True)
    target = saves / WORLD
    if not target.exists():
        source = ROOT / "run-server" / "world"
        shutil.copytree(source, target, ignore=shutil.ignore_patterns("session.lock"))
    mods = RUN / "mods"
    mods.mkdir(parents=True, exist_ok=True)
    if not (mods / "rosetta_bridge_dev.jar").exists():
        shutil.copyfile(ROOT / "run-server" / "mods" / "rosetta_bridge_dev.jar", mods / "rosetta_bridge_dev.jar")
    options = RUN / "options.txt"
    if not options.exists():
        # No tutorial toasts, no narrator prompt, windowed and not paused when unfocused.
        options.write_text("tutorialStep:none\nnarrator:0\npauseOnLostFocus:false\nfullscreen:false\n"
                           "renderDistance:6\nsimulationDistance:6\nguiScale:2\n", encoding="utf-8")
    token_file = RUN / "RosettaRemoteDebugBridge" / "remote-token.txt"
    started = time.time()
    LOG.parent.mkdir(parents=True, exist_ok=True)
    with open(LOG, "w", encoding="utf-8", errors="replace") as log:
        subprocess.Popen(["cmd", "/c", str(ROOT / "gradlew.bat"), "runClient", "-PquickPlay=" + WORLD, "--console=plain"],
                         cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
    print("client launching, log:", LOG, flush=True)
    while time.time() - started < 540:
        time.sleep(5)
        text = LOG.read_text(encoding="utf-8", errors="replace")
        if "Remote bridge listening on" in text or ("Remote bridge" in text and "listening" in text):
            print("BRIDGE_UP after %ds" % (time.time() - started))
            return 0
        if "BUILD FAILED" in text or "Crash report saved" in text or "---- Minecraft Crash Report ----" in text:
            print("CLIENT_FAILED; see", LOG)
            return 1
    print("TIMEOUT_NO_BRIDGE; see", LOG)
    return 1


# Probe bodies run on the integrated server thread: position the player directly, then return so the
# server can tick and stream chunks while the caller waits; the grab happens in a second call.
PLACE = """
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
mc.execute(() -> {
    mc.setScreen(null);
    mc.mouseHandler.releaseMouse();
    mc.options.hideGui = %(hideGui)s;
});
player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
player.getAbilities().flying = true;
player.onUpdateAbilities();
player.connection.teleport(%(x)s, %(y)s, %(z)s, %(yaw)sF, %(pitch)sF);
var level = server.overworld();
// A teleport alone does not move the player's chunk view until the client next reports a position.
level.getChunkSource().move(player);
level.setDayTime(6000L);
level.setWeatherParameters(6000, 0, false, false);
return "placed";
"""

GRAB = """
var mc = net.minecraft.client.Minecraft.getInstance();
var done = new java.util.concurrent.CompletableFuture<String>();
mc.execute(() -> {
    try {
        mc.setScreen(null);
        // Place the camera itself for the frame: whatever the server thinks, the picture is taken here.
        mc.player.setPos(%(x)s, %(y)s, %(z)s);
        mc.player.xo = %(x)s; mc.player.yo = %(y)s; mc.player.zo = %(z)s;
        mc.player.setYRot(%(yaw)sF);
        mc.player.setXRot(%(pitch)sF);
        mc.player.yRotO = %(yaw)sF;
        mc.player.xRotO = %(pitch)sF;
        mc.player.setYHeadRot(%(yaw)sF);
        mc.options.hideGui = %(hideGui)s;
        net.minecraft.client.Screenshot.grab(mc.gameDirectory, "%(name)s.png", mc.getMainRenderTarget(),
                message -> done.complete(message.getString()));
    } catch (Throwable t) {
        done.complete("FAILED " + t);
    }
});
String message = done.get(20, java.util.concurrent.TimeUnit.SECONDS);
return message + " at " + mc.player.position() + " yaw=" + mc.player.getYRot() + " pitch=" + mc.player.getXRot();
"""


def shot(name, x, y, z, yaw, pitch, settle="3", gui=False):
    # Interface shots need the HUD left alone, world shots want it out of the way.
    values = dict(name=name, x=x, y=y, z=z, yaw=yaw, pitch=pitch, hideGui="false" if gui else "true")
    print(exec_body(PLACE % values, "place"))
    time.sleep(float(settle))
    # Rotation is applied again on the render thread right before the grab, so mouse drift cannot move it.
    print(exec_body(GRAB % values, "grab"))
    print("file:", RUN / "screenshots" / (name + ".png"))


STOP = """
var mc = net.minecraft.client.Minecraft.getInstance();
mc.execute(() -> { try { mc.stop(); } catch (Throwable t) { } });
return "shutdown scheduled";
"""


def main():
    command = sys.argv[1] if len(sys.argv) > 1 else "help"
    if command == "start":
        sys.exit(start())
    if command == "exec":
        print(exec_file(Path(sys.argv[2]).resolve()))
    elif command == "shot":
        arguments = sys.argv[2:]
        gui = "--gui" in arguments
        shot(*[a for a in arguments if a != "--gui"], gui=gui)
    elif command == "stop":
        print(exec_body(STOP, "stop"))
    else:
        print(__doc__)


if __name__ == "__main__":
    main()
