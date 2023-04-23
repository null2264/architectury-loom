/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016-2023 FabricMC
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

package net.fabricmc.loom.configuration;

import static net.fabricmc.loom.util.Constants.Configurations;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.AbstractCopyTask;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.api.InterfaceInjectionExtensionAPI;
import net.fabricmc.loom.build.mixin.GroovyApInvoker;
import net.fabricmc.loom.build.mixin.JavaApInvoker;
import net.fabricmc.loom.build.mixin.KaptApInvoker;
import net.fabricmc.loom.build.mixin.ScalaApInvoker;
import net.fabricmc.loom.configuration.accesstransformer.AccessTransformerJarProcessor;
import net.fabricmc.loom.configuration.accesswidener.AccessWidenerJarProcessor;
import net.fabricmc.loom.configuration.ifaceinject.InterfaceInjectionProcessor;
import net.fabricmc.loom.configuration.processors.MinecraftJarProcessorManager;
import net.fabricmc.loom.configuration.processors.ModJavadocProcessor;
import net.fabricmc.loom.configuration.providers.forge.DependencyProviders;
import net.fabricmc.loom.configuration.providers.forge.ForgeLibrariesProvider;
import net.fabricmc.loom.configuration.providers.forge.ForgeProvider;
import net.fabricmc.loom.configuration.providers.forge.ForgeRunsProvider;
import net.fabricmc.loom.configuration.providers.forge.ForgeUniversalProvider;
import net.fabricmc.loom.configuration.providers.forge.ForgeUserdevProvider;
import net.fabricmc.loom.configuration.providers.forge.PatchProvider;
import net.fabricmc.loom.configuration.providers.forge.SrgProvider;
import net.fabricmc.loom.configuration.providers.forge.mcpconfig.McpConfigProvider;
import net.fabricmc.loom.configuration.providers.forge.minecraft.ForgeMinecraftProvider;
import net.fabricmc.loom.configuration.providers.mappings.MappingConfiguration;
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftJarConfiguration;
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftProvider;
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftSourceSets;
import net.fabricmc.loom.configuration.providers.minecraft.mapped.IntermediaryMinecraftProvider;
import net.fabricmc.loom.configuration.providers.minecraft.mapped.NamedMinecraftProvider;
import net.fabricmc.loom.configuration.providers.minecraft.mapped.SrgMinecraftProvider;
import net.fabricmc.loom.configuration.sources.ForgeSourcesRemapper;
import net.fabricmc.loom.extension.MixinExtension;
import net.fabricmc.loom.util.Checksum;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.ExceptionUtil;
import net.fabricmc.loom.util.OperatingSystem;
import net.fabricmc.loom.util.gradle.GradleUtils;
import net.fabricmc.loom.util.gradle.SourceSetHelper;
import net.fabricmc.loom.util.service.ScopedSharedServiceManager;
import net.fabricmc.loom.util.service.SharedServiceManager;

public final class CompileConfiguration {
	private final Project project;
	private final ConfigurationContainer configurations;

	public CompileConfiguration(Project project) {
		this.project = project;
		this.configurations = project.getConfigurations();
	}

