package dev.architectury.loom.metadata;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loom.LoomGradlePlugin;
import net.fabricmc.loom.configuration.ifaceinject.InterfaceInjectionProcessor;

public final class QuiltModJson implements ModMetadataFile {
	private static final Logger LOGGER = LoggerFactory.getLogger(QuiltModJson.class);
	private static final String ACCESS_WIDENER_KEY = "access_widener";
	private static final String MIXIN_KEY = "mixin";

	private final JsonObject json;

	private QuiltModJson(JsonObject json) {
		this.json = Objects.requireNonNull(json, "json");
	}

	public static QuiltModJson of(byte[] utf8) {
		return of(new String(utf8, StandardCharsets.UTF_8));
	}

	public static QuiltModJson of(String text) {
		return of(LoomGradlePlugin.GSON.fromJson(text, JsonObject.class));
	}

	public static QuiltModJson of(Path path) throws IOException {
		return of(Files.readString(path, StandardCharsets.UTF_8));
	}

	public static QuiltModJson of(File file) throws IOException {
		return of(file.toPath());
	}

	public static QuiltModJson of(JsonObject json) {
		return new QuiltModJson(json);
	}

	@Override
	public @Nullable String getAccessWidener() {
		if (json.has(ACCESS_WIDENER_KEY)) {
			if (json.get(ACCESS_WIDENER_KEY).isJsonArray()) {
				JsonArray array = json.get(ACCESS_WIDENER_KEY).getAsJsonArray();

				// TODO (1.1): Support multiple access wideners in Quilt mods
				if (array.size() != 1) {
					throw new UnsupportedOperationException("Loom does not support multiple access wideners in one mod!");
				}

				return array.get(0).getAsString();
			} else {
				return json.get(ACCESS_WIDENER_KEY).getAsString();
			}
		} else {
			return null;
		}
	}

	@Override
	public List<InterfaceInjectionProcessor.InjectedInterface> getInjectedInterfaces(@Nullable String modId) {
		try {
			modId = Objects.requireNonNullElseGet(modId, () -> json.getAsJsonObject("quilt_loader").get("id").getAsString());
		} catch (NullPointerException e) {
			throw new IllegalArgumentException("Could not determine mod ID for Quilt mod and no fallback provided");
		}

		// Quilt injected interfaces have the same format as architectury.common.json
		if (json.has("quilt_loom")) {
			JsonElement quiltLoom = json.get("quilt_loom");

			if (quiltLoom.isJsonObject()) {
				return ArchitecturyCommonJson.getInjectedInterfaces(json.getAsJsonObject("quilt_loom"), modId);
			} else {
				LOGGER.warn("Unexpected type for 'quilt_loom' in quilt.mod.json: {}", quiltLoom.getClass());
			}
		}

		return List.of();
	}

	public List<String> getMixinConfigs() {
		// RFC 0002: The `mixin` field:
		//   Type: Array/String
		//   Required: False

		if (json.has(MIXIN_KEY)) {
			JsonElement mixin = json.get(MIXIN_KEY);

			if (mixin.isJsonPrimitive()) {
				return List.of(mixin.getAsString());
			} else if (mixin.isJsonArray()) {
				List<String> mixinConfigs = new ArrayList<>();

				for (JsonElement child : mixin.getAsJsonArray()) {
					mixinConfigs.add(child.getAsString());
				}

				return mixinConfigs;
			} else {
				LOGGER.warn("'mixin' key in quilt.mod.json is of unexpected type {}", mixin.getClass());
			}
		}

		return List.of();
	}
}
