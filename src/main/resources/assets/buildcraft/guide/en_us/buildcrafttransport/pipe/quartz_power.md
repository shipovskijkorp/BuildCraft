<lore>
Power is useful only after it reaches the machine that was supposed to receive it.
</lore>
<no_lore>
The Quartz Power is a medium-capacity transport pipe separated from stone and cobblestone pipe families.
</no_lore>
<hint>
<bold>Hint:</bold> Use distinct materials to mark separate voltage or workload zones, even when their current throughput is similar.
</hint>


<recipes_usages stack="buildcrafttransport:quartz_power"/>

<chapter name="Power Pipe Mechanics"/>
Its default transfer limit is 32 MJ per tick.

Power entering a junction is distributed proportionally among requesting outputs. A section accepts no more than its current free capacity, and the network preserves unaccepted energy instead of duplicating or discarding it during normal transfer.

Use Pipe Plugs or paint neighbouring pipes different colours to prevent unwanted connections. Power-pipe limits are configured when the network changes and are restored after loading a world.