	public void setupConfigurations() {
		final LoomGradleExtension extension = LoomGradleExtension.get(project);

		project.afterEvaluate(project1 -> {
			if (extension.shouldGenerateSrgTiny()) {
				configurations.register(Constants.Configurations.SRG, configuration -> configuration.setTransitive(false));
			}

			if (extension.isDataGenEnabled()) {
				project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().getByName("main").resources(files -> {
					files.srcDir(project.file("src/generated/resources"));
				});
			}
		});

		configurations.register(Configurations.MOD_COMPILE_CLASSPATH);
		createNonTransitive(Configurations.MOD_COMPILE_CLASSPATH_MAPPED);

		// Set up the Minecraft compile configurations.
		var minecraftClientCompile = createNonTransitive(Configurations.MINECRAFT_CLIENT_COMPILE_LIBRARIES);
		var minecraftServerCompile = createNonTransitive(Configurations.MINECRAFT_SERVER_COMPILE_LIBRARIES);
		var minecraftCompile = createNonTransitive(Configurations.MINECRAFT_COMPILE_LIBRARIES);
		minecraftCompile.configure(configuration -> {
			configuration.extendsFrom(minecraftClientCompile.get());
			configuration.extendsFrom(minecraftServerCompile.get());
		});

		// Set up the minecraft runtime configurations, this extends from the compile configurations.
		var minecraftClientRuntime = createNonTransitive(Configurations.MINECRAFT_CLIENT_RUNTIME_LIBRARIES);
		var minecraftServerRuntime = createNonTransitive(Configurations.MINECRAFT_SERVER_RUNTIME_LIBRARIES);

		// Runtime extends from compile
		minecraftClientRuntime.configure(configuration -> configuration.extendsFrom(minecraftClientCompile.get()));
		minecraftServerRuntime.configure(configuration -> configuration.extendsFrom(minecraftServerCompile.get()));

		createNonTransitive(Configurations.MINECRAFT_RUNTIME_LIBRARIES).configure(minecraftRuntime -> {
			minecraftRuntime.extendsFrom(minecraftClientRuntime.get());
			minecraftRuntime.extendsFrom(minecraftServerRuntime.get());
		});

		createNonTransitive(Configurations.MINECRAFT_NATIVES);
		createNonTransitive(Configurations.LOADER_DEPENDENCIES);

		createNonTransitive(Configurations.MINECRAFT);
		createNonTransitive(Configurations.INCLUDE);
		createNonTransitive(Configurations.MAPPING_CONSTANTS);

		configurations.register(Configurations.NAMED_ELEMENTS, configuration -> {
			configuration.setCanBeConsumed(true);
			configuration.setCanBeResolved(false);
			configuration.extendsFrom(configurations.getByName(JavaPlugin.API_CONFIGURATION_NAME));
		});

		extendsFrom(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, Configurations.MAPPING_CONSTANTS);

		if (extension.isForge()) {
			createNonTransitive(Constants.Configurations.FORGE);
			createNonTransitive(Constants.Configurations.FORGE_USERDEV);
			createNonTransitive(Constants.Configurations.FORGE_INSTALLER);
			createNonTransitive(Constants.Configurations.FORGE_UNIVERSAL);
			configurations.register(Constants.Configurations.FORGE_DEPENDENCIES);
			createNonTransitive(Constants.Configurations.FORGE_NAMED);
			createNonTransitive(Constants.Configurations.FORGE_EXTRA);
			createNonTransitive(Constants.Configurations.MCP_CONFIG);
			configurations.register(Constants.Configurations.FORGE_RUNTIME_LIBRARY).configure(configuration -> {
				// Resolve for runtime usage
				Usage javaRuntime = project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME);
				configuration.attributes(attributes -> attributes.attribute(Usage.USAGE_ATTRIBUTE, javaRuntime));
			});

			extendsFrom(Constants.Configurations.MINECRAFT_SERVER_DEPENDENCIES, Constants.Configurations.FORGE_DEPENDENCIES);

			extendsFrom(Constants.Configurations.FORGE_RUNTIME_LIBRARY, Constants.Configurations.FORGE_DEPENDENCIES);
			extendsFrom(Constants.Configurations.FORGE_RUNTIME_LIBRARY, Constants.Configurations.MINECRAFT_DEPENDENCIES);
			extendsFrom(Constants.Configurations.FORGE_RUNTIME_LIBRARY, Constants.Configurations.FORGE_EXTRA);
			extendsFrom(Constants.Configurations.FORGE_RUNTIME_LIBRARY, Constants.Configurations.FORGE_NAMED);
			// Include any user-defined libraries on the runtime CP.
			// (All the other superconfigurations are already on there.)
			extendsFrom(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME, Constants.Configurations.FORGE_RUNTIME_LIBRARY);

			extendsFrom(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME, Constants.Configurations.FORGE_NAMED);
			extendsFrom(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME, Constants.Configurations.FORGE_NAMED);
			extendsFrom(JavaPlugin.TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME, Constants.Configurations.FORGE_NAMED);
			extendsFrom(JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME, Constants.Configurations.FORGE_NAMED);
			extendsFrom(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME, Constants.Configurations.FORGE_EXTRA);
			extendsFrom(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME, Constants.Configurations.FORGE_EXTRA);
			extendsFrom(JavaPlugin.TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME, Constants.Configurations.FORGE_EXTRA);
			extendsFrom(JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME, Constants.Configurations.FORGE_EXTRA);
		}

		configurations.register(Configurations.MAPPINGS);
		configurations.register(Configurations.MAPPINGS_FINAL);
		configurations.register(Configurations.LOOM_DEVELOPMENT_DEPENDENCIES);
		configurations.register(Configurations.UNPICK_CLASSPATH);
		configurations.register(Configurations.LOCAL_RUNTIME);
		extendsFrom(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME, Configurations.LOCAL_RUNTIME);

		extension.createRemapConfigurations(SourceSetHelper.getMainSourceSet(project));

		extendsFrom(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME, Configurations.MAPPINGS_FINAL);
		extendsFrom(JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME, Configurations.MAPPINGS_FINAL);

