package buildcraft.api.v2.filler;

@FunctionalInterface
public interface FillerPattern {
    FillerMask createMask(FillerPatternContext context);
}
