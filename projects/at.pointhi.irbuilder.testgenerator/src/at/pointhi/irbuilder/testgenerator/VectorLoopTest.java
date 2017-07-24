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
package at.pointhi.irbuilder.testgenerator;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runners.Parameterized;

import com.oracle.truffle.llvm.parser.model.ModelModule;
import com.oracle.truffle.llvm.parser.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.parser.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VectorType;
import com.oracle.truffle.llvm.test.options.TestOptions;

import at.pointhi.irbuilder.irbuilder.InstructionBuilder;
import at.pointhi.irbuilder.irbuilder.ModelModuleBuilder;
import at.pointhi.irbuilder.irbuilder.SimpleInstrunctionBuilder;
import at.pointhi.irbuilder.irbuilder.util.ConstantUtil;

public class VectorLoopTest extends BaseSuite {

    private static final Path FIBONACCI_SUITE_DIR = Paths.get(TestOptions.PROJECT_ROOT + "/../cache/tests/irbuilder/performance/vector");

    @Parameterized.Parameter(value = 0) public Path path;

    private final Type type = new VectorType(PrimitiveType.I32, 4);

    @Override
    public Path getSuiteDir() {
        return FIBONACCI_SUITE_DIR;
    }

    @Override
    public Path getFilename() {
        return Paths.get("test_fibonacci.ll");
    }

    /*
     * This is a workaround, to allow mx unittest to execute this testsuite.
     */
    @Override
    @Test(timeout = 1000)
    public void test() throws Exception {
        super.test();
    }

    @Override
    public ModelModule constructModelModule() {
        ModelModuleBuilder builder = new ModelModuleBuilder();

        createMain(builder);

        return builder.getModelModule();
    }

    private void createMain(ModelModuleBuilder builder) {
        FunctionDefinition main = builder.createFunctionDefinition("main", 1, new FunctionType(PrimitiveType.I32, new Type[]{}, false));
        SimpleInstrunctionBuilder instr = new SimpleInstrunctionBuilder(builder, main);

        final Instruction calcReg = instr.allocate(type);
        final Instruction incReg = instr.allocate(type);
        final Instruction counterReg = instr.allocate(PrimitiveType.I64);
        instr.store(counterReg, ConstantUtil.getI32Const(0), 4);

        instr.fillVector(calcReg, 0, 0, 0, 0);
        final Instruction increment = instr.fillVector(incReg, 1, 2, 3, 4);

        final InstructionBlock loopBlock = instr.getNextBlock();
        instr.jump(loopBlock);
        instr.nextBlock();

        final Instruction calcLoad = instr.load(calcReg);
        Instruction addRes = instr.binaryOperator(BinaryOperator.INT_ADD, calcLoad, increment);
        // for (int i = 0; i < 100; i++) {
        // addRes = instr.binaryOperator(BinaryOperator.INT_ADD, addRes, increment);
        // }
        instr.store(calcReg, addRes, 4);

        final Instruction index = instr.load(counterReg);
        final Instruction indexAdd = instr.binaryOperator(BinaryOperator.INT_ADD, index, 1);
        instr.store(counterReg, indexAdd, 4);
        final Instruction cmpRes = instr.compare(CompareOperator.INT_UNSIGNED_LESS_THAN, indexAdd, 10000000);

        final InstructionBlock returnBlock = instr.getNextBlock();
        instr.branch(cmpRes, loopBlock, returnBlock);
        instr.nextBlock();

        final Instruction res = instr.extractElement(instr.load(calcReg), 0);

        instr.returnx(res);
    }
}