		extendsFrom(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME, Configurations.MINECRAFT_RUNTIME_LIBRARIES);

		// Add the dev time dependencies
		project.getDependencies().add(Configurations.LOOM_DEVELOPMENT_DEPENDENCIES, Constants.Dependencies.DEV_LAUNCH_INJECTOR + Constants.Dependencies.Versions.DEV_LAUNCH_INJECTOR);
		project.getDependencies().add(Configurations.LOOM_DEVELOPMENT_DEPENDENCIES, Constants.Dependencies.TERMINAL_CONSOLE_APPENDER + Constants.Dependencies.Versions.TERMINAL_CONSOLE_APPENDER);
		project.getDependencies().add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, Constants.Dependencies.JETBRAINS_ANNOTATIONS + Constants.Dependencies.Versions.JETBRAINS_ANNOTATIONS);
		project.getDependencies().add(JavaPlugin.TEST_COMPILE_ONLY_CONFIGURATION_NAME, Constants.Dependencies.JETBRAINS_ANNOTATIONS + Constants.Dependencies.Versions.JETBRAINS_ANNOTATIONS);

		if (extension.isForge()) {
			project.getDependencies().add(Constants.Configurations.FORGE_EXTRA, Constants.Dependencies.FORGE_RUNTIME + Constants.Dependencies.Versions.FORGE_RUNTIME);
			project.getDependencies().add(Constants.Configurations.FORGE_EXTRA, Constants.Dependencies.UNPROTECT + Constants.Dependencies.Versions.UNPROTECT);
			project.getDependencies().add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, Constants.Dependencies.JAVAX_ANNOTATIONS + Constants.Dependencies.Versions.JAVAX_ANNOTATIONS);
		}
	}

	private NamedDomainObjectProvider<Configuration> createNonTransitive(String name) {
		return configurations.register(name, configuration -> configuration.setTransitive(false));
	}

	public void configureCompile() {
		LoomGradleExtension extension = LoomGradleExtension.get(project);

		project.getTasks().named(JavaPlugin.JAVADOC_TASK_NAME, Javadoc.class).configure(javadoc -> {
			final SourceSet main = SourceSetHelper.getMainSourceSet(project);
			javadoc.setClasspath(main.getOutput().plus(main.getCompileClasspath()));
		});

		afterEvaluationWithService(project, (serviceManager) -> {
			final ConfigContext configContext = new ConfigContextImpl(project, serviceManager, extension);

			MinecraftSourceSets.get(project).afterEvaluate(project);

			final boolean previousRefreshDeps = extension.refreshDeps();

			if (getAndLock()) {
				project.getLogger().lifecycle("Found existing cache lock file, rebuilding loom cache. This may have been caused by a failed or canceled build.");
				extension.setRefreshDeps(true);
			}

			try {
				setupMinecraft(configContext);
			} catch (Exception e) {
				throw ExceptionUtil.createDescriptiveWrapper(RuntimeException::new, "Failed to setup Minecraft", e);
			}

			LoomDependencyManager dependencyManager = new LoomDependencyManager();
			extension.setDependencyManager(dependencyManager);
			dependencyManager.handleDependencies(project, serviceManager);

			releaseLock();
			extension.setRefreshDeps(previousRefreshDeps);

			MixinExtension mixin = LoomGradleExtension.get(project).getMixin();

			if (mixin.getUseLegacyMixinAp().get()) {
				setupMixinAp(mixin);
			}

			configureDecompileTasks(configContext);

			if (extension.isForge()) {
				// (As of 0.12.0) Needs to be extended here since the source set is only created in aE.
				extendsFrom(Constants.Configurations.FORGE_RUNTIME_LIBRARY, MinecraftSourceSets.get(project).getCombinedSourceSetName(), project);

				// TODO: Find a better place for this?
				//   This has to be after dependencyManager.handleDependencies() above
				//   because of https://github.com/architectury/architectury-loom/issues/72.
				if (!OperatingSystem.isCIBuild()) {
					try {
						ForgeSourcesRemapper.addBaseForgeSources(project);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});

		finalizedBy("idea", "genIdeaWorkspace");
		finalizedBy("eclipse", "genEclipseRuns");
		finalizedBy("cleanEclipse", "cleanEclipseRuns");

		// Add the "dev" jar to the "namedElements" configuration
		project.artifacts(artifactHandler -> artifactHandler.add(Configurations.NAMED_ELEMENTS, project.getTasks().named("jar")));

		// Ensure that the encoding is set to UTF-8, no matter what the system default is
		// this fixes some edge cases with special characters not displaying correctly
		// see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
		project.getTasks().withType(AbstractCopyTask.class).configureEach(abstractCopyTask -> abstractCopyTask.setFilteringCharset(StandardCharsets.UTF_8.name()));
		project.getTasks().withType(JavaCompile.class).configureEach(javaCompile -> javaCompile.getOptions().setEncoding(StandardCharsets.UTF_8.name()));

		if (extension.isForge()) {
			// Create default mod from main source set
			extension.mods(mods -> {
				final SourceSet main = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
				mods.create("main").sourceSet(main);
			});
		}

		if (project.getPluginManager().hasPlugin("org.jetbrains.kotlin.kapt")) {
			// If loom is applied after kapt, then kapt will use the AP arguments too early for loom to pass the arguments we need for mixin.
			throw new IllegalArgumentException("fabric-loom must be applied BEFORE kapt in the plugins { } block.");
		}
	}

	// This is not thread safe across projects synchronize it here just to be sure, might be possible to move this further down, but for now this will do.
	private synchronized void setupMinecraft(ConfigContext configContext) throws Exception {
		final Project project = configContext.project();
		final LoomGradleExtension extension = configContext.extension();
		final MinecraftJarConfiguration jarConfiguration = extension.getMinecraftJarConfiguration().get();

		// Provide the vanilla mc jars -- TODO share across projects.
		final MinecraftProvider minecraftProvider = jarConfiguration.getMinecraftProviderFunction().apply(configContext);

		if (extension.isForge() && !(minecraftProvider instanceof ForgeMinecraftProvider)) {
			throw new UnsupportedOperationException("Using Forge with split jars is not supported!");
		}

		extension.setMinecraftProvider(minecraftProvider);
		minecraftProvider.provideFirst();

		// This needs to run after MinecraftProvider.initFiles
		// but before MinecraftPatchedProvider.provide.
		setupDependencyProviders(project, extension);

		if (!extension.isForge()) {
			minecraftProvider.provide();
		}

		final DependencyInfo mappingsDep = DependencyInfo.create(project, Configurations.MAPPINGS);
		final MappingConfiguration mappingConfiguration = MappingConfiguration.create(project, configContext.serviceManager(), mappingsDep, minecraftProvider);
		extension.setMappingConfiguration(mappingConfiguration);

		if (extension.isForge()) {
			ForgeLibrariesProvider.provide(mappingConfiguration, project);
			minecraftProvider.provide();
		}

		mappingConfiguration.setupPost(project);
		mappingConfiguration.applyToProject(project, mappingsDep);

		if (extension.isForge()) {
			extension.setForgeRunsProvider(ForgeRunsProvider.create(project));
		}

		if (minecraftProvider instanceof ForgeMinecraftProvider patched) {
			patched.getPatchedProvider().remapJar();
		}

		// Provide the remapped mc jars
		final IntermediaryMinecraftProvider<?> intermediaryMinecraftProvider = jarConfiguration.getIntermediaryMinecraftProviderBiFunction().apply(configContext, minecraftProvider);
		NamedMinecraftProvider<?> namedMinecraftProvider = jarConfiguration.getNamedMinecraftProviderBiFunction().apply(configContext, minecraftProvider);

		registerGameProcessors(configContext);
		MinecraftJarProcessorManager minecraftJarProcessorManager = MinecraftJarProcessorManager.create(project);

		if (minecraftJarProcessorManager != null) {
			// Wrap the named MC provider for one that will provide the processed jars
			namedMinecraftProvider = jarConfiguration.getProcessedNamedMinecraftProviderBiFunction().apply(namedMinecraftProvider, minecraftJarProcessorManager);
		}

		extension.setIntermediaryMinecraftProvider(intermediaryMinecraftProvider);
		intermediaryMinecraftProvider.provide(true);

		extension.setNamedMinecraftProvider(namedMinecraftProvider);
		namedMinecraftProvider.provide(true);

		if (extension.isForge()) {
			final SrgMinecraftProvider<?> srgMinecraftProvider = jarConfiguration.getSrgMinecraftProviderBiFunction().apply(configContext, minecraftProvider);
			extension.setSrgMinecraftProvider(srgMinecraftProvider);
			srgMinecraftProvider.provide(true);
		}
	}

	private void registerGameProcessors(ConfigContext configContext) {
		final LoomGradleExtension extension = configContext.extension();

		final boolean enableTransitiveAccessWideners = extension.getEnableTransitiveAccessWideners().get();
		extension.addMinecraftJarProcessor(AccessWidenerJarProcessor.class, "fabric-loom:access-widener", enableTransitiveAccessWideners, extension.getAccessWidenerPath());

		if (extension.getEnableModProvidedJavadoc().get()) {
			extension.addMinecraftJarProcessor(ModJavadocProcessor.class, "fabric-loom:mod-javadoc");
		}

		final InterfaceInjectionExtensionAPI interfaceInjection = extension.getInterfaceInjection();

		if (interfaceInjection.isEnabled()) {
			extension.addMinecraftJarProcessor(InterfaceInjectionProcessor.class, "fabric-loom:interface-inject", interfaceInjection.getEnableDependencyInterfaceInjection().get());
		}

		if (extension.isForge()) {
			extension.addMinecraftJarProcessor(AccessTransformerJarProcessor.class, "loom:access-transformer", configContext.project(), extension.getForge().getAccessTransformers());
		}
	}

	private void setupMixinAp(MixinExtension mixin) {
		mixin.init();

		// Disable some things used by log4j via the mixin AP that prevent it from being garbage collected
		System.setProperty("log4j2.disable.jmx", "true");
		System.setProperty("log4j.shutdownHookEnabled", "false");
		System.setProperty("log4j.skipJansi", "true");

		project.getLogger().info("Configuring compiler arguments for Java");

		new JavaApInvoker(project).configureMixin();

		if (project.getPluginManager().hasPlugin("scala")) {
			project.getLogger().info("Configuring compiler arguments for Scala");
			new ScalaApInvoker(project).configureMixin();
		}

		if (project.getPluginManager().hasPlugin("org.jetbrains.kotlin.kapt")) {
			project.getLogger().info("Configuring compiler arguments for Kapt plugin");
			new KaptApInvoker(project).configureMixin();
		}

		if (project.getPluginManager().hasPlugin("groovy")) {
			project.getLogger().info("Configuring compiler arguments for Groovy");
			new GroovyApInvoker(project).configureMixin();
		}
	}

	private void configureDecompileTasks(ConfigContext configContext) {
		final LoomGradleExtension extension = configContext.extension();

		extension.getMinecraftJarConfiguration().get().getDecompileConfigurationBiFunction()
				.apply(configContext, extension.getNamedMinecraftProvider()).afterEvaluation();
	}

	private Path getLockFile() {
		final LoomGradleExtension extension = LoomGradleExtension.get(project);
		final Path cacheDirectory = extension.getFiles().getUserCache().toPath();
		final String pathHash = Checksum.projectHash(project);
		return cacheDirectory.resolve("." + pathHash + ".lock");
	}

	private boolean getAndLock() {
		final Path lock = getLockFile();

		if (Files.exists(lock)) {
			return true;
		}

		try {
			Files.createFile(lock);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to acquire project configuration lock", e);
		}

		return false;
	}

	private void releaseLock() {
		final Path lock = getLockFile();

		if (!Files.exists(lock)) {
			return;
		}

		try {
			Files.delete(lock);
		} catch (IOException e1) {
			try {
				// If we failed to delete the lock file, moving it before trying to delete it may help.
				final Path del = lock.resolveSibling(lock.getFileName() + ".del");
				Files.move(lock, del);
				Files.delete(del);
			} catch (IOException e2) {
				var exception = new UncheckedIOException("Failed to release project configuration lock", e2);
				exception.addSuppressed(e1);
				throw exception;
			}
		}
	}

	public void extendsFrom(String a, String b) {
		project.getConfigurations().getByName(a, configuration -> configuration.extendsFrom(project.getConfigurations().getByName(b)));
	}

	private void finalizedBy(String a, String b) {
		project.getTasks().named(a).configure(task -> task.finalizedBy(project.getTasks().named(b)));
	}

	public static void setupDependencyProviders(Project project, LoomGradleExtension extension) {
		DependencyProviders dependencyProviders = new DependencyProviders();
		extension.setDependencyProviders(dependencyProviders);

		if (extension.isForge()) {
			dependencyProviders.addProvider(new ForgeProvider(project));
			dependencyProviders.addProvider(new ForgeUserdevProvider(project));
		}

		if (extension.shouldGenerateSrgTiny()) {
			dependencyProviders.addProvider(new SrgProvider(project));
		}

		if (extension.isForge()) {
			dependencyProviders.addProvider(new McpConfigProvider(project));
			dependencyProviders.addProvider(new PatchProvider(project));
			dependencyProviders.addProvider(new ForgeUniversalProvider(project));
		}

		dependencyProviders.handleDependencies(project);
	}

	private static void afterEvaluationWithService(Project project, Consumer<SharedServiceManager> consumer) {
		GradleUtils.afterSuccessfulEvaluation(project, () -> {
			try (var serviceManager = new ScopedSharedServiceManager()) {
				consumer.accept(serviceManager);
			}
		});
	}
}
