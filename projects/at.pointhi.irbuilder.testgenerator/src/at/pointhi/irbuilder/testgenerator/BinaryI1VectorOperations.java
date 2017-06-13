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
import java.util.ArrayList;
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
import com.oracle.truffle.llvm.runtime.options.LLVMOptions;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VectorType;

import at.pointhi.irbuilder.irbuilder.ModelModuleBuilder;
import at.pointhi.irbuilder.irbuilder.SimpleInstrunctionBuilder;
import at.pointhi.irbuilder.testgenerator.util.IntegerBinaryOperations;
import at.pointhi.irbuilder.testgenerator.util.IntegerBinaryOperations.UndefinedArithmeticResult;

@RunWith(Parameterized.class)
public class BinaryI1VectorOperations extends BaseSuite {

    private static final Path I1_SUITE_DIR = Paths.get(LLVMOptions.ENGINE.projectRoot() + "/../cache/tests/irbuilder/binaryI1Vector");

    private final BinaryOperator operator;

    public BinaryI1VectorOperations(BinaryOperator operator) {
        this.operator = operator;
    }

    @Override
    public Path getSuiteDir() {
        return I1_SUITE_DIR;
    }

    @Override
    public Path getFilename() {
        return Paths.get(String.format("test_i1_vector_%s.ll", operator.getIrString()));
    }

    @Parameters(name = "{index}: BinaryI1VectorOperations[operator={0}]")
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

            parameters.add(new Object[]{operator});
        }

        return parameters;
    }

    @Override
    public ModelModule constructModelModule() {
        ModelModuleBuilder builder = new ModelModuleBuilder();
        createMain(builder);

        return builder.getModelModule();
    }

    private void createMain(ModelModuleBuilder builder) {
        FunctionDefinition main = builder.createFunctionDefinition("main", 1, new FunctionType(PrimitiveType.I1, new Type[]{}, false));
        SimpleInstrunctionBuilder instr = new SimpleInstrunctionBuilder(builder, main);

        List<Object[]> paramList = new ArrayList<>();
        createResultList(paramList, operator);

        Instruction vec1 = instr.allocate(new VectorType(PrimitiveType.I1, paramList.size()));
        Instruction vec2 = instr.allocate(new VectorType(PrimitiveType.I1, paramList.size()));
        Instruction resVec = instr.allocate(new VectorType(PrimitiveType.I1, paramList.size()));

        vec1 = instr.fillVector(vec1, getConstantArray(paramList, 1));
        vec2 = instr.fillVector(vec2, getConstantArray(paramList, 2));
        resVec = instr.fillVector(resVec, getConstantArray(paramList, 3));

        Instruction retVec = instr.binaryOperator(operator, vec1, vec2); // Instruction under test

        Instruction ret = instr.compareVector(CompareOperator.INT_NOT_EQUAL, retVec, resVec);
        instr.returnx(ret); // 0=OK, 1=ERROR

        instr.getInstructionBuilder().exitFunction();
    }

    private static IntegerConstant[] getConstantArray(List<Object[]> paramList, int idx) {
        return paramList.stream().map(p -> new IntegerConstant(PrimitiveType.I1, (boolean) p[idx] ? 1 : 0)).toArray(IntegerConstant[]::new);
    }

    private static void createResultList(List<Object[]> parameters, BinaryOperator operator) {
        addParameter(parameters, operator, false, false);
        addParameter(parameters, operator, false, true);
        addParameter(parameters, operator, true, false);
        addParameter(parameters, operator, true, true);
    }

    private static void addParameter(List<Object[]> parameters, BinaryOperator operator, boolean op1, boolean op2) {
        try {
            boolean res = IntegerBinaryOperations.I1.calculateResult(operator, op1, op2);

            parameters.add(new Object[]{operator, op1, op2, res});
        } catch (UndefinedArithmeticResult res) {
            // we don't want to create a testcase when the operation is undefined
        }
    }
}
