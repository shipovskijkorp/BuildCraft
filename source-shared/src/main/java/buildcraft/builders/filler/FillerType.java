package buildcraft.builders.filler;

import buildcraft.builders.registry.FillerRegistry;
import buildcraft.builders.internal.filler.legacy.IFillerPattern;
import buildcraft.builders.BCBuildersStatements;
import buildcraft.lib.statement.StatementType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class FillerType extends StatementType<IFillerPattern> {
    private static final int MAX_NETWORK_ID_LENGTH = 256;
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
        IFillerPattern pattern = FillerRegistry.INSTANCE.getPattern(kind);
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
        String kind = buffer.readUtf(MAX_NETWORK_ID_LENGTH);
        IFillerPattern pattern = FillerRegistry.INSTANCE.getPattern(kind);
        if (pattern == null) {
            return defaultStatement;
        }
        return pattern;
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buffer, IFillerPattern slot) {
        buffer.writeUtf(slot.getUniqueTag(), MAX_NETWORK_ID_LENGTH);
    }
}
