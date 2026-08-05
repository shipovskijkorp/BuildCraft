<lore>
A robot told to work everywhere will eventually discover somewhere you did not want it to work.
</lore>
<no_lore>
The Zone Planner paints detailed multi-layer robot work zones onto Map Locations.
</no_lore>

<recipes_usages stack="buildcraftrobotics:zone_planner"/>

<chapter name="Drawing a Zone"/>
Insert a blank Map Location and edit the zone in the planner. The map supports sixteen independently editable layers over a 2048 by 2048 coordinate grid. Layers can be selected, combined and named in the interface.

After 120 ticks the output slot receives a Map Location containing the current Zone Plan. A previously saved zone may be placed in the import slot to continue editing it.

<chapter name="Using the Zone"/>
Place the resulting Map Location into a compatible Gate parameter or Docking Station action. Linked robots then restrict searches and work to the painted zone.

<link to="buildcraftcore:item/map_location"/>
