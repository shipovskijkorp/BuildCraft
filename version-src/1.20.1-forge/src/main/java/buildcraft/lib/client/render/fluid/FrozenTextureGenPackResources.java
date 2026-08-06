package buildcraft.lib.client.render.fluid;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.HashBiMap;
import com.mojang.blaze3d.platform.NativeImage;

import buildcraft.api.core.BCLog;
import buildcraft.lib.BCLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class FrozenTextureGenPackResources implements PackResources {

    private static final Logger LOGGER = BCLog.logger;
    private static final int SIZE = 2;
    private static final String PACK_ID = "buildcraftlib_generated_frozen_textures";

    /** Maps generated texture locations to their source textures. */
    private final HashBiMap<ResourceLocation, ResourceLocation> transLocations = HashBiMap.create();

    protected InputStream load(ResourceLocation srcLocation) {
        if (srcLocation == null) {
            return null;
        }

        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        Optional<Resource> resourceOp = manager.getResource(srcLocation);
        if (resourceOp.isEmpty()) {
            LOGGER.error("Cannot read resource {} while creating frozen sprite; it may have been requested too early", srcLocation);
            return null;
        }

        Resource resource = resourceOp.get();
        try (InputStream input = resource.open(); NativeImage srcImage = NativeImage.read(input)) {
            int widthOld = srcImage.getWidth();
            int heightOld = srcImage.getHeight();
            try {
                var animationMetadataSection = resource.metadata().getSection(AnimationMetadataSection.SERIALIZER);
                if (animationMetadataSection.isEmpty() && widthOld != heightOld) {
                    LOGGER.warn(
                        "[lib.fluid] Failed to create a frozen sprite of {} because the source was not animated and "
                            + "had different dimensions ({}x{})",
                        srcLocation,
                        widthOld,
                        heightOld
                    );
                    return null;
                }
            } catch (Exception exception) {
                LOGGER.error("Unable to parse metadata from {}", srcLocation, exception);
            }

            int width = widthOld * SIZE;
            int height = width;
            try (NativeImage outImage = new NativeImage(width, height, false)) {
                for (int x = 0; x < width; x++) {
                    int sourceX = x % widthOld;
                    for (int y = 0; y < height; y++) {
                        int sourceY = y % heightOld;
                        outImage.setPixelRGBA(x, y, srcImage.getPixelRGBA(sourceX, sourceY));
                    }
                }
                return new ByteArrayInputStream(outImage.asByteArray());
            }
        } catch (IOException exception) {
            LOGGER.warn("Failed to read generated fluid texture {}", srcLocation, exception);
            return null;
        }
    }

    public ResourceLocation registry(ResourceLocation srcLocation) {
        String path = "fluid_" + srcLocation.toString().replace(':', '_') + "_convert_frozen";
        ResourceLocation generated = new ResourceLocation(BCLib.MODID, "textures/" + path + ".png");
        ResourceLocation source = new ResourceLocation(
            srcLocation.getNamespace(),
            "textures/" + srcLocation.getPath() + ".png"
        );
        transLocations.put(generated, source);
        return new ResourceLocation(BCLib.MODID, path);
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... pathSegments) {
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType packType, ResourceLocation location) {
        if (packType != PackType.CLIENT_RESOURCES) {
            return null;
        }
        ResourceLocation source = transLocations.get(location);
        if (source == null) {
            return null;
        }
        return () -> {
            InputStream stream = load(source);
            if (stream == null) {
                throw new FileNotFoundException(source.toString());
            }
            return stream;
        };
    }

    @Override
    public void listResources(PackType packType, String namespace, String path, ResourceOutput output) {
        if (packType != PackType.CLIENT_RESOURCES || !BCLib.MODID.equals(namespace)) {
            return;
        }
        for (ResourceLocation location : transLocations.keySet()) {
            if (location.getNamespace().equals(namespace) && location.getPath().startsWith(path)) {
                IoSupplier<InputStream> supplier = getResource(packType, location);
                if (supplier != null) {
                    output.accept(location, supplier);
                }
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType packType) {
        return packType == PackType.CLIENT_RESOURCES ? Set.of(BCLib.MODID) : Set.of();
    }

    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
        return null;
    }

    @Override
    public String packId() {
        return PACK_ID;
    }

    public void clear() {
        transLocations.clear();
    }

    @Override
    public void close() {
        // This singleton survives resource-manager replacement during texture reloads.
    }
}
