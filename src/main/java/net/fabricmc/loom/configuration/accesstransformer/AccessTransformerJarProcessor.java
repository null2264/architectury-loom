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

package net.fabricmc.loom.configuration.accesstransformer;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import org.apache.commons.io.output.NullOutputStream;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.configuration.ShowStacktrace;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.configuration.processors.JarProcessor;
import net.fabricmc.loom.util.Checksum;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.DependencyDownloader;

public final class AccessTransformerJarProcessor implements JarProcessor {
	private final Project project;
	private byte[] atHash;
	private final Set<File> atFiles;

	public AccessTransformerJarProcessor(Project project, Set<File> atFiles) {
		this.project = project;
		this.atFiles = atFiles;
	}

	public static Set<File> getAccessTransformerFiles(Project project) {
		final Set<File> atFiles = new HashSet<>();
		LoomGradleExtension extension = LoomGradleExtension.get(project);
		ConfigurableFileCollection accessTransformers = extension.getForge().getAccessTransformers();
		accessTransformers.finalizeValue();
		atFiles.addAll(accessTransformers.getFiles());

		if (atFiles.isEmpty()) {
			SourceSet main = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().getByName("main");

			for (File srcDir : main.getResources().getSrcDirs()) {
				File projectAt = new File(srcDir, Constants.Forge.ACCESS_TRANSFORMER_PATH);

				if (projectAt.exists()) {
					atFiles.add(projectAt);
					break;
				}
			}
		}

		return atFiles;
	}

	@Override
	public String getId() {
		return "architectury:access_transformer:" + Checksum.toHex(atHash);
	}

	@Override
	public void setup() {
		atHash = getProjectAtsHash();
	}

	private byte[] getProjectAtsHash() {
		try {
			if (atFiles.isEmpty()) return ByteSource.empty().hash(Hashing.sha256()).asBytes();
			List<ByteSource> currentBytes = new ArrayList<>();

			for (File projectAt : atFiles) {
				currentBytes.add(com.google.common.io.Files.asByteSource(projectAt));
			}

			return ByteSource.concat(currentBytes).hash(Hashing.sha256()).asBytes();
		} catch (IOException e) {
			throw new UncheckedIOException("Could not compute project AT hash", e);
		}
	}

	@Override
	public void process(File file) {
		try {
			Path tempInput = Files.createTempFile(null, "loom-at.jar");
			Files.copy(file.toPath(), tempInput);

			executeAt(project, tempInput, file.toPath(), args -> {
				for (File atFile : atFiles) {
					args.add("--atFile");
					args.add(atFile.getAbsolutePath());
				}
			});

			Files.delete(tempInput);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not access transform " + file.getAbsolutePath(), e);
		}
	}

	public static void executeAt(Project project, Path input, Path output, AccessTransformerConfiguration configuration) throws IOException {
		boolean serverBundleMetadataPresent = LoomGradleExtension.get(project).getMinecraftProvider().getServerBundleMetadata() != null;
		String atDependency = Constants.Dependencies.ACCESS_TRANSFORMERS + (serverBundleMetadataPresent ? Constants.Dependencies.Versions.ACCESS_TRANSFORMERS_NEW : Constants.Dependencies.Versions.ACCESS_TRANSFORMERS);
		FileCollection classpath = DependencyDownloader.download(project, atDependency);
		List<String> args = new ArrayList<>();
		args.add("--inJar");
		args.add(input.toAbsolutePath().toString());
		args.add("--outJar");
		args.add(output.toAbsolutePath().toString());

		configuration.apply(args);

		project.javaexec(spec -> {
			spec.getMainClass().set("net.minecraftforge.accesstransformer.TransformerProcessor");
			spec.setArgs(args);
			spec.setClasspath(classpath);

			// if running with INFO or DEBUG logging
			if (project.getGradle().getStartParameter().getShowStacktrace() != ShowStacktrace.INTERNAL_EXCEPTIONS
					|| project.getGradle().getStartParameter().getLogLevel().compareTo(LogLevel.LIFECYCLE) < 0) {
				spec.setStandardOutput(System.out);
				spec.setErrorOutput(System.err);
			} else {
				spec.setStandardOutput(NullOutputStream.NULL_OUTPUT_STREAM);
				spec.setErrorOutput(NullOutputStream.NULL_OUTPUT_STREAM);
			}
		}).rethrowFailure().assertNormalExitValue();
	}

	@FunctionalInterface
	public interface AccessTransformerConfiguration {
		void apply(List<String> args) throws IOException;
	}
}
