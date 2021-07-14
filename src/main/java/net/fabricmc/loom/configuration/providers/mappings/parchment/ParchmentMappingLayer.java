/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016, 2017, 2018 FabricMC
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

package net.fabricmc.loom.configuration.providers.mappings.parchment;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.fabricmc.loom.LoomGradlePlugin;
import net.fabricmc.loom.configuration.providers.mappings.MappingLayer;
import net.fabricmc.loom.configuration.providers.mappings.MappingNamespace;
import net.fabricmc.mappingio.MappingVisitor;

public class ParchmentMappingLayer implements MappingLayer {
	private final File parchmentFile;
	private final boolean removePrefix;

	public ParchmentMappingLayer(File parchmentFile, boolean removePrefix) {
		this.parchmentFile = parchmentFile;
		this.removePrefix = removePrefix;
	}

	public File parchmentFile() {
		return parchmentFile;
	}

	public boolean removePrefix() {
		return removePrefix;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		ParchmentMappingLayer that = (ParchmentMappingLayer) obj;
		return Objects.equals(this.parchmentFile, that.parchmentFile)
				&& this.removePrefix == that.removePrefix;
	}

	@Override
	public int hashCode() {
		return Objects.hash(parchmentFile, removePrefix);
	}

	@Override
	public String toString() {
		return "ParchmentMappingLayer["
				+ "parchmentFile=" + parchmentFile + ", "
				+ "removePrefix=" + removePrefix + ']';
	}

	private static final String PARCHMENT_DATA_FILE_NAME = "parchment.json";

	@Override
	public void visit(MappingVisitor mappingVisitor) throws IOException {
		ParchmentTreeV1 parchmentData = getParchmentData();

		if (removePrefix()) {
			mappingVisitor = new ParchmentPrefixStripingMappingVisitor(mappingVisitor);
		}

		parchmentData.visit(mappingVisitor, MappingNamespace.NAMED.stringValue());
	}

	private ParchmentTreeV1 getParchmentData() throws IOException {
		try (ZipFile zipFile = new ZipFile(parchmentFile())) {
			ZipEntry zipFileEntry = zipFile.getEntry(PARCHMENT_DATA_FILE_NAME);
			Objects.requireNonNull(zipFileEntry, String.format("Could not find %s in parchment data file", PARCHMENT_DATA_FILE_NAME));

			try (InputStreamReader reader = new InputStreamReader(zipFile.getInputStream(zipFileEntry))) {
				return LoomGradlePlugin.GSON.fromJson(reader, ParchmentTreeV1.class);
			}
		}
	}
}
