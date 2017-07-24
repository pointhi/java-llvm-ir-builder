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
import com.oracle.truffle.llvm.parser.model.enums.CastOperator;
import com.oracle.truffle.llvm.parser.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VectorType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType.PrimitiveKind;
import com.oracle.truffle.llvm.test.options.TestOptions;

import at.pointhi.irbuilder.irbuilder.ModelModuleBuilder;
import at.pointhi.irbuilder.irbuilder.SimpleInstrunctionBuilder;
import at.pointhi.irbuilder.testgenerator.util.IntegerBinaryOperations.UndefinedArithmeticResult;

@RunWith(Parameterized.class)
public class VectorBitcastTest extends BaseSuite {

    private static final Path VECTOR_SUITE_DIR = Paths.get(TestOptions.PROJECT_ROOT + "/../cache/tests/irbuilder/castVector");

    private final Type src;
    private final Type dst;

    public VectorBitcastTest(Type src, Type dst) {
        this.src = src;
        this.dst = dst;
    }

    private static final VectorType[] vec128 = new VectorType[]{
                    new VectorType(PrimitiveType.I64, 2),
                    new VectorType(PrimitiveType.I32, 4),
                    new VectorType(PrimitiveType.I16, 8),
                    new VectorType(PrimitiveType.I8, 16),
                    new VectorType(PrimitiveType.I1, 128)
    };

