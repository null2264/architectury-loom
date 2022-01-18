package net.fabricmc.loom.configuration.providers.forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;

import net.fabricmc.loom.configuration.DependencyInfo;

public class DependencyProviders {
	private static class ProviderList {
		private final String key;
		private final List<DependencyProvider> providers = new ArrayList<>();

		ProviderList(String key) {
			this.key = key;
		}
	}

	private final List<DependencyProvider> dependencyProviderList = new ArrayList<>();

	public <T extends DependencyProvider> T addProvider(T provider) {
		if (dependencyProviderList.contains(provider)) {
			throw new RuntimeException("Provider is already registered");
		}

		if (getProvider(provider.getClass()) != null) {
			throw new RuntimeException("Provider of this type is already registered");
		}

		dependencyProviderList.add(provider);
		return provider;
	}

	public <T> T getProvider(Class<T> clazz) {
		for (DependencyProvider provider : dependencyProviderList) {
			if (provider.getClass() == clazz) {
				return (T) provider;
			}
		}

		return null;
	}

	public void handleDependencies(Project project) {
		List<Runnable> afterTasks = new ArrayList<>();

		project.getLogger().info(":setting up loom dependencies");
		Map<String, ProviderList> providerListMap = new HashMap<>();
		List<ProviderList> targetProviders = new ArrayList<>();

		for (DependencyProvider provider : dependencyProviderList) {
			providerListMap.computeIfAbsent(provider.getTargetConfig(), (k) -> {
				ProviderList list = new ProviderList(k);
				targetProviders.add(list);
				return list;
			}).providers.add(provider);
		}

		for (ProviderList list : targetProviders) {
			Configuration configuration = project.getConfigurations().getByName(list.key);
			DependencySet dependencies = configuration.getDependencies();

			if (dependencies.isEmpty()) {
				throw new IllegalArgumentException(String.format("No '%s' dependency was specified!", list.key));
			}

			if (dependencies.size() > 1) {
				throw new IllegalArgumentException(String.format("Only one '%s' dependency should be specified, but %d were!",
						list.key,
						dependencies.size())
				);
			}

			for (Dependency dependency : dependencies) {
				for (DependencyProvider provider : list.providers) {
					DependencyInfo info = DependencyInfo.create(project, dependency, configuration);

					try {
						provider.provide(info, afterTasks::add);
					} catch (Exception e) {
						throw new RuntimeException("Failed to provide " + dependency.getGroup() + ":" + dependency.getName() + ":" + dependency.getVersion() + " : " + e.toString(), e);
					}
				}
			}
		}

		for (Runnable runnable : afterTasks) {
			runnable.run();
		}
	}
}
