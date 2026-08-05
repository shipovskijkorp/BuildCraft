<lore>
Refining oil is useful. Turning it into enough power to run an industrial accident is considerably more useful.
</lore>
<no_lore>
The Combustion Engine burns BuildCraft liquid fuels, uses coolant to control heat and stores residue produced by dirty fuels.
</no_lore>

<recipes_usages stack="buildcraftenergy:engine_iron"/>

<chapter name="Tanks and Fuels"/>
The engine has separate tanks for fuel, coolant and residue. Fluid pipes or containers can fill and drain them directly. Apply a redstone signal to burn the inserted fuel; output depends on the selected fuel and ranges from low-output oil products to 8 MJ/t Gaseous Fuel.

Crude Oil, Heavy Oil and Dense Oil are dirty fuels. Burning them gradually produces Residue, and a full residue tank prevents that by-product from being accepted. Refined fuels burn without residue.

<chapter name="Cooling"/>
Water is the standard liquid coolant. Ice and Packed Ice can also be loaded as solid coolants and are converted into water with greater cooling strength. Hot biomes make the engine heat faster and cool less efficiently, while cold biomes have the opposite effect.

When the redstone signal is removed the engine pauses, applies a short restart cooling penalty and begins shedding excess heat. Keep coolant supplied before operating high-output fuels for long periods.

<chapter name="Engine Mechanics"/>
Place the engine against an MJ receiver and rotate it with a Wrench when necessary. The coloured stages show its heat level. Stored MJ is transferred only when the adjacent receiver accepts it; unaccepted power remains in the engine rather than being removed twice.

<link to="buildcraftcore:block/engine_basics"/>
<link to="buildcraftfactory:block/distiller"/>
<link to="buildcraftfactory:block/heat_exchange"/>
<link to="buildcraftenergy:item/oil"/>
