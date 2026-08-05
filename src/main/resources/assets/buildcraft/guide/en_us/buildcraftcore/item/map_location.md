<lore>
Robots and machines are much easier to command when a place, path or work zone can be carried in your pocket.
</lore>
<no_lore>
A Map Location records a point, area, path or Robotics zone for later use.
</no_lore>
<hint>
<bold>Hint:</bold> Record locations while standing on the exact block and facing the intended direction. This avoids one-block offsets when the location is reused by automation.
</hint>


<recipes_usages stack="buildcraftcore:map_location"/>

<chapter name="Recording Locations"/>
Use a blank Map Location on a normal block to record that point and the clicked face. Use it on a connected Land Mark, Volume Box or other area provider to record its box. Use it on a Path Mark network to record the path.

Sneak-right-click in the air to clear the stored data. Blank locations stack, while recorded locations are kept separate.

<chapter name="Robotics Zones"/>
The Zone Planner can write a sixteen-layer painted work zone to a Map Location. Gates and Docking Stations can then use that map to restrict where linked robots work.

<link to="buildcraftrobotics:block/zone_planner"/>
