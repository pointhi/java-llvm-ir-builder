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
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;
import com.oracle.truffle.llvm.test.options.TestOptions;

import at.pointhi.irbuilder.irbuilder.InstructionBuilder;
import at.pointhi.irbuilder.irbuilder.ModelModuleBuilder;
import at.pointhi.irbuilder.irbuilder.SimpleInstrunctionBuilder;

public class PhiNodeTest extends BaseSuite {

    private static final Path PHI_SUITE_DIR = Paths.get(TestOptions.PROJECT_ROOT + "/../cache/tests/irbuilder/phi");

    @Parameterized.Parameter(value = 0) public Path path;

    @Override
    public Path getSuiteDir() {
        return PHI_SUITE_DIR;
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

    private static void createMain(ModelModuleBuilder builder) {
        FunctionDefinition main = builder.createFunctionDefinition("main", 7, new FunctionType(PrimitiveType.I32, new Type[]{}, false));
        InstructionBuilder facade = new InstructionBuilder(main);
        SimpleInstrunctionBuilder instr = new SimpleInstrunctionBuilder(builder, facade);

        facade.createBranch(instr.getBlock(1)); // TODO: change

        InstructionBlock block1 = instr.nextBlock(); // Block 1
        Instruction res1 = instr.binaryOperator(BinaryOperator.INT_ADD, 0, new IntegerConstant(PrimitiveType.I32, 1));
        facade.createBranch(instr.getBlock(6));

        InstructionBlock block2 = instr.nextBlock(); // Block 2
        Instruction res2 = instr.binaryOperator(BinaryOperator.INT_ADD, 0, new IntegerConstant(PrimitiveType.I32, 2));
        facade.createBranch(instr.getBlock(6));

        InstructionBlock block3 = instr.nextBlock(); // Block 3
        Instruction res3 = instr.binaryOperator(BinaryOperator.INT_ADD, 0, new IntegerConstant(PrimitiveType.I32, 3));
        facade.createBranch(instr.getBlock(6));

        InstructionBlock block4 = instr.nextBlock(); // Block 4
        Instruction res4 = instr.binaryOperator(BinaryOperator.INT_ADD, 0, new IntegerConstant(PrimitiveType.I32, 4));
        facade.createBranch(instr.getBlock(6));

        InstructionBlock block5 = instr.nextBlock(); // Block 5
        Instruction res5 = instr.binaryOperator(BinaryOperator.INT_ADD, 0, new IntegerConstant(PrimitiveType.I32, 5));
        facade.createBranch(instr.getBlock(6));

        instr.nextBlock(); // Block 6
        final Symbol[] values = new Symbol[]{res1, res2, res3, res4, res5};
        final InstructionBlock[] blocks = new InstructionBlock[]{block1, block2, block3, block4, block5};
        Instruction phiResult = facade.createPhi(PrimitiveType.I32, values, blocks);

        instr.returnx(phiResult); // 0=OK, 1=ERROR
    }
}
