# Rosetta probes

Method bodies run inside the live dev server by the RosettaRemoteDebugBridge (see the memory note
`as-live-debug-bridge`). Run them in order against `./gradlew.bat runServer`:

```bash
T=$(cat run-server/RosettaRemoteDebugBridge/remote-token.txt)
python ../RainJava-work/RosettaRemoteDebugBridge/legacy/tools/rosetta_remote.py --port 48790 --token "$T" exec "$PWD/probes/server/01_environment.java"
```

| probe | checks |
|---|---|
| 01_environment | mods, registries, aspects, research, infusion data loaded |
| 02_grant_and_form_vat | research grant through the admin context; builds and forms a vat at a fixed spot (`run-server/rosetta_probes/vat_origin.txt`) |
| 03_curative_wired | jar → tube → tube → vat plumbing, a hurt pig inside |
| 04_curative_check | run ~30 s after 03: the pig is healed from the jar's victus |
| 05_vat_state | snapshot of the vat |
| 06_creature_infusion | pedestals + pig + essentia → awakened blood infusion applied, pedestals consumed |

Notes learned the hard way: a warded jar only connects through its top face, and a tube serves but never
pushes, so the consumer has to pull; force-load the chunk or nothing ticks.
