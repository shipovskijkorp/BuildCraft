/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */
package buildcraft.silicon;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.fml.event.config.ModConfigEvent;

public final class BCSiliconConfig {
    public static ModConfigSpec config;

    /** Whether players and automation can create or place new facades. Existing facades remain valid. */
    public static boolean enableFacades = true;

    private static BooleanValue propEnableFacades;

    private BCSiliconConfig() {
    }

    public static void preInit() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("facades");
        propEnableFacades = builder.comment(
            "Whether BuildCraft facades are enabled.",
            "When disabled, existing facades remain loaded, but new facades cannot be crafted or placed."
        ).worldRestart()
            .define("enable", true);
        builder.pop();
        config = builder.build();
    }

    public static void onLoadConfig(ModConfigEvent.Loading event) {
        reloadConfig(event.getConfig().getModId());
    }

    public static void onReloadConfig(ModConfigEvent.Reloading event) {
        reloadConfig(event.getConfig().getModId());
    }

    public static void reloadConfig(String modId) {
        if (!BCSilicon.MODID.equals(modId)) {
            return;
        }
        enableFacades = propEnableFacades.get();
    }
}
