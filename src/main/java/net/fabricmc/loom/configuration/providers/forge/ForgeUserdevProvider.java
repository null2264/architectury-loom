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

package net.fabricmc.loom.configuration.providers.forge;

import java.io.File;
import java.io.Reader;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

import net.fabricmc.loom.configuration.DependencyProvider;
import net.fabricmc.loom.configuration.ide.RunConfigSettings;
import net.fabricmc.loom.configuration.launch.LaunchProviderSettings;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.DependencyDownloader;

public class ForgeUserdevProvider extends DependencyProvider {
	private File userdevJar;
	private JsonObject json;

	public ForgeUserdevProvider(Project project) {
		super(project);
	}

	@Override
	public void provide(DependencyInfo dependency, Consumer<Runnable> postPopulationScheduler) throws Exception {
		userdevJar = new File(getExtension().getProjectPersistentCache(), "forge-" + dependency.getDependency().getVersion() + "-userdev.jar");

		Path configJson = getExtension()
				.getProjectPersistentCache()
				.toPath()
				.resolve("forge-config-" + dependency.getDependency().getVersion() + ".json");

		if (!userdevJar.exists() || Files.notExists(configJson) || isRefreshDeps()) {
			File resolved = dependency.resolveFile().orElseThrow(() -> new RuntimeException("Could not resolve Forge userdev"));
			Files.copy(resolved.toPath(), userdevJar.toPath(), StandardCopyOption.REPLACE_EXISTING);

			try (FileSystem fs = FileSystems.newFileSystem(new URI("jar:" + resolved.toURI()), ImmutableMap.of("create", false))) {
				Files.copy(fs.getPath("config.json"), configJson, StandardCopyOption.REPLACE_EXISTING);
			}
		}

		try (Reader reader = Files.newBufferedReader(configJson)) {
			json = new Gson().fromJson(reader, JsonObject.class);
		}

		addDependency(json.get("mcp").getAsString(), Constants.Configurations.MCP_CONFIG);
		addDependency(json.get("mcp").getAsString(), Constants.Configurations.SRG);
		addDependency(json.get("universal").getAsString(), Constants.Configurations.FORGE_UNIVERSAL);

		for (JsonElement lib : json.get("libraries").getAsJsonArray()) {
			if (lib.getAsString().startsWith("org.spongepowered:mixin:")) {
				if (getExtension().useFabricMixin) {
					addDependency("net.fabricmc:sponge-mixin:0.8.2+build.24", Constants.Configurations.FORGE_DEPENDENCIES);
					continue;
				}
			}

			addDependency(lib.getAsString(), Constants.Configurations.FORGE_DEPENDENCIES);
		}

		// TODO: Read launch configs from the JSON too
		// TODO: Should I copy the patches from here as well?
		//       That'd require me to run the "MCP environment" fully up to merging.
		for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("runs").entrySet()) {
			LaunchProviderSettings launchSettings = getExtension().getLaunchConfigs().findByName(entry.getKey());
			RunConfigSettings settings = getExtension().getRunConfigs().findByName(entry.getKey());
			JsonObject value = entry.getValue().getAsJsonObject();

			launchSettings.evaluateLater(() -> {
				launchSettings.arg(filterAlreadySeen(StreamSupport.stream(value.getAsJsonArray("args").spliterator(), false)
						.map(JsonElement::getAsString)
						.collect(Collectors.toList())).stream()
						.map(this::processTemplates)
						.collect(Collectors.toList()));

				for (Map.Entry<String, JsonElement> props : value.getAsJsonObject("props").entrySet()) {
					String string = processTemplates(props.getValue().getAsString());

					settings.property(props.getKey(), string);
				}
			});

			settings.evaluateLater(() -> {
				settings.defaultMainClass(value.getAsJsonPrimitive("main").getAsString());
				settings.vmArgs(StreamSupport.stream(value.getAsJsonArray("jvmArgs").spliterator(), false)
						.map(JsonElement::getAsString)
						.map(this::processTemplates)
						.collect(Collectors.toList()));
			});
		}
	}

	private List<String> filterAlreadySeen(List<String> args) {
		String last = null;

		for (int i = 0; i < args.size(); i++) {
			String current = args.get(i);

			if (Objects.equals(last, "--assetIndex") || Objects.equals(last, "--assetsDir")) {
				args.remove(i - 1);
				args.remove(i - 1);
				i -= 2;
			}

			last = current;
		}

		return args;
	}

	public String processTemplates(String string) {
		if (string.startsWith("{")) {
			String key = string.substring(1, string.length() - 1);

			// TODO: Look into ways to not hardcode
			if (key.equals("runtime_classpath")) {
				Set<File> mcLibs = getProject().getConfigurations().getByName(Constants.Configurations.FORGE_DEPENDENCIES).resolve();
				mcLibs.addAll(getProject().getConfigurations().getByName(Constants.Configurations.MINECRAFT_NAMED).resolve());
				mcLibs.addAll(getProject().getConfigurations().getByName(Constants.Configurations.FORGE_NAMED).resolve());
				string = mcLibs.stream()
						.map(File::getAbsolutePath)
//						.filter(s -> s.contains("cpw") || s.contains("minecraftforge") || s.contains("jopt-simple") || s.contains("log4j") || s.contains("night-config"))
						.collect(Collectors.joining(File.pathSeparator));
			} else if (json.has(key)) {
				JsonElement element = json.get(key);

				if (element.isJsonArray()) {
					string = StreamSupport.stream(element.getAsJsonArray().spliterator(), false)
							.map(JsonElement::getAsString)
							.map(str -> {
								if (str.contains(":")) {
									return DependencyDownloader.download(getProject(), str, false).getFiles().stream()
											.map(File::getAbsolutePath)
											.skip(1)
											.collect(Collectors.joining(File.pathSeparator));
								}

								return str;
							})
							.collect(Collectors.joining(File.pathSeparator));
				} else {
					string = element.toString();
				}
			} else {
				getProject().getLogger().warn("Unrecognized template! " + string);
			}
		}

		return string;
	}

	public File getUserdevJar() {
		return userdevJar;
	}

	@Override
	public String getTargetConfig() {
		return Constants.Configurations.FORGE_USERDEV;
	}
}
