<lore>
Power is useful only after it reaches the machine that was supposed to receive it.
</lore>
<no_lore>
The Cobblestone Power is the basic low-capacity power transport pipe.
</no_lore>

<recipes_usages stack="buildcrafttransport:cobblestone_power"/>

<chapter name="Power Pipe Mechanics"/>
Its default transfer limit is 4 MJ per tick. It does not connect to Stone Power Pipes, which allows adjacent lines to remain separate.

Power entering a junction is distributed proportionally among requesting outputs. A section accepts no more than its current free capacity, and the network preserves unaccepted energy instead of duplicating or discarding it during normal transfer.

Use Pipe Plugs or paint neighbouring pipes different colours to prevent unwanted connections. Power-pipe limits are configured when the network changes and are restored after loading a world.
