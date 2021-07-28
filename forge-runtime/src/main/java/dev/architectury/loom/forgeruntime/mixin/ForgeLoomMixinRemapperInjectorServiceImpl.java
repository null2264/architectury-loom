package dev.architectury.loom.forgeruntime.mixin;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;

import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;

public class ForgeLoomMixinRemapperInjectorServiceImpl {
	private static final Logger LOGGER = LogManager.getLogger("ForgeLoomRemapperInjector");

	public static void attach() {
		LOGGER.debug("We will be injecting our remapper.");

		try {
			MixinEnvironment.getDefaultEnvironment().getRemappers().add(new MixinIntermediaryDevRemapper(Objects.requireNonNull(resolveMappings()), "intermediary", "named"));
			LOGGER.debug("We have successfully injected our remapper.");
		} catch (Exception e) {
			LOGGER.debug("We have failed to inject our remapper.", e);
		}
	}

	private static TinyTree resolveMappings() {
		try {
			String srgNamedProperty = System.getProperty("mixin.forgeloom.inject.mappings.srg-named");
			Path path = Paths.get(srgNamedProperty);

			try (BufferedReader reader = Files.newBufferedReader(path)) {
				return TinyMappingFactory.loadWithDetection(reader);
			}
		} catch (Throwable throwable) {
			throwable.printStackTrace();
			return null;
		}
	}
}
