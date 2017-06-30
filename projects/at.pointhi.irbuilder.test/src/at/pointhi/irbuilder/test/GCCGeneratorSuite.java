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

import at.pointhi.irbuilder.irwriter.IRWriterVersion;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public final class GCCGeneratorSuite extends BaseGeneratorSuite {

    private static final Path GCC_SUITE_DIR = new File(LLVMOptions.ENGINE.projectRoot() + "/../cache/tests/gcc").toPath();
    private static final Path GCC_SOURCE_DIR = new File(LLVMOptions.ENGINE.projectRoot() + "/../tests/gcc/gcc-5.2.0").toPath();
    private static final Path GCC_CONFIG_DIR = new File(LLVMOptions.ENGINE.projectRoot() + "/../tests/gcc/configs").toPath();

    // Sulong exception handling not supported in Sulong v3.2 mode; use Sulong mode v3.8 or higher.
    private static final File[] GCC_EXCLUDED_FILES_32 = new File[]{
                    new File(GCC_SUITE_DIR.toFile(), "/gcc-5.2.0/gcc/testsuite/g++.dg/torture/pr47541/pr47541_clangcpp_O0.bc"),
                    new File(GCC_SUITE_DIR.toFile(), "/gcc-5.2.0/gcc/testsuite/g++.dg/opt/pr15054-2/pr15054-2_clangcpp_O0.bc"),
                    new File(GCC_SUITE_DIR.toFile(), "/gcc-5.2.0/gcc/testsuite/g++.dg/opt/pr17697-1/pr17697-1_clangcpp_O0.bc"),
                    new File(GCC_SUITE_DIR.toFile(), "/gcc-5.2.0/gcc/testsuite/g++.dg/opt/pr43655/pr43655_clangcpp_O0.bc"),
                    new File(GCC_SUITE_DIR.toFile(), "/gcc-5.2.0/gcc/testsuite/g++.dg/opt/dtor1/dtor1_clangcpp_O0.bc"),
                    new File(GCC_SUITE_DIR.toFile(), "/gcc-5.2.0/gcc/testsuite/g++.dg/template/repo9/repo9_clangcpp_O0.bc")
    };

    @Parameterized.Parameter(value = 0) public Path path;
    @Parameterized.Parameter(value = 1) public String testName;

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> data() {
        return collectTestCases(GCC_CONFIG_DIR, GCC_SUITE_DIR, GCC_SOURCE_DIR);
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
    public boolean isExcluded(File file) {
        if (IRWriterVersion.fromEnviromentVariables() == IRWriterVersion.LLVM_IR_3_2) {
            return Arrays.stream(GCC_EXCLUDED_FILES_32).filter(f -> f.equals(file)).findAny().isPresent();
        }
        return false;
    }
}
