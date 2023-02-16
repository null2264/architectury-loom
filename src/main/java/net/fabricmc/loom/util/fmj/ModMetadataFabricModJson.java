/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.fabricmc.loom.util.fmj;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.architectury.loom.metadata.JsonBackedModMetadataFile;
import dev.architectury.loom.metadata.ModMetadataFile;
import org.jetbrains.annotations.Nullable;

public final class ModMetadataFabricModJson extends FabricModJson {
	private final ModMetadataFile modMetadata;

	ModMetadataFabricModJson(ModMetadataFile modMetadata, FabricModJsonSource source) {
		super(getJsonForModMetadata(modMetadata), source);
		this.modMetadata = modMetadata;
	}

	private static JsonObject getJsonForModMetadata(ModMetadataFile modMetadata) {
		if (modMetadata instanceof JsonBackedModMetadataFile jsonBacked) {
			return jsonBacked.getJson();
		}

		return new JsonObject();
	}

	public ModMetadataFile getModMetadata() {
		return modMetadata;
	}

	@Override
	public String getId() {
		return Objects.requireNonNullElseGet(modMetadata.getId(), super::getId);
	}

	@Override
	public int getVersion() {
		throw new UnsupportedOperationException();
	}

	@Override
	public @Nullable JsonElement getCustom(String key) {
		// TODO 1.1 MERGE: Add support for quilt custom keys
		return null;
	}

	@Override
	public List<String> getMixinConfigurations() {
		return modMetadata.getMixinConfigs();
	}

	@Override
	public Map<String, ModEnvironment> getClassTweakers() {
		return modMetadata.getAccessWideners()
				.stream()
				.collect(Collectors.toMap(Function.identity(), path -> ModEnvironment.UNIVERSAL));
	}
}
