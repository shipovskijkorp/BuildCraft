package buildcraft.api.v2.statement;

import net.minecraft.resources.ResourceLocation;

public interface StatementCollector {
    void addTrigger(ResourceLocation triggerId);
    void addAction(ResourceLocation actionId);
}
