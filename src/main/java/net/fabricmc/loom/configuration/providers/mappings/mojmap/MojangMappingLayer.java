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

package net.fabricmc.loom.configuration.providers.mappings.mojmap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import org.gradle.api.logging.Logger;

import net.fabricmc.loom.configuration.providers.mappings.MappingLayer;
import net.fabricmc.loom.configuration.providers.mappings.MappingNamespace;
import net.fabricmc.loom.configuration.providers.mappings.intermediary.IntermediaryMappingLayer;
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftVersionMeta;
import net.fabricmc.loom.util.HashedDownloadUtil;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.ProGuardReader;

public class MojangMappingLayer implements MappingLayer {
	private final MinecraftVersionMeta.Download clientDownload;
	private final MinecraftVersionMeta.Download serverDownload;
	private final File workingDir;
	private final Logger logger;
	private final BooleanSupplier silenceLicense;

	public MojangMappingLayer(MinecraftVersionMeta.Download clientDownload,
			MinecraftVersionMeta.Download serverDownload,
			File workingDir,
			Logger logger,
			BooleanSupplier silenceLicense) {
		this.clientDownload = clientDownload;
		this.serverDownload = serverDownload;
		this.workingDir = workingDir;
		this.logger = logger;
		this.silenceLicense = silenceLicense;
	}

	public MinecraftVersionMeta.Download clientDownload() {
		return clientDownload;
	}

	public MinecraftVersionMeta.Download serverDownload() {
		return serverDownload;
	}

	public File workingDir() {
		return workingDir;
	}

	public Logger logger() {
		return logger;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		MojangMappingLayer that = (MojangMappingLayer) obj;
		return Objects.equals(this.clientDownload, that.clientDownload)
				&& Objects.equals(this.serverDownload, that.serverDownload)
				&& Objects.equals(this.workingDir, that.workingDir)
				&& Objects.equals(this.logger, that.logger);
	}

	@Override
	public int hashCode() {
		return Objects.hash(clientDownload, serverDownload, workingDir, logger);
	}

	@Override
	public String toString() {
		return "MojangMappingLayer["
				+ "clientDownload=" + clientDownload + ", "
				+ "serverDownload=" + serverDownload + ", "
				+ "workingDir=" + workingDir + ", "
				+ "logger=" + logger + ']';
	}

	@Override
	public void visit(MappingVisitor mappingVisitor) throws IOException {
		File clientMappings = new File(workingDir(), "client.txt");
		File serverMappings = new File(workingDir(), "server.txt");

		download(clientMappings, serverMappings);

		if (!silenceLicense.getAsBoolean()) {
			printMappingsLicense(clientMappings.toPath());
		}

		// Make official the source namespace
		MappingSourceNsSwitch nsSwitch = new MappingSourceNsSwitch(mappingVisitor, MappingNamespace.OFFICIAL.stringValue());

		try (BufferedReader clientBufferedReader = Files.newBufferedReader(clientMappings.toPath(), StandardCharsets.UTF_8);
				BufferedReader serverBufferedReader = Files.newBufferedReader(serverMappings.toPath(), StandardCharsets.UTF_8)) {
			ProGuardReader.read(clientBufferedReader, MappingNamespace.NAMED.stringValue(), MappingNamespace.OFFICIAL.stringValue(), nsSwitch);
			ProGuardReader.read(serverBufferedReader, MappingNamespace.NAMED.stringValue(), MappingNamespace.OFFICIAL.stringValue(), nsSwitch);
		}
	}

	private void download(File clientMappings, File serverMappings) throws IOException {
		HashedDownloadUtil.downloadIfInvalid(new URL(clientDownload().getUrl()), clientMappings, clientDownload().getSha1(), logger(), false);
		HashedDownloadUtil.downloadIfInvalid(new URL(serverDownload().getUrl()), serverMappings, serverDownload().getSha1(), logger(), false);
	}

	private void printMappingsLicense(Path clientMappings) {
		try (BufferedReader clientBufferedReader = Files.newBufferedReader(clientMappings, StandardCharsets.UTF_8)) {
			logger().warn("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
			logger().warn("Using of the official minecraft mappings is at your own risk!");
			logger().warn("Please make sure to read and understand the following license:");
			logger().warn("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
			String line;

			while ((line = clientBufferedReader.readLine()).startsWith("#")) {
				logger().warn(line);
			}

			logger().warn("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		} catch (IOException e) {
			throw new RuntimeException("Failed to read client mappings", e);
		}
	}

	@Override
	public MappingNamespace getSourceNamespace() {
		return MappingNamespace.OFFICIAL;
	}

	@Override
	public List<Class<? extends MappingLayer>> dependsOn() {
		return Collections.singletonList(IntermediaryMappingLayer.class);
	}
}
