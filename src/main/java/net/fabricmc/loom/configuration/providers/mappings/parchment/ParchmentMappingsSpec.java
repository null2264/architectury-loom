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

import java.util.Objects;

import net.fabricmc.loom.configuration.providers.mappings.MappingContext;
import net.fabricmc.loom.configuration.providers.mappings.MappingsSpec;

public class ParchmentMappingsSpec implements MappingsSpec<ParchmentMappingLayer> {
	private final String mavenNotation;
	private final boolean removePrefix;

	public ParchmentMappingsSpec(String mavenNotation, boolean removePrefix) {
		this.mavenNotation = mavenNotation;
		this.removePrefix = removePrefix;
	}

	public String mavenNotation() {
		return mavenNotation;
	}

	public boolean removePrefix() {
		return removePrefix;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		ParchmentMappingsSpec that = (ParchmentMappingsSpec) obj;
		return Objects.equals(this.mavenNotation, that.mavenNotation)
				&& this.removePrefix == that.removePrefix;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mavenNotation, removePrefix);
	}

	@Override
	public String toString() {
		return "ParchmentMappingsSpec["
				+ "mavenNotation=" + mavenNotation + ", "
				+ "removePrefix=" + removePrefix + ']';
	}

	@Override
	public ParchmentMappingLayer createLayer(MappingContext context) {
		return new ParchmentMappingLayer(context.mavenFile(mavenNotation()), removePrefix());
	}
}
