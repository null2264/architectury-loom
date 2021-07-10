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
		Dependency dependency = project.getDependencies().create(dependencyNotation);
		Configuration config = project.getConfigurations().detachedConfiguration(dependency);
		return config.fileCollection(dep -> true);
	}
}