    @Parameters(name = "{index}: VectorBitcastTest[src={0}, dst={1}]")
    public static Collection<Object[]> data() {
        List<Object[]> parameters = new LinkedList<>();

        parameters.add(new Object[]{new VectorType(PrimitiveType.I16, 5), PrimitiveType.X86_FP80});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I8, 10), PrimitiveType.X86_FP80});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I1, 80), PrimitiveType.X86_FP80});

        parameters.add(new Object[]{new VectorType(PrimitiveType.I64, 1), PrimitiveType.I64});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I32, 2), PrimitiveType.I64});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I16, 4), PrimitiveType.I64});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I8, 8), PrimitiveType.I64});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I1, 64), PrimitiveType.I64});

        parameters.add(new Object[]{new VectorType(PrimitiveType.I64, 1), PrimitiveType.DOUBLE});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I32, 2), PrimitiveType.DOUBLE});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I16, 4), PrimitiveType.DOUBLE});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I8, 8), PrimitiveType.DOUBLE});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I1, 64), PrimitiveType.DOUBLE});

        parameters.add(new Object[]{new VectorType(PrimitiveType.I32, 1), PrimitiveType.I32});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I16, 2), PrimitiveType.I32});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I8, 4), PrimitiveType.I32});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I1, 32), PrimitiveType.I32});

        parameters.add(new Object[]{new VectorType(PrimitiveType.I32, 1), PrimitiveType.FLOAT});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I16, 2), PrimitiveType.FLOAT});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I8, 4), PrimitiveType.FLOAT});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I1, 32), PrimitiveType.FLOAT});

        parameters.add(new Object[]{new VectorType(PrimitiveType.I16, 1), PrimitiveType.I16});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I8, 2), PrimitiveType.I16});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I1, 16), PrimitiveType.I16});

        parameters.add(new Object[]{new VectorType(PrimitiveType.I8, 1), PrimitiveType.I8});
        parameters.add(new Object[]{new VectorType(PrimitiveType.I1, 8), PrimitiveType.I8});

        parameters.add(new Object[]{new VectorType(PrimitiveType.I1, 1), PrimitiveType.I1});

        for (int i = 0; i < vec128.length; i++) {
            for (int j = 0; j < vec128.length; j++) {
                parameters.add(new Object[]{vec128[i], vec128[j]});
            }
        }

        return parameters;
    }

    @Override
    public ModelModule constructModelModule() throws UndefinedArithmeticResult {
        // current limitation of our implementation
        assert src instanceof VectorType;

        ModelModuleBuilder builder = new ModelModuleBuilder();

        FunctionDefinition max = createTestMax(builder);
        FunctionDefinition mid = createTestMid(builder);
        FunctionDefinition min = createTestMin(builder);

        FunctionDefinition endian = null;
        if (src instanceof PrimitiveType && ((PrimitiveType) src).getPrimitiveKind() != PrimitiveKind.X86_FP80) {
            endian = createTestEndian(builder);
        }
        createMain(builder, max, mid, min, endian);

        return builder.getModelModule();
    }

    @Override
    public Path getSuiteDir() {
        return VECTOR_SUITE_DIR;
    }

    @Override
    public Path getFilename() {
        return Paths.get(String.format("test_vector_%s_%s.ll", src.toString().replace(" ", ""), dst.toString().replace(" ", "")));
    }

    private static void createMain(ModelModuleBuilder builder, FunctionDefinition max, FunctionDefinition mid, FunctionDefinition min, FunctionDefinition endian) {
        FunctionDefinition main = builder.createFunctionDefinition("main", 1, new FunctionType(PrimitiveType.I1, new Type[]{}, false));
        SimpleInstrunctionBuilder instr = new SimpleInstrunctionBuilder(builder, main);

        Instruction retMax = instr.call(max);
        Instruction retMid = instr.call(mid);
        Instruction retMin = instr.call(min);

        Instruction retMaxMid = instr.binaryOperator(BinaryOperator.INT_OR, retMax, retMid);
        Instruction retMaxMidMin = instr.binaryOperator(BinaryOperator.INT_OR, retMaxMid, retMin);

        if (endian != null) {
            Instruction retEndian = instr.call(endian);
            Instruction ret = instr.binaryOperator(BinaryOperator.INT_OR, retMaxMidMin, retEndian);
            instr.returnx(ret); // 0=OK, 1=ERROR
        } else {
            instr.returnx(retMaxMidMin); // 0=OK, 1=ERROR
        }
    }

    private FunctionDefinition createTestMax(ModelModuleBuilder builder) {
        long maxSrcValue = 0;
        for (int i = 0; i < ((VectorType) src).getElementType().getBitSize(); i++) {
            maxSrcValue |= 1L << i;
        }

        return createTestFunction(builder, "max", maxSrcValue);
    }

    private FunctionDefinition createTestMid(ModelModuleBuilder builder) {
        long midSrcValue = 0;
        for (int i = 0; i < ((VectorType) src).getElementType().getBitSize(); i++) {
            midSrcValue |= 1L << i;
        }

        midSrcValue &= 0xAAAAAAAAAAAAAAAAL;

        return createTestFunction(builder, "mid", midSrcValue);
    }

    private FunctionDefinition createTestMin(ModelModuleBuilder builder) {
        return createTestFunction(builder, "min", 0);
    }

    private FunctionDefinition createTestEndian(ModelModuleBuilder builder) {
        long endianSrcValue = 0;
        for (int i = 0; i < ((VectorType) src).getElementType().getBitSize(); i++) {
            endianSrcValue |= 1L << i;
        }
        endianSrcValue &= 0xAAAAAAAAAAAAAAAAL;
        long endianDstValue = 0;
        if (dst instanceof VectorType) {
            for (int i = 0; i < ((VectorType) dst).getElementType().getBitSize(); i++) {
                endianDstValue |= 1L << i;
            }
            endianDstValue &= endianSrcValue;
        } else {
            endianDstValue = endianSrcValue;
        }

        if (Type.isFloatingpointType(dst)) {
            endianDstValue = endianSrcValue;
        }

        // endianDstValue = 0x01L;

        FunctionDefinition def = builder.createFunctionDefinition("endian", 1, new FunctionType(PrimitiveType.I1, new Type[]{}, false));
        SimpleInstrunctionBuilder instr = new SimpleInstrunctionBuilder(builder, def);

        Instruction srcVecPtr = instr.allocate(src);

        long[] srcValues = new long[((VectorType) src).getNumberOfElements()];

        for (int i = 0; i < srcValues.length; i++) {
            srcValues[i] = 0;
        }
        srcValues[0] = endianSrcValue;

        Instruction filled = instr.fillVector(srcVecPtr, srcValues);

        Instruction dst1 = instr.getInstructionBuilder().createCast(dst, CastOperator.BITCAST, filled);
        Instruction dst2 = instr.getInstructionBuilder().createCast(src, CastOperator.BITCAST, dst1);

        final CompareOperator op = Type.isFloatingpointType(dst) ? CompareOperator.FP_ORDERED_NOT_EQUAL : CompareOperator.INT_NOT_EQUAL;
        final Instruction resDst;
        if (dst instanceof VectorType) {
            Instruction elem = instr.extractElement(dst1, 0);
            if (Type.isFloatingpointType(dst)) {
                resDst = instr.compare(op, elem, Double.longBitsToDouble(endianDstValue));
            } else {
                resDst = instr.compare(op, elem, endianDstValue);
            }
        } else {
            if (Type.isFloatingpointType(dst)) {
                resDst = instr.compare(op, dst1, Double.longBitsToDouble(endianDstValue));
            } else {
                resDst = instr.compare(op, dst1, endianDstValue);
            }
        }

        Instruction resDst2 = instr.compareVector(CompareOperator.INT_NOT_EQUAL, filled, dst2);

        Instruction ret = instr.binaryOperator(BinaryOperator.INT_OR, resDst, resDst2);

        instr.returnx(ret); // 0=OK, 1=ERROR

        return def;
    }

    private FunctionDefinition createTestFunction(ModelModuleBuilder builder, String name, long initialValue) {
        FunctionDefinition def = builder.createFunctionDefinition(name, 1, new FunctionType(PrimitiveType.I1, new Type[]{}, false));
        SimpleInstrunctionBuilder instr = new SimpleInstrunctionBuilder(builder, def);

        Instruction srcVecPtr = instr.allocate(src);

        long[] srcValues = new long[((VectorType) src).getNumberOfElements()];

        for (int i = 0; i < srcValues.length; i++) {
            srcValues[i] = initialValue;
        }

        Instruction filled = instr.fillVector(srcVecPtr, srcValues);

        Instruction dst1 = instr.getInstructionBuilder().createCast(dst, CastOperator.BITCAST, filled);
        Instruction dst2 = instr.getInstructionBuilder().createCast(src, CastOperator.BITCAST, dst1);

        Instruction ret = instr.compareVector(CompareOperator.INT_NOT_EQUAL, filled, dst2);
        instr.returnx(ret); // 0=OK, 1=ERROR

        return def;
    }
}
