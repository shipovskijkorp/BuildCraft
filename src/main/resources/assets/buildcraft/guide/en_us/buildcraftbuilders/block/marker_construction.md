<lore>
A Builder robot needs a plan and a place to collect work even when no stationary Builder is doing the construction itself.
</lore>
<no_lore>
The Construction Marker exposes Blueprint build tasks to Builder Robots.
</no_lore>
<hint>
<bold>Hint:</bold> Construction Markers are best used to inspect a plan before resources are supplied. Correct the anchor and orientation while changes are still free.
</hint>


<recipes_usages stack="buildcraftbuilders:marker_construction"/>

<chapter name="Robot Construction"/>
Place a Construction Marker and insert a used Blueprint. The marker creates build tasks but does not consume MJ or place blocks by itself. Nearby Builder Robots can reserve those tasks, fetch the required materials and perform the construction.

The marker uses the snapshot's rotation, excavation and ownership settings. Restrict Builder Robots with a work zone when several projects are close together.

<link to="buildcraftrobotics:robot/builder"/>
<link to="buildcraftbuilders:item/blueprint"/>
