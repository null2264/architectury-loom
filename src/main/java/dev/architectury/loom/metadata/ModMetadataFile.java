package dev.architectury.loom.metadata;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.loom.configuration.ifaceinject.InterfaceInjectionProcessor;
import net.fabricmc.loom.util.ZipUtils;

public interface ModMetadataFile {
	/**
	 * {@return the mod ID in this mod metadata file, or {@code null} if absent}.
	 */
	// TODO: When we have mods.toml here, shouldn't it support multiple IDs + maybe a "first ID"?
	@Nullable String getId();

	/**
	 * {@return the path to the access widener file of this mod, or {@code null} if absent}.
	 */
	@Nullable String getAccessWidener();

	/**
	 * {@return the injected interface data in this mod metadata file}.
	 *
	 * @param modId the mod ID to use as a fallback if {@link #getId} returns {@code null}
	 * @throws IllegalArgumentException if both {@code modId} and {@link #getId} are {@code null}
	 */
	List<InterfaceInjectionProcessor.InjectedInterface> getInjectedInterfaces(@Nullable String modId);

	/**
	 * {@return the file name of this mod metadata file}.
	 */
	String getFileName();

	/**
	 * Reads the mod metadata file from a jar.
	 *
	 * @param jar the path to the jar file
	 * @return the mod metadata file, or {@code null} if not found
	 */
	static @Nullable ModMetadataFile fromJar(Path jar) throws IOException {
		// architectury.common.json
		byte[] bytes = ZipUtils.unpackNullable(jar, ArchitecturyCommonJson.FILE_NAME);

		if (bytes != null) {
			return ArchitecturyCommonJson.of(bytes);
		}

		// quilt.mod.json
		bytes = ZipUtils.unpackNullable(jar, QuiltModJson.FILE_NAME);

		if (bytes != null) {
			return QuiltModJson.of(bytes);
		}

		return null;
	}
}
