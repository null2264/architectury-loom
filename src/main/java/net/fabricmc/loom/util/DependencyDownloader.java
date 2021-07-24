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

package net.fabricmc.loom.util;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileCollection;

/**
 * Simplified dependency downloading.
 *
 * @author Juuz
 */
public final class DependencyDownloader {
	/**
	 * Resolves a dependency as well as its transitive dependencies into a {@link FileCollection}.
	 *
	 * @param project            the project needing these files
	 * @param dependencyNotation the dependency notation
	 * @return the resolved files
	 */
	public static FileCollection download(Project project, String dependencyNotation) {
		return download(project, dependencyNotation, false);
	}

	public static FileCollection download(Project project, String dependencyNotation, boolean resolve) {
		Dependency dependency = project.getDependencies().create(dependencyNotation);
		Configuration config = project.getConfigurations().detachedConfiguration(dependency);
		FileCollection files = config.fileCollection(dep -> true);

		if (resolve) {
			files = project.files(files.getFiles());
		}

		return files;
	}
}
