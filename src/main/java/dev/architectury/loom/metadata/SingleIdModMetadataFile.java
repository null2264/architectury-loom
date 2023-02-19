package dev.architectury.loom.metadata;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

interface SingleIdModMetadataFile extends ModMetadataFile {
	@Override
	default Set<String> getIds() {
		final String id = getId();
		return id != null ? Set.of(id) : Set.of();
	}

	@Override
	@Nullable String getId();
}
