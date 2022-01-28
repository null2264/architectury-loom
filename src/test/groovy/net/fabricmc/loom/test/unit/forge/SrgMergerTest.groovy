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

package net.fabricmc.loom.test.unit.forge

import net.fabricmc.loom.util.srg.SrgMerger
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class SrgMergerTest extends Specification {
    @TempDir
    Path mappingsDir

    def "test SrgMerger"() {
        def srgInput = extractTempFile("srgInput.tsrg")
        def tinyInput = extractTempFile("tinyInput.tiny")
        def proguardInput = extractTempFile("proguard.txt")
        def output = mappingsDir.resolve("output.tiny")
        def expected = readTestData("expectedOutput.tiny")

        when:
            SrgMerger.mergeSrg(srgInput, tinyInput, output, proguardInput, true)

        then:
            Files.readAllLines(output) == expected
    }

    private InputStream openTestDataStream(String path) {
        return getClass().getResourceAsStream("/forge/testSrg/$path")
    }

    private List<String> readTestData(String path) {
        try (def input = openTestDataStream(path)) {
            assert input != null
            return input.readLines()
        }
    }

    private Path extractTempFile(String path) {
        def output = mappingsDir.resolve(path)

        try (def input = openTestDataStream(path)) {
            assert input != null
            Files.copy(input, output)
        }

        return output
    }
}
