package dev.architectury.loom.extensions;

import java.nio.file.Path;

import org.gradle.api.logging.Logger;

import net.fabricmc.loom.util.ModPlatform;
import net.fabricmc.loom.util.ZipUtils;
import net.fabricmc.loom.util.fmj.FabricModJsonFactory;

public final class ModDependencyExtensions {
	public static boolean shouldRemapMod(Logger logger, Path modJar, ModPlatform platform, String config) {
		if (ZipUtils.contains(modJar, "architectury.common.marker")) return true;
		if (FabricModJsonFactory.isModJar(modJar, platform)) return true;

		if (platform == ModPlatform.FORGE) {
			// TODO: Info log level
			logger.lifecycle(":could not find forge mod in " + config + " but forcing: {}", modJar.getFileName());
			return true;
		}

		return false;
	}
}
