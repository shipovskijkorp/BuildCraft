/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */
package buildcraft.builders;

import net.minecraft.util.Mth;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class BCBuildersConfig {

    public static ForgeConfigSpec config;

    /** The minimum height that all quarry frames must be. */
    public static int quarryFrameMinHeight = 4;

    public static int quarryMaxTasksPerTick = 4;
    public static int quarryTaskPowerDivisor = 2;
    public static double quarryMaxFrameMoveSpeed;
    public static double quarryMaxBlockMineRate;

    private static IntValue propQuarryFrameMinHeight;
    private static IntValue propQuarryMaxTasksPerTick;
    private static IntValue propQuarryPowerDivisor;
    private static DoubleValue propQuarryMaxFrameSpeed;
    private static DoubleValue propQuarryMaxBlockMineRate;

    public static void preInit() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general");
        propQuarryFrameMinHeight = builder.comment("The minimum height that all quarry frames must be.")
            .defineInRange("quarryFrameMinHeight", 4, 1, 512);

        propQuarryMaxTasksPerTick = builder.comment(
            "The maximum number of tasks that the quarry will do per tick.",
            "A task is either breaking a block or moving the frame."
        ).defineInRange("quarryMaxTasksPerTick", 4, 1, 20);

        propQuarryPowerDivisor = builder.comment(
            "1 divided by this value is added to the power cost for each additional task done per tick.",
            "A value of 0 disables this behaviour."
        ).defineInRange("quarryPowerDivisor", 2, 0, 100);

        propQuarryMaxFrameSpeed = builder.comment(
            "The maximum number of blocks that a quarry is allowed to move per second.",
            "A value of 0 means no limit."
        ).defineInRange("quarryMaxFrameSpeed", 0.0, 0.0, 5120.0);

        propQuarryMaxBlockMineRate = builder.comment(
            "The maximum number of blocks that a quarry is allowed to mine each second.",
            "A value of 0 means no limit, and a value of 0.5 mines up to half a block per second."
        ).defineInRange("quarryMaxBlockMineRate", 0.0, 0.0, 1000.0);
        builder.pop();

        config = builder.build();
    }

    public static void onReloadConfig(ModConfigEvent.Reloading event) {
        reloadConfig(event.getConfig().getModId());
    }

    public static void onLoadConfig(ModConfigEvent.Loading event) {
        reloadConfig(event.getConfig().getModId());
    }

    public static void reloadConfig(String modId) {
        if (!BCBuilders.MODID.equals(modId)) {
            return;
        }
        quarryFrameMinHeight = propQuarryFrameMinHeight.get();
        quarryMaxTasksPerTick = Mth.clamp(propQuarryMaxTasksPerTick.get(), 0, 20);
        quarryTaskPowerDivisor = Mth.clamp(propQuarryPowerDivisor.get(), 0, 100);
        quarryMaxFrameMoveSpeed = Mth.clamp(propQuarryMaxFrameSpeed.get(), 0, 5120.0);
        quarryMaxBlockMineRate = Mth.clamp(propQuarryMaxBlockMineRate.get(), 0, 1000.0);
    }
}
