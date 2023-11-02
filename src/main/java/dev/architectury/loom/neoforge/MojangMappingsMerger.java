package dev.architectury.loom.neoforge;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import net.fabricmc.loom.api.mappings.layered.MappingContext;
import net.fabricmc.loom.api.mappings.layered.MappingLayer;
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.configuration.providers.mappings.LayeredMappingsProcessor;
import net.fabricmc.loom.configuration.providers.mappings.mojmap.MojangMappingsSpec;
import net.fabricmc.loom.util.ExceptionUtil;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.adapter.MappingNsCompleter;
import net.fabricmc.mappingio.adapter.MappingNsRenamer;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public final class MojangMappingsMerger {
	public static void mergeMojangMappings(MappingContext context, Path raw, Path merged) {
		try (MappingWriter writer = MappingWriter.create(merged, MappingFormat.TINY_2)) {
			final MemoryMappingTree mappingTree = mergeMojangMappings(context, raw);
			mappingTree.accept(writer);
		} catch (IOException e) {
			throw ExceptionUtil.createDescriptiveWrapper(UncheckedIOException::new, "Could not write Mojang-merged mappings", e);
		}
	}

	public static MemoryMappingTree mergeMojangMappings(MappingContext context, Path raw) {
		try {
			var processor = new LayeredMappingsProcessor(null);
			var inputLayer = new FileLayer(raw, MappingsNamespace.NAMED);
			var mojangLayer = new MojangMappingsSpec(true).createLayer(context);
			var renamedMojangLayer = new WrappedLayer(mojangLayer, next -> {
				Map<String, String> renames = Map.of(MappingsNamespace.NAMED.toString(), MappingsNamespace.MOJANG.toString());
				return new MappingNsRenamer(next, renames);
			});
			MemoryMappingTree incomplete = processor.getMappings(List.of(inputLayer, renamedMojangLayer));
			MemoryMappingTree result = new MemoryMappingTree();
			MappingVisitor visitor = new MappingSourceNsSwitch(result, MappingsNamespace.OFFICIAL.toString());
			// Run via intermediary first to drop any missing names.
			visitor = new MappingSourceNsSwitch(visitor, MappingsNamespace.INTERMEDIARY.toString(), true);
			Map<String, String> toComplete = Map.of(MappingsNamespace.MOJANG.toString(), MappingsNamespace.OFFICIAL.toString());
			visitor = new MappingNsCompleter(visitor, toComplete);
			incomplete.accept(visitor);
			return result;
		} catch (IOException e) {
			throw ExceptionUtil.createDescriptiveWrapper(UncheckedIOException::new, "Could not merge Mojang mappings", e);
		}
	}

	private record FileLayer(Path input, MappingsNamespace mergeNamespace) implements MappingLayer {
		@Override
		public void visit(MappingVisitor mappingVisitor) throws IOException {
			MappingReader.read(input, mappingVisitor);
		}

		@Override
		public MappingsNamespace getSourceNamespace() {
			return mergeNamespace;
		}
	}

	private record WrappedLayer(MappingLayer layer, UnaryOperator<MappingVisitor> visitorWrapper) implements MappingLayer {
		@Override
		public void visit(MappingVisitor mappingVisitor) throws IOException {
			layer.visit(visitorWrapper.apply(mappingVisitor));
		}

		@Override
		public MappingsNamespace getSourceNamespace() {
			return layer.getSourceNamespace();
		}
	}
}
