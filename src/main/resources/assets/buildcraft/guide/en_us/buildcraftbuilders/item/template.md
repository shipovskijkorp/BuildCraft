<lore>
Sometimes the shape matters more than the exact stone, wood or metal used to fill it.
</lore>
<no_lore>
A Template records only the occupied shape of an area, allowing a Builder to fill it with supplied materials.
</no_lore>
<hint>
<bold>Hint:</bold> Templates record shape rather than exact block identity. Use them when the same structure should be rebuilt from locally available materials.
</hint>


<recipes_usages stack="buildcraftbuilders:template"/>

<chapter name="Recording"/>
Record a Template in an Architect Table exactly like a Blueprint. The scan remembers which positions are filled and which are empty, but does not preserve the original block types or block-entity data.

<chapter name="Building"/>
Insert the used Template into a Builder and supply the blocks you want to use. The Builder consumes available materials to reproduce the recorded shape. Templates are useful for roads, walls, shells and repeated structures where the final material may change.

<link to="buildcraftbuilders:item/blueprint"/>
<link to="buildcraftbuilders:block/architect"/>
<link to="buildcraftbuilders:block/builder"/>
