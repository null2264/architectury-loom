package dev.architectury.loom.metadata;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

public interface JsonBackedModMetadataFile extends ModMetadataFile {
	JsonObject getJson();

	/**
	 * Gets a custom value from this mod metadata file.
	 *
	 * @param key the key of the custom value
	 * @return the custom value, or {@code null} if not found
	 */
	default @Nullable JsonElement getCustomValue(String key) {
		return null;
	}
}
