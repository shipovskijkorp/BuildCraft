<lore>
A furnace that moves a piston instead of cooking dinner is still a perfectly respectable furnace.
</lore>
<no_lore>
The Stirling Engine burns ordinary furnace fuels and produces between one third and one MJ per tick.
</no_lore>

<recipes_usages stack="buildcraftenergy:engine_stone"/>

<chapter name="Fuel and Output"/>
Place any valid furnace fuel in the engine's single fuel slot and supply a redstone signal. The engine adjusts its output between roughly 0.33 MJ/t and 1 MJ/t according to the amount of MJ already held in its 1,000 MJ internal buffer.

The engine consumes the same furnace fuels recognised by Minecraft. Containers returned by a fuel, such as an empty bucket, remain available through the engine inventory.

<chapter name="Engine Mechanics"/>
Place the engine against an MJ receiver. It selects a compatible output face when placed; use a Wrench to rotate it when another face should be powered.

The blue, green, yellow, red and black stages represent the engine's current internal heat, which follows the fill level of its power buffer. If output is blocked the buffer fills, the engine reaches the overheat stage and stops generating. Remove the redstone signal or provide somewhere for the stored MJ to go so it can cool again.

<link to="buildcraftcore:block/engine_basics"/>
<link to="buildcraftcore:item/wrench"/>
