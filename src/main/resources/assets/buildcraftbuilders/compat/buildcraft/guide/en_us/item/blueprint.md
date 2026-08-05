<lore>
A finished structure is useful once; a Blueprint allows the same design to be built again and again.
</lore>
<no_lore>
A Blueprint stores exact blocks, block states and supported block-entity data from a scanned area.
</no_lore>

<recipes_usages stack="buildcraftbuilders:blueprint"/>

<chapter name="Recording"/>
Insert a blank Blueprint into an Architect Table beside a Land Mark or Volume Box area. The table scans the selected volume over time and returns a used Blueprint. Blank Blueprints stack to sixteen; recorded Blueprints do not stack.

A Blueprint remembers the real materials and orientation of the scanned structure. Supported block-entity data and entities are included when the current schematic handlers allow them.

<chapter name="Building"/>
Place the used Blueprint in a Builder. Supply the requested blocks, fluids and MJ. The Builder recreates the recorded structure and can rotate or excavate it when those options were enabled while recording.

<link to="buildcraftbuilders:block/architect"/>
<link to="buildcraftbuilders:block/builder"/>
<link to="buildcraftbuilders:item/template"/>
