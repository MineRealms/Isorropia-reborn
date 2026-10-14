package fr.wind_blade.isorropia.common.compat.kubejs;

//? if forge {
public final class HorizonsKubePlugin extends dev.latvian.mods.kubejs.KubeJSPlugin {
    @Override
    public void registerBindings(dev.latvian.mods.kubejs.script.BindingsEvent event) {
        event.add("ThaumicHorizons", HorizonsData.class);
    }
}
//?} else {
/*public final class HorizonsKubePlugin implements dev.latvian.mods.kubejs.plugin.KubeJSPlugin {
    @Override
    public void registerBindings(dev.latvian.mods.kubejs.script.BindingRegistry event) {
        event.add("ThaumicHorizons", HorizonsData.class);
    }
}
*///?}
