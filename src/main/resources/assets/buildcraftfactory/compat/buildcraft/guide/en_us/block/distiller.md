<lore>
Crude oil is a complicated collection of useful fluids waiting for someone to separate them.
</lore>
<no_lore>
The Distiller consumes MJ and separates a fluid into one gaseous output and one liquid output.
</no_lore>
<hint>
<bold>Hint:</bold> Store each Distiller output in a separate tank. Mixing intermediate fluids or routing both outputs through one unfiltered line makes later refining stages difficult to diagnose.
</hint>


<recipes_usages stack="buildcraftfactory:distiller"/>

<chapter name="Connections"/>
Insert the source fluid into the input tank. The gaseous product is extracted from the top and the liquid product from the bottom. Keep both outputs clear: the machine cannot complete another operation when either destination has no room.

Supply MJ through a compatible power connection. More available power lets the current operation advance faster, up to the machine's processing limit.

<chapter name="Temperature Selects the Recipe"/>
Cool, Hot and Searing forms of the same oil product can have different distillation recipes. The outputs retain the input temperature, so a Heat Exchanger is used between distillation stages when the next recipe requires another heat level.

Crude Oil can begin three different refining routes. Further distillation of Distilled Oil, Heavy Oil, Dense Oil and the mixed fuels separates them into Gaseous, Light and Dense Fuel plus Residue.

<link to="buildcraftfactory:block/heat_exchange"/>
<link to="buildcraftenergy:item/oil"/>
<link to="buildcraftenergy:block/engine_iron"/>
