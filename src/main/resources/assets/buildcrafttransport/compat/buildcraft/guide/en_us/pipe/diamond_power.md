<lore>
Power is useful only after it reaches the machine that was supposed to receive it.
</lore>
<no_lore>
The Diamond Power is a very high-capacity configurable power limiter.
</no_lore>
<hint>
<bold>Hint:</bold> Use Diamond Power Pipes for high-demand trunks and branching points, not as a substitute for balancing the consumers on each branch.
</hint>


<recipes_usages stack="buildcrafttransport:diamond_power"/>

<chapter name="Power Pipe Mechanics"/>
Its full transfer limit is 256 MJ per tick. Use a Wrench to halve the limit repeatedly or disable transfer entirely.

Power entering a junction is distributed proportionally among requesting outputs. A section accepts no more than its current free capacity, and the network preserves unaccepted energy instead of duplicating or discarding it during normal transfer.

Use Pipe Plugs or paint neighbouring pipes different colours to prevent unwanted connections. Power-pipe limits are configured when the network changes and are restored after loading a world.
