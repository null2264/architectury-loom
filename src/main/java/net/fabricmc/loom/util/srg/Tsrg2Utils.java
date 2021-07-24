package net.fabricmc.loom.util.srg;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.io.srg.tsrg.TSrgWriter;
import org.cadixdev.lorenz.model.ClassMapping;
import org.cadixdev.lorenz.model.MethodMapping;

import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor;
import net.fabricmc.mappingio.format.TsrgReader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public class Tsrg2Utils {
	public static void convert(Reader reader, Writer writer) throws IOException {
		writeTsrg(visitor -> {
			try {
				TsrgReader.read(reader, visitor);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}, "srg", false, writer);
	}

	public static void writeTsrg(Consumer<MappingVisitor> visitorConsumer, String dstNamespace, boolean applyParameterMappings, Writer writer) throws IOException {
		MappingSet set;

		try (MappingsIO2LorenzWriter lorenzWriter = new MappingsIO2LorenzWriter(dstNamespace, applyParameterMappings)) {
			visitorConsumer.accept(lorenzWriter);
			set = lorenzWriter.read();
		}

		try (TSrgWriter w = new TSrgWriter(writer)) {
			w.write(set);
		}
	}

	// TODO Move this elsewhere
	public static class MappingsIO2LorenzWriter extends ForwardingMappingVisitor implements MappingWriter {
		private final Object dstNamespaceUnresolved;
		private int dstNamespace;
		private boolean applyParameterMappings;

		public MappingsIO2LorenzWriter(int dstNamespace, boolean applyParameterMappings) {
			super(new MemoryMappingTree());
			this.dstNamespaceUnresolved = dstNamespace;
			this.applyParameterMappings = applyParameterMappings;
		}

		public MappingsIO2LorenzWriter(String dstNamespace, boolean applyParameterMappings) {
			super(new MemoryMappingTree());
			this.dstNamespaceUnresolved = dstNamespace;
			this.applyParameterMappings = applyParameterMappings;
		}

		@Override
		public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) {
			super.visitNamespaces(srcNamespace, dstNamespaces);
			this.dstNamespace = dstNamespaceUnresolved instanceof Integer ? (Integer) dstNamespaceUnresolved : dstNamespaces.indexOf((String) dstNamespaceUnresolved);
		}

		public MappingSet read() throws IOException {
			return this.read(MappingSet.create());
		}

		public MappingSet read(final MappingSet mappings) throws IOException {
			MappingTree tree = (MappingTree) next;

			for (MappingTree.ClassMapping aClass : tree.getClasses()) {
				ClassMapping<?, ?> lClass = mappings.getOrCreateClassMapping(aClass.getSrcName())
						.setDeobfuscatedName(aClass.getDstName(dstNamespace));

				for (MappingTree.FieldMapping aField : aClass.getFields()) {
					String srcDesc = aField.getSrcDesc();

					if (srcDesc == null || srcDesc.isEmpty()) {
						lClass.getOrCreateFieldMapping(aField.getSrcName())
								.setDeobfuscatedName(aField.getDstName(dstNamespace));
					} else {
						lClass.getOrCreateFieldMapping(aField.getSrcName(), srcDesc)
								.setDeobfuscatedName(aField.getDstName(dstNamespace));
					}
				}

				for (MappingTree.MethodMapping aMethod : aClass.getMethods()) {
					MethodMapping lMethod = lClass.getOrCreateMethodMapping(aMethod.getSrcName(), aMethod.getSrcDesc())
							.setDeobfuscatedName(aMethod.getDstName(dstNamespace));

					if (applyParameterMappings) {
						for (MappingTree.MethodArgMapping aArg : aMethod.getArgs()) {
							lMethod.getOrCreateParameterMapping(aArg.getLvIndex())
									.setDeobfuscatedName(aArg.getDstName(dstNamespace));
						}
					}
				}
			}

			return mappings;
		}

		@Override
		public void close() throws IOException {
			MappingTree tree = (MappingTree) next;
			List<String> names = new ArrayList<>();

			for (MappingTree.ClassMapping aClass : tree.getClasses()) {
				names.add(aClass.getSrcName());
			}

			for (String name : names) {
				tree.removeClass(name);
			}
		}
	}
}
