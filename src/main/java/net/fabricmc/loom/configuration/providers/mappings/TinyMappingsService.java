/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022 FabricMC
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

package net.fabricmc.loom.configuration.providers.mappings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;

import net.fabricmc.loom.util.service.SharedService;
import net.fabricmc.loom.util.service.SharedServiceManager;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public final class TinyMappingsService implements SharedService {
	private final MemoryMappingTree mappingTree;
	private final Supplier<MemoryMappingTree> mappingTreeWithSrg;

	public TinyMappingsService(Path tinyMappings, Path tinyMappingsWithSrg) {
		try {
			this.mappingTree = new MemoryMappingTree();
			MappingReader.read(tinyMappings, mappingTree);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read mappings", e);
		}

		this.mappingTreeWithSrg = Suppliers.memoize(() -> {
			try {
				MemoryMappingTree tree = new MemoryMappingTree();
				MappingReader.read(tinyMappingsWithSrg, tree);
				return tree;
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to read mappings", e);
			}
		});
	}

	public static synchronized TinyMappingsService create(SharedServiceManager serviceManager, Path tinyMappings, Path tinyMappingsWithSrg) {
		return serviceManager.getOrCreateService("TinyMappingsService:" + tinyMappings.toAbsolutePath(), () -> new TinyMappingsService(tinyMappings, tinyMappingsWithSrg));
	}

	public MemoryMappingTree getMappingTree() {
		return mappingTree;
	}

	public MemoryMappingTree getMappingTreeWithSrg() {
		return mappingTreeWithSrg.get();
	}
}
