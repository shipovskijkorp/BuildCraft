<lore>
A buffer is most helpful when it knows which items are actually welcome inside it.
</lore>
<no_lore>
The Filtered Buffer is a nine-slot inventory whose real slots are controlled by matching phantom filters.
</no_lore>

<recipes_usages stack="buildcrafttransport:filtered_buffer"/>

<chapter name="Filtering"/>
Configure the nine phantom filter slots in the interface. Each matching real inventory slot accepts only the item selected by its filter. Pipes and other item handlers can insert into or extract from the real inventory through the exposed item capability.

Use the buffer between sorting stages to reserve separate storage positions for different items and to stop unrelated stacks from entering the line.
