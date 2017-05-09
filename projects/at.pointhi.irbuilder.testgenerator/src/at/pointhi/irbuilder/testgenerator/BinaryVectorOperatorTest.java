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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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

import at.pointhi.irbuilder.irbuilder.InstructionBuilder;
import at.pointhi.irbuilder.irbuilder.ModelModuleBuilder;
import at.pointhi.irbuilder.irwriter.IRWriter;
import at.pointhi.irbuilder.irwriter.IRWriterVersion;

@RunWith(Parameterized.class)
public class BinaryVectorOperatorTest {

    private static final Path VECTOR_SUITE_DIR = new File(LLVMOptions.ENGINE.projectRoot() + "/../cache/tests/irbuilder/vector").toPath();

    private final PrimitiveType type;
    private final BinaryOperator operator;

    public BinaryVectorOperatorTest(PrimitiveType type, BinaryOperator operator) {
        this.type = type;
        this.operator = operator;
    }

    @Parameters(name = "{index}: BinaryVectorOperator[type={0}, operator={1}]")
    public static Collection<Object[]> data() {
        List<Object[]> parameters = new LinkedList<>();

        // TODO: other Vector arrays have some implementation gaps
        final PrimitiveType[] types = new PrimitiveType[]{PrimitiveType.I16, PrimitiveType.I32, PrimitiveType.I64};

        for (PrimitiveType type : types) {
            if (!PrimitiveType.isIntegerType(type)) {
                continue;
            }
            for (BinaryOperator operator : BinaryOperator.values()) {
                if (operator.isFloatingPoint()) {
                    continue;
                }
                parameters.add(new Object[]{type, operator});
            }
        }

        return parameters;
    }

    private static final long VECTOR1_1 = 111191111L; // prim
    private static final long VECTOR1_2 = 792606555396976L; // even

    private static final long VECTOR2_1 = 200560490131L; // prim
    private static final long VECTOR2_2 = 1442968193L; // prim

    @Test
    public void test() {
        assert PrimitiveType.isIntegerType(type);
        assert !operator.isFloatingPoint();

        ModelModuleBuilder builder = new ModelModuleBuilder();

        createMain(builder);

        File resultFile = getOutputPath();

        VECTOR_SUITE_DIR.toFile().mkdirs(); // TODO: do only once
        IRWriter.writeIRToFile(builder.getModelModule(), IRWriterVersion.fromEnviromentVariables(), resultFile);

        System.out.print("."); // TODO
    }

    private File getOutputPath() {
        String filename = String.format("test_vector_i%d_%s.ll", type.getBitSize(), operator.getIrString());

        return new File(VECTOR_SUITE_DIR.toFile(), filename);
    }

    private void createMain(ModelModuleBuilder builder) {
        long maxValue = type.getBitSize() < 64 ? 1L << (type.getBitSize() - 1) : Long.MAX_VALUE;

        long vector11 = VECTOR1_1 % maxValue;
        long vector12 = VECTOR1_2 % maxValue;
        long vector21 = VECTOR2_1 % maxValue;
        long vector22 = VECTOR2_2 % maxValue;

        switch (operator) {
            case INT_SHIFT_LEFT:
                // TODO: workaround for bug, or is this expected behaviour?
                vector21 %= (type.getBitSize() / 2);
                vector22 %= (type.getBitSize() / 2);
                break;
            default:
                break;
        }

        long result1 = calculateResultValue(vector11, vector21, maxValue);
        long result2 = calculateResultValue(vector12, vector22, maxValue);

        FunctionDefinition main = builder.createFunctionDefinition("main", 1, new FunctionType(PrimitiveType.I1, new Type[]{}, false));
        InstructionBuilder mainBuilder = new InstructionBuilder(main);

        // TODO: wrong align?
        Instruction vec1 = mainBuilder.createAllocate(new VectorType(type, 2));
        Instruction vec2 = mainBuilder.createAllocate(new VectorType(type, 2));

        vec1 = mainBuilder.createLoad(vec1);
        vec1 = mainBuilder.createInsertElement(vec1, new IntegerConstant(type, vector11), 0);
        vec1 = mainBuilder.createInsertElement(vec1, new IntegerConstant(type, vector12), 1);

        vec2 = mainBuilder.createLoad(vec2);
        vec2 = mainBuilder.createInsertElement(vec2, new IntegerConstant(type, vector21), 0);
        vec2 = mainBuilder.createInsertElement(vec2, new IntegerConstant(type, vector22), 1);

        Instruction retVec = mainBuilder.createBinaryOperation(vec1, vec2, operator);

        Instruction retVec1 = mainBuilder.createExtractElement(retVec, 0);
        Instruction retVec2 = mainBuilder.createExtractElement(retVec, 1);

        retVec1 = mainBuilder.createCompare(CompareOperator.INT_NOT_EQUAL, retVec1, new IntegerConstant(type, result1));
        retVec2 = mainBuilder.createCompare(CompareOperator.INT_NOT_EQUAL, retVec2, new IntegerConstant(type, result2));

        Instruction ret = mainBuilder.createBinaryOperation(retVec1, retVec2, BinaryOperator.INT_OR);
        mainBuilder.createReturn(ret); // 0=OK, 1=ERROR

        mainBuilder.exitFunction();
    }

    private long calculateResultValue(long vector1, long vector2, long maxValue) {
        // TODO: signed/unsigned?
        switch (operator) {
            case INT_ADD:
                return (vector1 + vector2) % maxValue;
            case INT_SUBTRACT:
                return (vector1 - vector2) % maxValue;
            case INT_MULTIPLY:
                return (vector1 * vector2) % maxValue;
            case INT_UNSIGNED_DIVIDE:
                return (vector1 / vector2) % maxValue;
            case INT_SIGNED_DIVIDE:
                return (vector1 / vector2) % maxValue;
            case INT_UNSIGNED_REMAINDER:
                return (vector1 % vector2) % maxValue;
            case INT_SIGNED_REMAINDER:
                return (vector1 % vector2) % maxValue;
            case INT_SHIFT_LEFT:
                return (vector1 << vector2) % maxValue;
            case INT_LOGICAL_SHIFT_RIGHT:
                return (vector1 >> vector2) % maxValue;
            case INT_ARITHMETIC_SHIFT_RIGHT:
                return (vector1 >>> vector2) % maxValue;
            case INT_AND:
                return (vector1 & vector2) % maxValue;
            case INT_OR:
                return (vector1 | vector2) % maxValue;
            case INT_XOR:
                return (vector1 ^ vector2) % maxValue;
            default:
                fail("unexpected operator");
                return 0;
        }
    }
}
