<lore>
Large machines need an exact boundary, otherwise their appetite for blocks can become rather difficult to control.
</lore>
<no_lore>
Land Marks define rectangular work areas for machines such as the Quarry, Filler and Architect Table.
</no_lore>
<hint>
<bold>Hint:</bold> Verify the coloured boundary lines from more than one side before starting a Quarry, Builder or Filler. One misplaced marker can select a much larger area than expected.
</hint>


<recipes_usages stack="buildcraftcore:marker_volume"/>

<chapter name="Defining an Area"/>
Place Land Marks on matching X, Y or Z axes and connect them with the Marker Connector. Connected marks form the edges of a rectangular box. Most machines only need the horizontal corners, but a full three-dimensional box may also be defined.

Powering a Land Mark with redstone displays guide lasers along the axes, making aligned placement easier.

<chapter name="Using the Area"/>
Place the machine next to one of the connected marks. The machine claims the marked box and uses it as its work area. If no valid marks are found, some machines use their own default area instead.

<link to="buildcraftcore:item/marker_connector"/>
<link to="buildcraftcore:item/volume_box"/>
