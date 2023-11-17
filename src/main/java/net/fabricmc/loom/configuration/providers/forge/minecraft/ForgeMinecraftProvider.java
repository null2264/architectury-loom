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

package net.fabricmc.loom.configuration.providers.forge.minecraft;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.configuration.ConfigContext;
import net.fabricmc.loom.configuration.providers.forge.MinecraftPatchedProvider;
import net.fabricmc.loom.configuration.providers.minecraft.MergedMinecraftProvider;
import net.fabricmc.loom.configuration.providers.minecraft.SingleJarMinecraftProvider;

/**
 * A {@link net.fabricmc.loom.configuration.providers.minecraft.MinecraftProvider} that
 * provides a Forge patched Minecraft jar.
 */
public interface ForgeMinecraftProvider {
	MinecraftPatchedProvider getPatchedProvider();

	static MergedMinecraftProvider createMerged(ConfigContext context) {
		return LoomGradleExtension.get(context.project()).isForgeLike() ? new MergedForgeMinecraftProvider(context) : new MergedMinecraftProvider(context);
	}

	static SingleJarMinecraftProvider createServerOnly(ConfigContext context) {
		return LoomGradleExtension.get(context.project()).isForgeLike() ? SingleJarForgeMinecraftProvider.server(context) : SingleJarMinecraftProvider.server(context);
	}

	static SingleJarMinecraftProvider createClientOnly(ConfigContext context) {
		return LoomGradleExtension.get(context.project()).isForgeLike() ? SingleJarForgeMinecraftProvider.client(context) : SingleJarMinecraftProvider.client(context);
	}
}
