package buildcraft.robotics;

import buildcraft.lib.internal.statement.api2.StatementApi2Bridge;
import buildcraft.robotics.statements.*;

public class BCRoboticsStatements {

    // ── Triggers ──────────────────────────────────────────────
    public static final TriggerRobotInStation TRIGGER_ROBOT_IN_STATION;
    public static final TriggerRobotSleep     TRIGGER_ROBOT_SLEEP;
    public static final TriggerRobotLinked    TRIGGER_ROBOT_LINKED;
    public static final TriggerRobotLinked    TRIGGER_ROBOT_RESERVED;

    // ── Robot actions ─────────────────────────────────────────
    public static final ActionRobotFilter        ACTION_ROBOT_FILTER;
    public static final ActionRobotFilterTool    ACTION_ROBOT_FILTER_TOOL;
    public static final ActionRobotGotoStation   ACTION_ROBOT_GOTO_STATION;
    public static final ActionRobotWakeUp        ACTION_ROBOT_WAKEUP;
    public static final ActionRobotWorkInArea    ACTION_ROBOT_WORK_IN_AREA;
    public static final ActionRobotWorkInArea    ACTION_ROBOT_LOAD_UNLOAD_AREA;

    // ── Station actions ───────────────────────────────────────
    public static final ActionStationAcceptFluids       ACTION_STATION_ACCEPT_FLUIDS;
    public static final ActionStationAcceptItems        ACTION_STATION_ACCEPT_ITEMS;
    public static final ActionStationForbidRobot        ACTION_STATION_FORBID_ROBOT;
    public static final ActionStationForbidRobot        ACTION_STATION_FORCE_ROBOT;
    public static final ActionStationProvideFluids      ACTION_STATION_PROVIDE_FLUIDS;
    public static final ActionStationProvideItems       ACTION_STATION_PROVIDE_ITEMS;
    public static final ActionStationRequestItems       ACTION_STATION_REQUEST_ITEMS;
    public static final ActionStationRequestItemsMachine ACTION_STATION_MACHINE_REQUEST;

    static {
        TRIGGER_ROBOT_IN_STATION    = new TriggerRobotInStation();
        TRIGGER_ROBOT_SLEEP         = new TriggerRobotSleep();
        TRIGGER_ROBOT_LINKED        = new TriggerRobotLinked(false);
        TRIGGER_ROBOT_RESERVED      = new TriggerRobotLinked(true);

        ACTION_ROBOT_FILTER         = new ActionRobotFilter();
        ACTION_ROBOT_FILTER_TOOL    = new ActionRobotFilterTool();
        ACTION_ROBOT_GOTO_STATION   = new ActionRobotGotoStation();
        ACTION_ROBOT_WAKEUP         = new ActionRobotWakeUp();
        ACTION_ROBOT_WORK_IN_AREA   = new ActionRobotWorkInArea(false);
        ACTION_ROBOT_LOAD_UNLOAD_AREA = new ActionRobotWorkInArea(true);

        ACTION_STATION_ACCEPT_FLUIDS  = new ActionStationAcceptFluids();
        ACTION_STATION_ACCEPT_ITEMS   = new ActionStationAcceptItems();
        ACTION_STATION_FORBID_ROBOT   = new ActionStationForbidRobot(false);
        ACTION_STATION_FORCE_ROBOT    = new ActionStationForbidRobot(true);
        ACTION_STATION_PROVIDE_FLUIDS = new ActionStationProvideFluids();
        ACTION_STATION_PROVIDE_ITEMS  = new ActionStationProvideItems();
        ACTION_STATION_REQUEST_ITEMS  = new ActionStationRequestItems();
        ACTION_STATION_MACHINE_REQUEST = new ActionStationRequestItemsMachine();

        StatementApi2Bridge.mirrorLegacyStatements(
            TRIGGER_ROBOT_IN_STATION, TRIGGER_ROBOT_SLEEP, TRIGGER_ROBOT_LINKED, TRIGGER_ROBOT_RESERVED,
            ACTION_ROBOT_FILTER, ACTION_ROBOT_FILTER_TOOL, ACTION_ROBOT_GOTO_STATION, ACTION_ROBOT_WAKEUP,
            ACTION_ROBOT_WORK_IN_AREA, ACTION_ROBOT_LOAD_UNLOAD_AREA,
            ACTION_STATION_ACCEPT_FLUIDS, ACTION_STATION_ACCEPT_ITEMS, ACTION_STATION_FORBID_ROBOT,
            ACTION_STATION_FORCE_ROBOT, ACTION_STATION_PROVIDE_FLUIDS, ACTION_STATION_PROVIDE_ITEMS,
            ACTION_STATION_REQUEST_ITEMS, ACTION_STATION_MACHINE_REQUEST
        );
    }

    public static void preInit() {
        // Static initialization performs construction-safe API2 mirroring.
    }
}
