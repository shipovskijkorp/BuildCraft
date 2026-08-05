<lore>
Replacing every copy of one block in a large design by hand is a poor use of an architect's time.
</lore>
<no_lore>
A Single Block Schematic records one block state for use by the Replacer.
</no_lore>
<hint>
<bold>Hint:</bold> Single-block schematics are useful for testing placement rules and rotations before committing a large Blueprint to a Builder.
</hint>


<recipes_usages stack="buildcraftbuilders:schematic_single"/>

<chapter name="Recording a Block"/>
Use a blank schematic on a block to record it. The recorded schematic stores the block state and supported requirements needed to reproduce that block. Sneak-right-click to clear it.

<chapter name="Replacing Blueprint Blocks"/>
The Replacer accepts a used Blueprint, a source schematic and a destination schematic. Every matching source block in the Blueprint is changed to the destination block without modifying the world directly.

<link to="buildcraftbuilders:block/replacer"/>
