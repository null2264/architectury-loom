/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020-2023 FabricMC
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
import java.nio.file.Path;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.configuration.DependencyInfo;
import net.fabricmc.loom.util.Constants;

public class ForgeProvider extends DependencyProvider {
	private ForgeVersion version = new ForgeVersion(null);
	private File globalCache;
	private File projectCache;

	public ForgeProvider(Project project) {
		super(project);
	}

	@Override
	public void provide(DependencyInfo dependency) throws Exception {
		version = new ForgeVersion(dependency.getResolvedVersion());
		addDependency(dependency.getDepString() + ":userdev", Constants.Configurations.FORGE_USERDEV);
		addDependency(dependency.getDepString() + ":installer", Constants.Configurations.FORGE_INSTALLER);
	}

	public ForgeVersion getVersion() {
		return version;
	}

	public File getGlobalCache() {
		if (globalCache == null) {
			globalCache = getMinecraftProvider().dir("forge/" + version.getCombined());
			globalCache.mkdirs();
		}

		return globalCache;
	}

	public File getProjectCache() {
		if (projectCache == null) {
			projectCache = new File(getDirectories().getRootProjectPersistentCache(), getMinecraftProvider().minecraftVersion() + "/forge/" + getExtension().getForgeProvider().getVersion().getCombined() + "/project-" + getProject().getPath().replace(':', '@'));
			projectCache.mkdirs();
		}

		return projectCache;
	}

	@Override
	public String getTargetConfig() {
		return Constants.Configurations.FORGE;
	}

	/**
	 * Gets the full/combined Forge version <em>without</em> resolving dependencies
	 * that haven't been resolved yet.
	 *
	 * @param project the Gradle project
	 * @return the Forge version
	 */
	public static String getCombinedForgeVersion(Project project) {
		final LoomGradleExtension extension = LoomGradleExtension.get(project);

		if (extension.getDependencyProviders() != null) {
			return extension.getForgeProvider().getVersion().getCombined();
		}

		final Configuration configuration = project.getConfigurations().getByName(Constants.Configurations.FORGE);

		for (Dependency dependency : configuration.getDependencies()) {
			if (dependency.getVersion() != null) {
				return dependency.getVersion();
			}
		}

		throw new RuntimeException("Could not find Forge version. Searched " + configuration.getDependencies().size() + " dependencies");
	}

	/**
	 * {@return the Forge cache directory}
	 *
	 * @param project the project
	 * @param version the Forge version
	 */
	public static Path getForgeCache(Project project, String version) {
		return LoomGradleExtension.get(project).getMinecraftProvider()
				.dir("forge/" + version).toPath();
	}

	/**
	 * {@return the cache directory for the current Forge version}
	 *
	 * <p>This method is slower than {@link #getForgeCache} if you already know the Forge version.
	 *
	 * @param project the project
	 */
	public static Path getCurrentForgeCache(Project project) {
		return getForgeCache(project, getCombinedForgeVersion(project));
	}

	public static final class ForgeVersion {
		private final String combined;
		private final String minecraftVersion;
		private final String forgeVersion;

		public ForgeVersion(String combined) {
			this.combined = combined;

			if (combined == null) {
				this.minecraftVersion = "NO_VERSION";
				this.forgeVersion = "NO_VERSION";
				return;
			}

			int hyphenIndex = combined.indexOf('-');

			if (hyphenIndex != -1) {
				this.minecraftVersion = combined.substring(0, hyphenIndex);
				this.forgeVersion = combined.substring(hyphenIndex + 1);
			} else {
				this.minecraftVersion = "NO_VERSION";
				this.forgeVersion = combined;
			}
		}

		public String getCombined() {
			return combined;
		}

		public String getMinecraftVersion() {
			return minecraftVersion;
		}

		public String getForgeVersion() {
			return forgeVersion;
		}
	}
}
