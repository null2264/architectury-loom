package dev.architectury.loom.metadata;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.loom.configuration.ifaceinject.InterfaceInjectionProcessor;

public interface ModMetadataFile {
	/**
	 * {@return the mod ID in this mod metadata file, or {@code null} if absent}.
	 */
	// TODO: When we have mods.toml here, shouldn't it support multiple IDs + maybe a "first ID"?
	@Nullable String getId();

	/**
	 * {@return the paths to the access widener file of this mod, or an empty set if absent}.
	 */
	Set<String> getAccessWideners();

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
	 * {@return a list of the mixin configs declared in this mod metadata file}.
	 */
	List<String> getMixinConfigs();
}
