package dev.architectury.loom.neoforge;

import net.fabricmc.loom.api.mappings.layered.MappingContext;
import net.fabricmc.loom.api.mappings.layered.MappingLayer;
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.configuration.providers.mappings.LayeredMappingsProcessor;
import net.fabricmc.loom.configuration.providers.mappings.mojmap.MojangMappingsSpec;
import net.fabricmc.loom.util.ExceptionUtil;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.adapter.MappingNsRenamer;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class MojangMappingsMerger {
	public static void mergeMojangMappings(MappingContext context, Path raw, Path merged) {
		try {
			var processor = new LayeredMappingsProcessor(null);
			var inputLayer = new FileLayer(raw, MappingsNamespace.NAMED);
			var mojangLayer = new MojangMappingsSpec(true).createLayer(context);
			var renamedMojangLayer = new WrappedLayer(mojangLayer, next -> {
				Map<String, String> renames = Map.of(MappingsNamespace.NAMED.toString(), MappingsNamespace.MOJANG.toString());
				return new MappingNsRenamer(next, renames);
			});
			MemoryMappingTree mappingTree = processor.getMappings(List.of(inputLayer, renamedMojangLayer));

			try (MappingWriter writer = MappingWriter.create(merged, MappingFormat.TINY_2)) {
				mappingTree.accept(writer);
			}
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
