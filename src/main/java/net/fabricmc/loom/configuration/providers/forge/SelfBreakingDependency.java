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

package net.fabricmc.loom.configuration.providers.forge;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.SelfResolvingDependency;
import org.gradle.api.capabilities.Capability;
import org.gradle.api.internal.artifacts.dependencies.AbstractModuleDependency;
import org.gradle.api.tasks.TaskDependency;
import org.jetbrains.annotations.Nullable;

public class SelfBreakingDependency extends AbstractModuleDependency implements SelfResolvingDependency {
	public SelfBreakingDependency() {
		super(null);
	}

	@Override
	public Set<File> resolve() {
		throw new RuntimeException();
	}

	@Override
	public Set<File> resolve(boolean b) {
		throw new RuntimeException();
	}

	@Override
	public TaskDependency getBuildDependencies() {
		return task -> Collections.emptySet();
	}

	@Nullable
	@Override
	public String getGroup() {
		return "break";
	}

	@Override
	public String getName() {
		return "break";
	}

	@Nullable
	@Override
	public String getVersion() {
		return "break";
	}

	@Override
	public boolean contentEquals(Dependency dependency) {
		return false;
	}

	@Override
	public ModuleDependency copy() {
		return this;
	}

	@Override
	public List<Capability> getRequestedCapabilities() {
		return Collections.emptyList();
	}
}
