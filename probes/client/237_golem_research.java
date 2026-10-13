// List TC4R's golem-related research ids, so the golem research can be parented onto things that exist.
var sb = new StringBuilder();
try {
    var reg = dev.tc4port.thaumcraft.api.research.ResearchApi.registry();
    var keys = new java.util.ArrayList<String>();
    for (var key : reg.entries().keySet()) {
        var s = key.serialized();
        if (s.toLowerCase().contains("golem")) {
            keys.add(s);
        }
    }
    java.util.Collections.sort(keys);
    sb.append("count=").append(keys.size()).append("\n");
    for (var k : keys) {
        sb.append("  ").append(k).append("\n");
    }
    // Also confirm the TH parents and our own chain resolve.
    for (var probe : new String[]{"GOLEMTHAUMIUM", "ADVANCEDGOLEM", "thaumcraft:GOLEMTHAUMIUM",
            "thaumcraft:ADVANCEDGOLEM", "isorropia:planarRift", "isorropia:wispSpawn"}) {
        var parsed = dev.tc4port.thaumcraft.api.research.ResearchKey.parse(probe);
        sb.append(probe).append(" -> ").append(reg.get(parsed).isPresent()).append("\n");
    }
    return sb.toString();
} catch (Throwable t) {
    var sw = new java.io.StringWriter();
    t.printStackTrace(new java.io.PrintWriter(sw));
    return "EXC: " + sw.toString();
}
