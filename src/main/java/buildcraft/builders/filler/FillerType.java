package buildcraft.builders.filler;

import buildcraft.api.filler.FillerManager;
import buildcraft.api.filler.IFillerPattern;
import buildcraft.builders.BCBuildersStatements;
import buildcraft.lib.statement.StatementType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class FillerType extends StatementType<IFillerPattern> {
    public static final FillerType INSTANCE = new FillerType();

    private FillerType() {
        super(IFillerPattern.class, BCBuildersStatements.PATTERN_NONE);
    }

    @Override
    public IFillerPattern convertToType(Object value) {
        return value instanceof IFillerPattern ? (IFillerPattern) value : null;
    }

    @Override
    public IFillerPattern readFromNbt(CompoundTag nbt) {
        String kind = nbt.getString("kind");
        IFillerPattern pattern = FillerManager.registry.getPattern(kind);
        if (pattern == null) {
            return defaultStatement;
        }
        return pattern;
    }

    @Override
    public CompoundTag writeToNbt(IFillerPattern slot) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("kind", slot.getUniqueTag());
        return nbt;
    }

    @Override
    public IFillerPattern readFromBuffer(FriendlyByteBuf buffer) {
        String kind = buffer.readUtf();
        IFillerPattern pattern = FillerManager.registry.getPattern(kind);
        if (pattern == null) {
            return defaultStatement;
        }
        return pattern;
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buffer, IFillerPattern slot) {
        buffer.writeUtf(slot.getUniqueTag());
    }
}
