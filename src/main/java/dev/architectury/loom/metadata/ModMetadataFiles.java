package dev.architectury.loom.metadata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.loom.util.ZipUtils;
import net.fabricmc.loom.util.gradle.SourceSetHelper;

public final class ModMetadataFiles {
	private static final Map<String, Function<byte[], ModMetadataFile>> SINGLE_FILE_METADATA_TYPES = ImmutableMap.<String, Function<byte[], ModMetadataFile>>builder()
			.put(ArchitecturyCommonJson.FILE_NAME, ArchitecturyCommonJson::of)
			.put(QuiltModJson.FILE_NAME, QuiltModJson::of)
			.build();

	public static @Nullable ModMetadataFile fromJar(Path jar) throws IOException {
		for (final String filePath : SINGLE_FILE_METADATA_TYPES.keySet()) {
			final byte @Nullable [] bytes = ZipUtils.unpackNullable(jar, filePath);

			if (bytes != null) {
				return SINGLE_FILE_METADATA_TYPES.get(filePath).apply(bytes);
			}
		}

		return null;
	}

	public static @Nullable ModMetadataFile fromDirectory(Path directory) throws IOException {
		for (final String filePath : SINGLE_FILE_METADATA_TYPES.keySet()) {
			final Path metadataPath = directory.resolve(filePath);

			if (Files.exists(metadataPath)) {
				return SINGLE_FILE_METADATA_TYPES.get(filePath).apply(Files.readAllBytes(directory));
			}
		}

		return null;
	}

	public static @Nullable ModMetadataFile fromSourceSets(SourceSet... sourceSets) throws IOException {
		for (final String filePath : SINGLE_FILE_METADATA_TYPES.keySet()) {
			final @Nullable File file = SourceSetHelper.findFirstFileInResource(filePath, sourceSets);

			if (file != null) {
				return SINGLE_FILE_METADATA_TYPES.get(filePath).apply(Files.readAllBytes(file.toPath()));
			}
		}

		return null;
	}
}
