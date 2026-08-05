<lore>
Mining by hand is rewarding until the desired ore is underneath several million blocks that are not the desired ore.
</lore>
<no_lore>
The Quarry builds a frame and automatically mines every permitted block inside its selected area.
</no_lore>

<recipes_usages stack="buildcraftbuilders:quarry"/>

<chapter name="Selecting the Area"/>
Place the Quarry beside connected Land Marks to define its horizontal work area. Without a valid marked area it creates a smaller default quarry in front of itself. The Quarry loads the chunks needed for its claimed work area while operating.

<chapter name="Mining"/>
Supply MJ. The Quarry first scans and builds its frame, then moves the drill across the area and mines downward to the configured depth or world bottom. More available power allows more work to be completed each tick, up to the machine limit.

Mined drops are offered to adjacent inventories and item pipes. Keep enough output space available: a blocked output can stall useful progress. Protected blocks are checked using the Quarry owner's fake player and are not silently bypassed.

<link to="buildcraftcore:item/marker_volume"/>
<link to="buildcraftbuilders:block/frame"/>
