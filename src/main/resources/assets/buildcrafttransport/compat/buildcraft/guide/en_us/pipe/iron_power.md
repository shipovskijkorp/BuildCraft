<lore>
Power is useful only after it reaches the machine that was supposed to receive it.
</lore>
<no_lore>
The Iron Power is a configurable power limiter.
</no_lore>
<hint>
<bold>Hint:</bold> Recheck directional power outputs after rotating or moving the pipe. One incorrect face can starve an entire branch.
</hint>


<recipes_usages stack="buildcrafttransport:iron_power"/>

<chapter name="Power Pipe Mechanics"/>
Its full transfer limit is 32 MJ per tick. Use a Wrench to halve the limit repeatedly down to one sixty-fourth, then disable transfer entirely before cycling back to full power.

Power entering a junction is distributed proportionally among requesting outputs. A section accepts no more than its current free capacity, and the network preserves unaccepted energy instead of duplicating or discarding it during normal transfer.

Use Pipe Plugs or paint neighbouring pipes different colours to prevent unwanted connections. Power-pipe limits are configured when the network changes and are restored after loading a world.
