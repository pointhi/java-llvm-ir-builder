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

import java.math.BigInteger;
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
public class BinaryVectorOperatorTest extends BaseSuite {

    private static final Path VECTOR_SUITE_DIR = Paths.get(LLVMOptions.ENGINE.projectRoot() + "/../cache/tests/irbuilder/vector");

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
        final Type[] types = new Type[]{PrimitiveType.I8, PrimitiveType.I16, PrimitiveType.I32, PrimitiveType.I64};

        for (Type type : types) {
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

    @Override
    public ModelModule constructModelModule() throws UndefinedArithmeticResult {
        assert PrimitiveType.isIntegerType(type);
        assert !operator.isFloatingPoint();

        ModelModuleBuilder builder = new ModelModuleBuilder();
        createMain(builder);

        return builder.getModelModule();
    }

    @Override
    public Path getSuiteDir() {
        return VECTOR_SUITE_DIR;
    }

    @Override
    public Path getFilename() {
        return Paths.get(String.format("test_vector_i%d_%s.ll", type.getBitSize(), operator.getIrString()));
    }

    private void createMain(ModelModuleBuilder builder) throws UndefinedArithmeticResult {
        long maxValue = type.getBitSize() < 64 ? 1L << (type.getBitSize() - 1) : Long.MAX_VALUE;

        OperatorResult resultValue1 = new OperatorResult(operator,
                        BigInteger.valueOf(VECTOR1_1), BigInteger.valueOf(VECTOR1_2),
                        BigInteger.ZERO, BigInteger.valueOf(maxValue));

        OperatorResult resultValue2 = new OperatorResult(operator,
                        BigInteger.valueOf(VECTOR2_1), BigInteger.valueOf(VECTOR2_2),
                        BigInteger.ZERO, BigInteger.valueOf(maxValue));

        FunctionDefinition main = builder.createFunctionDefinition("main", 1, new FunctionType(PrimitiveType.I1, new Type[]{}, false));
        SimpleInstrunctionBuilder instr = new SimpleInstrunctionBuilder(main);

        // TODO: wrong align?
        Instruction vec1 = instr.allocate(new VectorType(type, 2));
        Instruction vec2 = instr.allocate(new VectorType(type, 2));
        Instruction resVec = instr.allocate(new VectorType(type, 2));

        vec1 = instr.fillVector(vec1, resultValue1.getSeed1(), resultValue2.getSeed1());
        vec2 = instr.fillVector(vec2, resultValue1.getSeed2(), resultValue2.getSeed2());
        resVec = instr.fillVector(resVec, resultValue1.getResult(), resultValue2.getResult());

        Instruction retVec = instr.binaryOperator(operator, vec1, vec2); // Instruction under test

        Instruction ret = instr.compareVector(CompareOperator.INT_NOT_EQUAL, retVec, resVec);
        instr.returnx(ret); // 0=OK, 1=ERROR

        instr.getInstructionBuilder().exitFunction();
    }

    private static final class OperatorResult {
        private final BinaryOperator operator;
        private final BigInteger seed1;
        private final BigInteger seed2;

        private OperatorResult(BinaryOperator operator, BigInteger seed1, BigInteger seed2, BigInteger minValue, BigInteger maxValue) {
            this.operator = operator;

            BigInteger tmpSeed1 = seed1.mod(maxValue);
            BigInteger tmpSeed2 = getMinSeed2(getMaxSeed2(seed2.mod(maxValue)));

            if (tmpSeed2.equals(BigInteger.ZERO)) {
                tmpSeed2 = BigInteger.ONE;
            }

            // minimize the values until no overflow error occurs
            minimize: for (;;) {
                // TODO: better code
                switch (operator) {
                    case INT_SHIFT_LEFT:
                    case INT_LOGICAL_SHIFT_RIGHT:
                    case INT_ARITHMETIC_SHIFT_RIGHT:
                        break minimize; // in those cases, a overflow is not problematic

                    case INT_SUBTRACT:
                        if (maxValue.longValue() == 1) {
                            tmpSeed1 = seed1.mod(BigInteger.valueOf(2));
                            tmpSeed2 = seed2.mod(BigInteger.valueOf(2));
                            break minimize;
                        }
                        break;
                    default:
                        break;
                }
                try {
                    BigInteger result = IntegerBinaryOperations.I64.calculateResult(operator, tmpSeed1, tmpSeed2);
                    if (result.compareTo(minValue) < 0 || result.compareTo(maxValue) > 0) {
                        if (tmpSeed1.abs().compareTo(tmpSeed2.abs()) >= 0) {
                            tmpSeed1 = tmpSeed1.divide(BigInteger.valueOf(2));
                        } else {
                            tmpSeed2 = tmpSeed2.divide(BigInteger.valueOf(2));
                        }
                        if (tmpSeed2.equals(BigInteger.ZERO)) {
                            tmpSeed2 = BigInteger.ONE;
                        }
                        continue;
                    }
                    break;
                } catch (ArithmeticException | UndefinedArithmeticResult e) {
                    tmpSeed2 = tmpSeed2.add(BigInteger.valueOf(2));
                }
            }

            this.seed1 = tmpSeed1;
            this.seed2 = tmpSeed2;
        }

        public BigInteger getSeed1() {
            return seed1;
        }

        public BigInteger getSeed2() {
            return seed2;
        }

        public BigInteger getResult() throws UndefinedArithmeticResult {
            return IntegerBinaryOperations.I64.calculateResult(operator, seed1, seed2);
        }

        private BigInteger getMinSeed2(BigInteger minValue) {
            switch (operator) {
                case INT_UNSIGNED_DIVIDE:
                case INT_UNSIGNED_REMAINDER:
                    return BigInteger.ZERO;
                default:
                    return minValue;
            }
        }

        private BigInteger getMaxSeed2(BigInteger maxValue) {
            switch (operator) {
                case INT_SHIFT_LEFT:
                case INT_LOGICAL_SHIFT_RIGHT:
                case INT_ARITHMETIC_SHIFT_RIGHT:
                    // limit the value range, to construct a useful example
                    return BigInteger.valueOf(maxValue.bitCount() / 2);
                default:
                    return maxValue;
            }
        }

    }
}
