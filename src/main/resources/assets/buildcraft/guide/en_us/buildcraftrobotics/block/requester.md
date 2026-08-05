<lore>
A storage room is much more useful when it can ask for missing stock before someone notices the empty shelf.
</lore>
<no_lore>
The Requester maintains configured item quantities and exposes the missing amounts to Delivery Robots.
</no_lore>
<hint>
<bold>Hint:</bold> Request small quantities first and confirm that providers and delivery routes are reachable. Large permanent requests can monopolise carriers and storage.
</hint>


<recipes_usages stack="buildcraftrobotics:requester"/>

<chapter name="Configuring Requests"/>
The upper twenty phantom slots define the requested item and quantity for each position. The matching real slot below accepts only that requested item or a compatible List match.

Delivery Robots query the missing amount and deliver items until each request is fulfilled. Items already present reduce the active request rather than creating duplicate work.

<chapter name="Redstone Output"/>
A comparator reports the average fill level of configured request slots. Empty configurations produce no signal; increasingly complete requests raise the signal toward fifteen.

<link to="buildcraftrobotics:robot/delivery"/>
