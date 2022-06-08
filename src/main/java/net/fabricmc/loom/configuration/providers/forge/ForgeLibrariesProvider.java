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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.gson.JsonElement;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.configuration.providers.mappings.MappingsProviderImpl;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.FileSystemUtil;
import net.fabricmc.loom.util.PropertyUtil;
import net.fabricmc.loom.util.srg.RemapObjectHolderVisitor;
import net.fabricmc.loom.util.srg.SrgMerger;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public class ForgeLibrariesProvider {
	public static void provide(MappingsProviderImpl mappingsProvider, Project project) throws Exception {
		LoomGradleExtension extension = LoomGradleExtension.get(project);
		Attribute<String> transformed = Attribute.of("architectury-loom-forge-dependencies-transformed-3", String.class);
		String mappingsIdentifier = extension.getMappingsProvider().mappingsIdentifier;

		project.getDependencies().registerTransform(ALFDTransformAction.class, spec -> {
			spec.getFrom().attribute(transformed, "");
			spec.getTo().attribute(transformed, mappingsIdentifier);

			spec.getParameters().getMappingsIdentifier().set(mappingsIdentifier);

			Supplier<Path> mappings = Suppliers.memoize(() -> {
				try {
					SrgMerger.ExtraMappings extraMappings = SrgMerger.ExtraMappings.ofMojmapTsrg(MappingsProviderImpl.getMojmapSrgFileIfPossible(project));
					Path tempFile = Files.createTempFile(null, null);
					Files.deleteIfExists(tempFile);
					SrgMerger.mergeSrg(MappingsProviderImpl.getRawSrgFile(project), mappingsProvider.tinyMappings, tempFile, extraMappings, true);
					tempFile.toFile().deleteOnExit();
					return tempFile;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});

			spec.getParameters().getMappings().fileProvider(project.provider(() -> mappings.get().toFile()));
			spec.getParameters().getFromNamespace().set("srg");
			spec.getParameters().getToNamespace().set("named");
		});

		for (ArtifactTypeDefinition type : project.getDependencies().getArtifactTypes()) {
			type.getAttributes().attribute(transformed, "");
		}

		for (JsonElement lib : extension.getForgeUserdevProvider().getJson().get("libraries").getAsJsonArray()) {
			Dependency dep = null;

			if (lib.getAsString().startsWith("org.spongepowered:mixin:")) {
				if (PropertyUtil.getAndFinalize(extension.getForge().getUseCustomMixin())) {
					if (lib.getAsString().contains("0.8.2")) {
						dep = DependencyProvider.addDependency(project, "net.fabricmc:sponge-mixin:0.8.2+build.24", Constants.Configurations.FORGE_DEPENDENCIES);
					} else {
						dep = DependencyProvider.addDependency(project, "dev.architectury:mixin-patched" + lib.getAsString().substring(lib.getAsString().lastIndexOf(":")) + ".+", Constants.Configurations.FORGE_DEPENDENCIES);
					}
				}
			}

			if (dep == null) {
				dep = DependencyProvider.addDependency(project, lib.getAsString(), Constants.Configurations.FORGE_DEPENDENCIES);
			}

			if (lib.getAsString().split(":").length < 4) {
				((ModuleDependency) dep).attributes(attributes -> {
					attributes.attribute(transformed, mappingsIdentifier);
				});
			}
		}
	}

	public abstract static class ALFDTransformAction implements TransformAction<ALFDTransformParameters> {
		@InputArtifact
		public abstract Provider<FileSystemLocation> getInput();

		@Override
		public void transform(TransformOutputs outputs) {
			try {
				File input = getInput().get().getAsFile();
				//architectury-loom-forge-dependencies-transformed
				HashCode hash = Hashing.sha256().hashString(getParameters().getMappingsIdentifier().get(), StandardCharsets.UTF_8);
				File output = outputs.file("alfd-transformed-" + hash + "/" + input.getName());
				Files.copy(input.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);

				try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(output, false)) {
					Path path = fs.get().getPath("META-INF/services/cpw.mods.modlauncher.api.INameMappingService");
					Files.deleteIfExists(path);

					if (Files.exists(fs.get().getPath("net/minecraftforge/fml/common/asm/ObjectHolderDefinalize.class"))) {
						MemoryMappingTree mappings = new MemoryMappingTree();
						MappingReader.read(getParameters().getMappings().get().getAsFile().toPath(), mappings);
						RemapObjectHolderVisitor.remapObjectHolder(output.toPath(), "net.minecraftforge.fml.common.asm.ObjectHolderDefinalize", mappings, getParameters().getFromNamespace().get(), getParameters().getToNamespace().get());
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public interface ALFDTransformParameters extends TransformParameters {
		@InputFile
		RegularFileProperty getMappings();

		@Input
		Property<String> getMappingsIdentifier();

		@Input
		Property<String> getFromNamespace();

		@Input
		Property<String> getToNamespace();
	}
}
