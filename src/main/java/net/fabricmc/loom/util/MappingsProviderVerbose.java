package net.fabricmc.loom.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;

import dev.architectury.mappingslayers.api.mutable.MutableTinyMetadata;
import dev.architectury.mappingslayers.api.mutable.MutableTinyTree;
import dev.architectury.mappingslayers.api.utils.MappingsUtils;
import dev.architectury.tinyremapper.IMappingProvider;

public class MappingsProviderVerbose {
	public static void saveFile(Iterable<IMappingProvider> providers) throws IOException {
		MutableTinyTree tree = MappingsUtils.create(MutableTinyMetadata.create(2, 0, Arrays.asList("from", "to"), new HashMap<>()));

		for (IMappingProvider provider : providers) {
			provider.load(new IMappingProvider.MappingAcceptor() {
				@Override
				public void acceptClass(String from, String to) {
					tree.getOrCreateClass(from).setName(1, to);
				}

				@Override
				public void acceptMethod(IMappingProvider.Member from, String to) {
					tree.getOrCreateClass(from.owner).getOrCreateMethod(from.name, from.desc)
							.setName(1, to);
				}

				@Override
				public void acceptMethodArg(IMappingProvider.Member from, int lvIndex, String to) {
					tree.getOrCreateClass(from.owner).getOrCreateMethod(from.name, from.desc)
							.getOrCreateParameter(lvIndex, "")
							.setName(1, to);
				}

				@Override
				public void acceptMethodVar(IMappingProvider.Member from, int i, int i1, int i2, String s) {
					// NO-OP
				}

				@Override
				public void acceptField(IMappingProvider.Member from, String to) {
					tree.getOrCreateClass(from.owner).getOrCreateField(from.name, from.desc)
							.setName(1, to);
				}
			});
		}

		Path check = Files.createTempFile("CHECK", null);
		Files.write(check, MappingsUtils.serializeToString(tree).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}
}
