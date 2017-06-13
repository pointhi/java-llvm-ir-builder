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
import com.oracle.truffle.llvm.parser.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.parser.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.runtime.options.LLVMOptions;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;

import at.pointhi.irbuilder.irbuilder.InstructionBuilder;
import at.pointhi.irbuilder.irbuilder.ModelModuleBuilder;
import at.pointhi.irbuilder.irbuilder.SimpleInstrunctionBuilder;

public class FibonacciFunctionCallTest extends BaseSuite {

    private static final Path FIBONACCI_SUITE_DIR = Paths.get(LLVMOptions.ENGINE.projectRoot() + "/../cache/tests/irbuilder/fibonacci");

    @Parameterized.Parameter(value = 0) public Path path;

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

        FunctionDefinition fibonacci = builder.createFunctionDefinition("fibonacci", 3, new FunctionType(PrimitiveType.I32, new Type[]{PrimitiveType.I32}, false));
        InstructionBuilder fibFacade = new InstructionBuilder(fibonacci);
        SimpleInstrunctionBuilder fibInstr = new SimpleInstrunctionBuilder(builder, fibFacade);

        createMain(builder, fibonacci);

        FunctionParameter param1 = fibInstr.nextParameter();

        Instruction cmp1 = fibInstr.compare(CompareOperator.INT_SIGNED_LESS_OR_EQUAL, param1, 1);
        fibFacade.createBranch(cmp1, fibFacade.getBlock(1), fibFacade.getBlock(2));

        fibFacade.nextBlock();
        fibInstr.returnx(param1);

        fibFacade.nextBlock();
        Instruction fib1Pos = fibInstr.binaryOperator(BinaryOperator.INT_SUBTRACT, param1, 1);
        Instruction fib1 = fibInstr.call(fibonacci, fib1Pos);

        Instruction fib2Pos = fibInstr.binaryOperator(BinaryOperator.INT_SUBTRACT, param1, 2);
        Instruction fib2 = fibInstr.call(fibonacci, fib2Pos);

        Instruction res = fibInstr.binaryOperator(BinaryOperator.INT_ADD, fib1, fib2);

        fibInstr.returnx(res);
        fibFacade.exitFunction();

        return builder.getModelModule();
    }

    private static void createMain(ModelModuleBuilder builder, FunctionDefinition fibonacci) {
        FunctionDefinition main = builder.createFunctionDefinition("main", 1, new FunctionType(PrimitiveType.I1, new Type[]{}, false));
        InstructionBuilder mainFacade = new InstructionBuilder(main);
        SimpleInstrunctionBuilder mainInstr = new SimpleInstrunctionBuilder(builder, mainFacade);

        Instruction fibRes = mainInstr.call(fibonacci, new IntegerConstant(PrimitiveType.I32, 10));
        Instruction ret = mainInstr.compare(CompareOperator.INT_NOT_EQUAL, fibRes, 55);
        mainInstr.returnx(ret); // 0=OK, 1=ERROR

        mainFacade.exitFunction();
    }
}
