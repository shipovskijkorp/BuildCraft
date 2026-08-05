<lore>
Power is useful only after it reaches the machine that was supposed to receive it.
</lore>
<no_lore>
The Sandstone Power carries power between pipes without connecting directly to neighbouring machines.
</no_lore>
<hint>
<bold>Hint:</bold> Use Sandstone Power Pipes near machines that should not connect directly to the line. Confirm the intended pipe-to-pipe backbone remains continuous.
</hint>


<recipes_usages stack="buildcrafttransport:sandstone_power"/>

<chapter name="Power Pipe Mechanics"/>
Its default transfer limit is 16 MJ per tick. Use it to route a line past machines that should not become power endpoints.

Power entering a junction is distributed proportionally among requesting outputs. A section accepts no more than its current free capacity, and the network preserves unaccepted energy instead of duplicating or discarding it during normal transfer.

Use Pipe Plugs or paint neighbouring pipes different colours to prevent unwanted connections. Power-pipe limits are configured when the network changes and are restored after loading a world.
