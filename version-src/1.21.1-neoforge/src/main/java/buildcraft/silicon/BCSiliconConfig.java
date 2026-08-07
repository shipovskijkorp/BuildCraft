/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.fml.event.config.ModConfigEvent;

public class BCSiliconConfig {

    public static ModConfigSpec config;

    public static boolean renderLaserBeams = true;

    private static BooleanValue propRenderLaserBeams;

    public static void preInit() {

        ModConfigSpec.Builder con_builder = new ModConfigSpec.Builder();

        con_builder.push("display");
        propRenderLaserBeams = con_builder.comment("When false laser beams will not be visible while transmitting power without wearing Goggles")
                .define("renderLaserBeams", true);
        con_builder.pop();
        config = con_builder.build();

    }

    public static void onReloadConfig(final ModConfigEvent.Reloading restarted) {
        reloadConfig(restarted.getConfig().getModId());
    }

    public static void onLoadConfig(final ModConfigEvent.Loading load) {
        reloadConfig(load.getConfig().getModId());
    }

    public static void reloadConfig(String modid) {
        if(!modid.equals(BCSilicon.MODID))return ;
        renderLaserBeams = propRenderLaserBeams.get();
    }
}
