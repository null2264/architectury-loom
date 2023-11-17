/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2023 FabricMC
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

package net.fabricmc.loom.test.integration.forge

import spock.lang.Specification
import spock.lang.Unroll

import net.fabricmc.loom.test.util.GradleProjectTestTrait

import static net.fabricmc.loom.test.LoomTestConstants.DEFAULT_GRADLE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ForgeTest extends Specification implements GradleProjectTestTrait {
	@Unroll
	def "build #mcVersion #forgeVersion #mappings"() {
		setup:
		def gradle = gradleProject(project: "forge/simple", version: DEFAULT_GRADLE)
		gradle.buildGradle.text = gradle.buildGradle.text.replace('@MCVERSION@', mcVersion)
				.replace('@FORGEVERSION@', forgeVersion)
				.replace('@MAPPINGS@', mappings)
				.replace('@REPOSITORIES@', '')
				.replace('@PACKAGE@', 'net.minecraftforge:forge')
				.replace('@JAVA_VERSION@', javaVersion)

		when:
		def result = gradle.run(task: "build")

		then:
		result.task(":build").outcome == SUCCESS

		where:
		mcVersion | forgeVersion | javaVersion | mappings
		'1.20.1'  | "47.2.1"     | '17'        | "loom.officialMojangMappings()"
		'1.20.1'  | "47.2.1"     | '17'        | "'net.fabricmc:yarn:1.19.4+build.2:v2'"
		'1.19.4'  | "45.0.43"    | '17'        | "loom.officialMojangMappings()"
		'1.19.4'  | "45.0.43"    | '17'        | "'net.fabricmc:yarn:1.19.4+build.2:v2'"
		'1.18.1'  | "39.0.63"    | '17'        | "loom.officialMojangMappings()"
		'1.18.1'  | "39.0.63"    | '17'        | '"net.fabricmc:yarn:1.18.1+build.22:v2"'
		'1.17.1'  | "37.0.67"    | '16'        | "loom.officialMojangMappings()"
		'1.17.1'  | "37.0.67"    | '16'        | '"net.fabricmc:yarn:1.17.1+build.61:v2"'
		'1.16.5'  | "36.2.4"     | '8'         | "loom.officialMojangMappings()"
		'1.16.5'  | "36.2.4"     | '8'         | '"net.fabricmc:yarn:1.16.5+build.5:v2"'
		'1.16.5'  | '36.2.4'     | '8'         | '"de.oceanlabs.mcp:mcp_snapshot:20210309-1.16.5"'
		'1.14.4'  | "28.2.23"    | '8'         | "loom.officialMojangMappings()"
		'1.14.4'  | "28.2.23"    | '8'         | '"net.fabricmc:yarn:1.14.4+build.18:v2"'
	}
}
