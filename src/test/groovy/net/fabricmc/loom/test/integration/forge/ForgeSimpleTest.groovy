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

package net.fabricmc.loom.test.integration.forge

import net.fabricmc.loom.test.util.ArchiveAssertionsTrait
import net.fabricmc.loom.test.util.ProjectTestTrait
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Unroll

import static java.lang.System.setProperty
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Stepwise
class ForgeSimpleTest extends Specification implements ProjectTestTrait, ArchiveAssertionsTrait {
	@Override
	String name() {
		"forge/simple"
	}

	@Unroll
	def "build for #mcVersion #forgeVersion"() {
		given:
			setProperty('loom.test.mc_version', mcVersion)
			setProperty('loom.test.forge_version', forgeVersion)
		when:
			def result = create("build", DEFAULT_GRADLE)
		then:
			result.task(":build").outcome == SUCCESS
		where:
			mcVersion | forgeVersion
			'1.14.4'  | '28.2.23'
			'1.16.5'  | '36.2.2'
	}

	@Unroll
	def "modern build for #mcVersion #forgeVersion"() {
		given:
			setProperty('loom.test.mc_version', mcVersion)
			setProperty('loom.test.forge_version', forgeVersion)
		when:
			def result = create("build", DEFAULT_GRADLE)
		then:
			result.task(":build").outcome == SUCCESS
		where:
			mcVersion | forgeVersion
			'1.17.1'  | '37.0.13'
	}
}
