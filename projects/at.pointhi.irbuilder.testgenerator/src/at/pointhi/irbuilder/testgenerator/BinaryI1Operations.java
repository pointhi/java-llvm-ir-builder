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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.llvm.parser.model.ModelModule;
import com.oracle.truffle.llvm.parser.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.parser.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.test.options.TestOptions;

import at.pointhi.irbuilder.irbuilder.ModelModuleBuilder;
import at.pointhi.irbuilder.irbuilder.SimpleInstrunctionBuilder;
import at.pointhi.irbuilder.testgenerator.util.IntegerBinaryOperations;
import at.pointhi.irbuilder.testgenerator.util.IntegerBinaryOperations.UndefinedArithmeticResult;

@RunWith(Parameterized.class)
public class BinaryI1Operations extends BaseSuite {

    private static final Path I1_SUITE_DIR = Paths.get(TestOptions.PROJECT_ROOT + "/../cache/tests/irbuilder/binaryI1");

    private final BinaryOperator operator;
    private final boolean op1;
    private final boolean op2;

    public BinaryI1Operations(BinaryOperator operator, boolean op1, boolean op2) {
        this.operator = operator;
        this.op1 = op1;
        this.op2 = op2;
    }

    @Override
    public Path getSuiteDir() {
        return I1_SUITE_DIR;
    }

    @Override
    public Path getFilename() {
        return Paths.get(String.format("test_i1_%s_(%b-%b).ll", operator.getIrString(), op1, op2));
    }

    @Parameters(name = "{index}: BinaryI1Operations[operator={0}, op1={1}, op2={2}]")
    public static Collection<Object[]> data() {
        List<Object[]> parameters = new LinkedList<>();

        for (BinaryOperator operator : BinaryOperator.values()) {
            if (operator.isFloatingPoint()) {
                continue;
            }

            switch (operator) {
                case INT_SHIFT_LEFT:
                case INT_LOGICAL_SHIFT_RIGHT:
                case INT_ARITHMETIC_SHIFT_RIGHT:
                    continue; // not really implemented yet for boolean
                default:
                    break;
            }

            addParameter(parameters, operator, false, false);
            addParameter(parameters, operator, false, true);
            addParameter(parameters, operator, true, false);
            addParameter(parameters, operator, true, true);
        }

        return parameters;
    }

    private static void addParameter(List<Object[]> parameters, BinaryOperator operator, boolean op1, boolean op2) {
        try {
            IntegerBinaryOperations intOp = new IntegerBinaryOperations(PrimitiveType.I1);
            intOp.calculateResult(operator, op1, op2);
        } catch (UndefinedArithmeticResult res) {
            return; // we don't want to create a testcase when the operation is undefined
        }
        parameters.add(new Object[]{operator, op1, op2});
    }

    @Override
    public ModelModule constructModelModule() throws UndefinedArithmeticResult {
        ModelModuleBuilder builder = new ModelModuleBuilder();
        createMain(builder);

        return builder.getModelModule();
    }

    private void createMain(ModelModuleBuilder builder) throws UndefinedArithmeticResult {
        FunctionDefinition main = builder.createFunctionDefinition("main", 1, new FunctionType(PrimitiveType.I1, new Type[]{}, false));
        SimpleInstrunctionBuilder instr = new SimpleInstrunctionBuilder(builder, main);

        IntegerConstant op1Sym = new IntegerConstant(PrimitiveType.I1, op1 ? 1 : 0);
        IntegerConstant op2Sym = new IntegerConstant(PrimitiveType.I1, op2 ? 1 : 0);

        Instruction opRet = instr.binaryOperator(operator, op1Sym, op2Sym); // Instruction under

        boolean opResult = IntegerBinaryOperations.I1.calculateResult(operator, op1, op2);
        Instruction ret = instr.compare(CompareOperator.INT_NOT_EQUAL, opRet, opResult);
        instr.returnx(ret); // 0=OK, 1=ERROR
    }

}
