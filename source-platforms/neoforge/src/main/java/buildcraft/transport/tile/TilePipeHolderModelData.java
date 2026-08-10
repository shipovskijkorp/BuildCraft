package buildcraft.transport.tile;

import buildcraft.transport.client.model.ModelPipe;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

@OnlyIn(Dist.CLIENT)
public class TilePipeHolderModelData {
    public static ModelData build(TilePipeHolder tile) {
        return ModelData.builder().with(ModelPipe.PipeTypeModelKey, tile).build();
    }
}
