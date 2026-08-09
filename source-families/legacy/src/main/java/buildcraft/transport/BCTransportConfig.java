/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */
package buildcraft.transport;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.transport.pipe.EnumPipeColourType;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeApi.PowerTransferInfo;
import buildcraft.api.transport.pipe.PipeDefinition;
import buildcraft.lib.misc.MathUtil;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class BCTransportConfig {

    public static ForgeConfigSpec config;

    private static final long MJ_REQ_MILLIBUCKET_MIN = 100;
    private static final long MJ_REQ_ITEM_MIN = 50_000;

    public static long mjPerMillibucket = 1_000;
    public static long mjPerItem = MjAPI.MJ;
    public static int baseFlowRate = 10;
    public static boolean fluidPipeColourBorder;

    private static IntValue propMjPerMillibucket;
    private static IntValue propMjPerItem;
    private static IntValue propBaseFlowRate;
    private static BooleanValue propFluidPipeColourBorder;

    public static void preInit() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general");
        propMjPerMillibucket = builder.worldRestart()
            .defineInRange("pipes.mjPerMillibucket", (int) mjPerMillibucket, (int) MJ_REQ_MILLIBUCKET_MIN, Integer.MAX_VALUE);

        propMjPerItem = builder.worldRestart()
            .defineInRange("pipes.mjPerItem", (int) mjPerItem, (int) MJ_REQ_ITEM_MIN, Integer.MAX_VALUE);

        propBaseFlowRate = builder.worldRestart()
            .defineInRange("pipes.baseFluidRate", baseFlowRate, 1, 40);
        builder.pop();

        builder.push("display");
        propFluidPipeColourBorder = builder.worldRestart()
            .define("pipes.fluidColourIsBorder", true);
        builder.pop();

        config = builder.build();
    }

    public static void reloadConfig() {
        mjPerMillibucket = Math.max(MJ_REQ_MILLIBUCKET_MIN, propMjPerMillibucket.get());
        mjPerItem = Math.max(MJ_REQ_ITEM_MIN, propMjPerItem.get());
        baseFlowRate = MathUtil.clamp(propBaseFlowRate.get(), 1, 40);
        int basePowerRate = 4;

        fluidPipeColourBorder = propFluidPipeColourBorder.get();
        PipeApi.flowFluids.fallbackColourType = fluidPipeColourBorder
            ? EnumPipeColourType.BORDER_INNER
            : EnumPipeColourType.TRANSLUCENT;

        fluidTransfer(BCTransportPipes.cobbleFluid, baseFlowRate, 10);
        fluidTransfer(BCTransportPipes.woodFluid, baseFlowRate, 10);

        fluidTransfer(BCTransportPipes.stoneFluid, baseFlowRate * 2, 10);
        fluidTransfer(BCTransportPipes.sandstoneFluid, baseFlowRate * 2, 10);

        fluidTransfer(BCTransportPipes.clayFluid, baseFlowRate * 4, 10);
        fluidTransfer(BCTransportPipes.ironFluid, baseFlowRate * 4, 10);
        fluidTransfer(BCTransportPipes.quartzFluid, baseFlowRate * 4, 10);

        fluidTransfer(BCTransportPipes.diamondFluid, baseFlowRate * 8, 10);
        fluidTransfer(BCTransportPipes.diaWoodFluid, baseFlowRate * 8, 10);
        fluidTransfer(BCTransportPipes.goldFluid, baseFlowRate * 8, 2);
        fluidTransfer(BCTransportPipes.voidFluid, baseFlowRate * 8, 10);

        powerTransfer(BCTransportPipes.cobblePower, basePowerRate, 16, false);
        powerTransfer(BCTransportPipes.stonePower, basePowerRate * 2, 32, false);
        powerTransfer(BCTransportPipes.woodPower, basePowerRate * 4, 128, true);
        powerTransfer(BCTransportPipes.sandstonePower, basePowerRate * 4, 32, false);
        powerTransfer(BCTransportPipes.quartzPower, basePowerRate * 8, 32, false);
        powerTransfer(BCTransportPipes.ironPower, basePowerRate * 8, 32, false);
        powerTransfer(BCTransportPipes.goldPower, basePowerRate * 16, 32, false);
        powerTransfer(BCTransportPipes.diamondPower, basePowerRate * 64, 32, false);
        powerTransfer(BCTransportPipes.diaWoodPower, basePowerRate * 64, 32, true);
    }

    private static void fluidTransfer(PipeDefinition def, int rate, int delay) {
        PipeApi.fluidTransferData.put(def, new PipeApi.FluidTransferInfo(rate, delay));
    }

    private static void powerTransfer(PipeDefinition def, int transferMultiplier, int resistanceDivisor, boolean recv) {
        long transfer = MjAPI.MJ * transferMultiplier;
        long resistance = MjAPI.MJ / resistanceDivisor;
        PipeApi.powerTransferData.put(def, PowerTransferInfo.createFromResistance(transfer, resistance, recv));
    }

    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (BCTransport.MODID.equals(event.getConfig().getModId())) {
            reloadConfig();
        }
    }

    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (BCTransport.MODID.equals(event.getConfig().getModId())) {
            reloadConfig();
        }
    }
}
