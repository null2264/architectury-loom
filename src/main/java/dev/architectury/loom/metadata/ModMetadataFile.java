package dev.architectury.loom.metadata;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.loom.configuration.ifaceinject.InterfaceInjectionProcessor;

public interface ModMetadataFile {
	@Nullable String getAccessWidener();
	List<InterfaceInjectionProcessor.InjectedInterface> getInjectedInterfaces(@Nullable String modId);
}
