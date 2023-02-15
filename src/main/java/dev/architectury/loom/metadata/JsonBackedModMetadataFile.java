package dev.architectury.loom.metadata;

import com.google.gson.JsonObject;

public interface JsonBackedModMetadataFile extends ModMetadataFile {
	JsonObject getJson();
}
