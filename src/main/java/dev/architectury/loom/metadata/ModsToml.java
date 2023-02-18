package dev.architectury.loom.metadata;

import net.fabricmc.loom.configuration.ifaceinject.InterfaceInjectionProcessor;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

// A no-op mod metadata file for Forge's mods.toml.
public final class ModsToml implements ModMetadataFile {
	public static final String FILE_PATH = "META-INF/mods.toml";
	public static final ModsToml INSTANCE = new ModsToml();

	private ModsToml() {
	}

	@Override
	public @Nullable String getId() {
		return null;
	}

	@Override
	public Set<String> getAccessWideners() {
		return Set.of();
	}

	@Override
	public List<InterfaceInjectionProcessor.InjectedInterface> getInjectedInterfaces(@Nullable String modId) {
		return List.of();
	}

	@Override
	public String getFileName() {
		return FILE_PATH;
	}

	@Override
	public List<String> getMixinConfigs() {
		return List.of();
	}
}
