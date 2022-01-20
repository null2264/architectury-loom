/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020-2021 FabricMC
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

package net.fabricmc.loom.util.srg;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Stopwatch;
import org.gradle.api.logging.Logger;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.mappingio.FlatMappingVisitor;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.adapter.MappingNsRenamer;
import net.fabricmc.mappingio.adapter.RegularAsFlatMappingVisitor;
import net.fabricmc.mappingio.format.Tiny2Writer;
import net.fabricmc.mappingio.format.TsrgReader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

// TODO: Check if #49 came back with the rewrite
/**
 * Utilities for merging SRG mappings.
 *
 * @author Juuz
 */
public final class SrgMerger {
	private final MemoryMappingTree srg;
	private final MemoryMappingTree src;
	private final MemoryMappingTree output;
	private final FlatMappingVisitor flatOutput;
	private final boolean lenient;

	private SrgMerger(Path srg, Path tiny, boolean lenient) throws IOException {
		this.srg = readSrg(srg);
		this.src = new MemoryMappingTree();
		this.output = new MemoryMappingTree();
		this.flatOutput = new RegularAsFlatMappingVisitor(output);
		this.lenient = lenient;

		MappingReader.read(tiny, this.src);

		if (!"official".equals(this.src.getSrcNamespace())) {
			throw new MappingException("Mapping file " + tiny + " does not have the 'official' namespace as the default!");
		}

		this.output.visitNamespaces(this.src.getSrcNamespace(), Stream.concat(Stream.of("srg"), this.src.getDstNamespaces().stream()).collect(Collectors.toList()));
	}

	/**
	 * Creates a destination name array where the first element
	 * will be the srg name of the mapping element.
	 */
	private String[] createDstNameArray(MappingTree.ElementMappingView srg) {
		String[] dstNames = new String[output.getDstNamespaces().size()];
		dstNames[0] = srg.getDstName(0);
		return dstNames;
	}

	/**
	 * Copies all the non-srg destination names from an element.
	 */
	private void copyDstNames(String[] dstNames, MappingTreeView.ElementMappingView from) {
		for (int i = 1; i < dstNames.length; i++) {
			dstNames[i] = from.getDstName(i - 1);
		}
	}

	/**
	 * Fills in an array of destination names with the element's srg name.
	 */
	private void fillMappings(String[] names, MappingTree.ElementMappingView srg) {
		for (int i = 1; i < names.length; i++) {
			names[i] = srg.getDstName(0);
		}
	}

	public MemoryMappingTree merge() throws IOException {
		for (MappingTree.ClassMapping srgClass : srg.getClasses()) {
			String[] dstNames = createDstNameArray(srgClass);
			MappingTree.ClassMapping tinyClass = src.getClass(srgClass.getSrcName());

			if (tinyClass != null) {
				copyDstNames(dstNames, tinyClass);
			} else if (lenient) {
				// Tiny class not found, we'll just use srg names
				fillMappings(dstNames, srgClass);
			} else {
				throw new MappingException("Could not find class " + srgClass.getSrcName() + "|" + srgClass.getDstName(0));
			}

			flatOutput.visitClass(srgClass.getSrcName(), dstNames);

			for (MappingTree.FieldMapping field : srgClass.getFields()) {
				mergeField(srgClass, field, tinyClass);
			}

			for (MappingTree.MethodMapping method : srgClass.getMethods()) {
				mergeMethod(srgClass, method, tinyClass);
			}
		}

		return output;
	}

	private void mergeField(MappingTree.ClassMapping srgClass, MappingTree.FieldMapping srgField, @Nullable MappingTree.ClassMapping tinyClass) throws IOException {
		String[] dstNames = createDstNameArray(srgField);
		MappingTree.FieldMapping tinyField = null;

		if (tinyClass != null) {
			tinyField = tinyClass.getField(srgField.getSrcName(), srgField.getSrcDesc());
		} else if (!lenient) {
			throw new MappingException("Could not find field " + srgClass.getDstName(0) + '.' + srgField.getDstName(0) + ' ' + srgField.getDstDesc(0));
		}

		if (tinyField != null) {
			copyDstNames(dstNames, tinyField);
		} else {
			fillMappings(dstNames, srgField);
		}

		flatOutput.visitField(srgClass.getSrcName(), srgField.getSrcName(), srgField.getSrcDesc(), dstNames);
	}

	private void mergeMethod(MappingTree.ClassMapping srgClass, MappingTree.MethodMapping srgMethod, @Nullable MappingTree.ClassMapping tinyClass) throws IOException {
		String[] dstNames = createDstNameArray(srgMethod);
		MappingTree.MethodMapping tinyMethod = null;

		if (tinyClass != null) {
			tinyMethod = tinyClass.getMethod(srgMethod.getSrcName(), srgMethod.getSrcDesc());
		} else if (!lenient) {
			throw new MappingException("Could not find method " + srgClass.getDstName(0) + '.' + srgMethod.getDstName(0) + ' ' + srgMethod.getDstDesc(0));
		}

		if (tinyMethod != null) {
			copyDstNames(dstNames, tinyMethod);
		} else {
			fillMappings(dstNames, srgMethod);
		}

		flatOutput.visitMethod(srgClass.getSrcName(), srgMethod.getSrcName(), srgMethod.getSrcDesc(), dstNames);
	}

	/**
	 * Merges SRG mappings with a tiny mappings tree through the obf names.
	 *
	 * <p>This method does not care about method parameters, local variables or javadoc comments.
	 * As such, it shouldn't be used for remapping the game jar.
	 *
	 * <p>If {@code lenient} is true, the merger will not error when encountering names not present
	 * in the tiny mappings. Instead, the names will be filled from the {@code official} namespace.
	 *
	 * @param srg     the SRG file in .tsrg format
	 * @param tiny    the tiny file
	 * @param out     the output file, will be in tiny v2
	 * @param lenient whether lenient mode is enabled
	 * @throws IOException      if an IO error occurs while reading or writing the mappings
	 * @throws MappingException if the input tiny tree's default namespace is not 'official'
	 *                          or if an element mentioned in the SRG file does not have tiny mappings in non-lenient mode
	 */
	public static void mergeSrg(Logger logger, Path srg, Path tiny, Path out, boolean lenient)
			throws IOException, MappingException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		MemoryMappingTree tree = new SrgMerger(srg, tiny, lenient).merge();

		try (Tiny2Writer writer = new Tiny2Writer(Files.newBufferedWriter(out), false)) {
			tree.accept(writer);
		} catch (IOException e) {
			e.printStackTrace();
		}

		logger.info(":merged srg mappings in " + stopwatch.stop());
	}

	private MemoryMappingTree readSrg(Path srg) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(srg)) {
			MemoryMappingTree tsrg = new MemoryMappingTree();
			MemoryMappingTree temp = new MemoryMappingTree();
			TsrgReader.read(reader, temp);
			Map<String, String> namespaces = Map.of(
					temp.getSrcNamespace(), MappingsNamespace.OFFICIAL.toString(),
					temp.getDstNamespaces().get(0), MappingsNamespace.SRG.toString()
			);
			temp.accept(new MappingNsRenamer(tsrg, namespaces));
			return tsrg;
		}
	}
}
