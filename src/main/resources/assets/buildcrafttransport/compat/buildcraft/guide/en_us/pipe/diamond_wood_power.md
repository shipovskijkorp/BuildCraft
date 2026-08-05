<lore>
Power is useful only after it reaches the machine that was supposed to receive it.
</lore>
<no_lore>
The Diamond Wood Power is a high-capacity wooden extraction pipe.
</no_lore>

<recipes_usages stack="buildcrafttransport:diamond_wood_power"/>

<chapter name="Power Pipe Mechanics"/>
Its default transfer limit is 256 MJ per tick. Like the normal Wooden Power Pipe, it receives MJ from compatible producers and does not connect to another wooden extraction pipe.

Power entering a junction is distributed proportionally among requesting outputs. A section accepts no more than its current free capacity, and the network preserves unaccepted energy instead of duplicating or discarding it during normal transfer.

Use Pipe Plugs or paint neighbouring pipes different colours to prevent unwanted connections. Power-pipe limits are configured when the network changes and are restored after loading a world.
