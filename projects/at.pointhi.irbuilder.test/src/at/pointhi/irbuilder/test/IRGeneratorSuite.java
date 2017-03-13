/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2017, Thomas Pointhuber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *
 * Neither the name of the copyright holder nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package at.pointhi.irbuilder.test;

import com.oracle.truffle.llvm.runtime.options.LLVMOptions;
import com.oracle.truffle.llvm.test.alpha.BaseSuiteHarness;
import com.oracle.truffle.llvm.test.alpha.BaseTestHarness;

import at.pointhi.irbuilder.irwriter.SourceParser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public final class IRGeneratorSuite extends BaseSuiteHarness {

    private static final Path SULONG_SUITE_DIR = new File(LLVMOptions.ENGINE.projectRoot() + "/../cache/tests/sulong").toPath();
    private static final Path SULONG_SOURCE_DIR = new File(LLVMOptions.ENGINE.projectRoot() + "/../tests/sulong").toPath();
    private static final Path SULONG_CONFIG_DIR = new File(LLVMOptions.ENGINE.projectRoot() + "/../tests/sulong/configs").toPath();

    @Parameterized.Parameter(value = 0) public Path path;
    @Parameterized.Parameter(value = 1) public String testName;

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> data() {
        return collectTestCases(SULONG_CONFIG_DIR, SULONG_SUITE_DIR, SULONG_SOURCE_DIR);
    }

    @Override
    protected Path getSuiteDirectory() {
        return SULONG_SUITE_DIR;
    }

    @Override
    protected Path getTestDirectory() {
        return path;
    }

    @Override
    protected String getTestName() {
        return testName;
    }

    @Override
    @Test
    public void test() throws Exception {
        final List<Path> testCandidates = Files.walk(path).filter(BaseTestHarness.isFile).filter(BaseTestHarness.isSulong).collect(Collectors.toList());
        for (Path candidate : testCandidates) {

            if (!candidate.toAbsolutePath().toFile().exists()) {
                throw new AssertionError("File " + candidate.toAbsolutePath().toFile() + " does not exist.");
            }

            SourceParser.parseAndOutputFile(candidate.toFile());
        }

    }

}