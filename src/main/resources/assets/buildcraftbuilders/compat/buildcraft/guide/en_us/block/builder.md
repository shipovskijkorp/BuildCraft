<lore>
A plan is only a collection of ideas until something begins placing blocks.
</lore>
<no_lore>
The Builder consumes MJ and resources to construct a used Blueprint or Template.
</no_lore>

<recipes_usages stack="buildcraftbuilders:builder"/>

<chapter name="Supplying the Builder"/>
Insert a used Blueprint or Template into the snapshot slot. The requirements display shows needed materials. Supply items through the resource inventory or connected item pipes, fluids through the internal tanks, and MJ through a compatible power connection.

Blueprints request their recorded blocks. Templates use suitable supplied blocks to reproduce the saved shape.

<chapter name="Placement and Options"/>
The structure is placed relative to the Builder's facing. Rotation and excavation are available only when allowed by the snapshot header. When a connected Path Mark route is present, the Builder can repeat the design at positions along the path.

Work state and reserved resources are saved. After a reload, unfinished tasks are rechecked and their reserved items, fluids and energy are safely returned before work resumes.

<link to="buildcraftcore:item/marker_path"/>
<link to="buildcraftbuilders:item/blueprint"/>
<link to="buildcraftbuilders:item/template"/>
