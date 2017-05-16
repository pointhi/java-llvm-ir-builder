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
package at.pointhi.irbuilder.testgenerator.util;

import static org.junit.Assert.fail;

import java.math.BigInteger;

import com.oracle.truffle.llvm.parser.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;

public class IntegerBinaryOperations {

    public static final IntegerBinaryOperations I1 = new IntegerBinaryOperations(PrimitiveType.I1);
    public static final IntegerBinaryOperations I8 = new IntegerBinaryOperations(PrimitiveType.I8);
    public static final IntegerBinaryOperations I16 = new IntegerBinaryOperations(PrimitiveType.I16);
    public static final IntegerBinaryOperations I32 = new IntegerBinaryOperations(PrimitiveType.I32);
    public static final IntegerBinaryOperations I64 = new IntegerBinaryOperations(PrimitiveType.I64);

    private final PrimitiveType type;

    public IntegerBinaryOperations(PrimitiveType type) {
        this.type = type;
    }

    public long calculateResult(BinaryOperator operator, long vector1, long vector2) throws UndefinedArithmeticResult {
        return calculateResult(operator, BigInteger.valueOf(vector1), BigInteger.valueOf(vector2)).longValue();
    }

    public boolean calculateResult(BinaryOperator operator, boolean vector1, boolean vector2) throws UndefinedArithmeticResult {
        return !calculateResult(operator, BigInteger.valueOf(vector1 ? 1 : 0), BigInteger.valueOf(vector2 ? 1 : 0)).equals(BigInteger.ZERO);
    }

    public BigInteger calculateResult(BinaryOperator operator, BigInteger vector1, BigInteger vector2) throws UndefinedArithmeticResult {
        BigInteger bitwidth = BigInteger.valueOf(2).pow(type.getBitSize());

        switch (operator) {
            case INT_ADD:
                return vector1.add(vector2).remainder(bitwidth);
            case INT_SUBTRACT:
                return vector1.subtract(vector2).remainder(bitwidth);
            case INT_MULTIPLY:
                return vector1.multiply(vector2).remainder(bitwidth);
            case INT_UNSIGNED_DIVIDE:
                if (vector2.equals(BigInteger.ZERO)) {
                    throw new UndefinedArithmeticResult("divison by zero!");
                }
                if (vector1.compareTo(BigInteger.ZERO) < 0 || vector2.compareTo(BigInteger.ZERO) < 0) {
                    throw new UndefinedArithmeticResult("there is a unsigned operation with negative numbers performed");
                }
                return vector1.divide(vector2); // TODO: difference?
            case INT_SIGNED_DIVIDE:
                if (vector2.equals(BigInteger.ZERO)) {
                    throw new UndefinedArithmeticResult("divison by zero!");
                }
                return vector1.divide(vector2);
            case INT_UNSIGNED_REMAINDER:
                if (vector2.equals(BigInteger.ZERO)) {
                    throw new UndefinedArithmeticResult("divison by zero!");
                }
                if (vector1.compareTo(BigInteger.ZERO) < 0 || vector2.compareTo(BigInteger.ZERO) < 0) {
                    throw new UndefinedArithmeticResult("there is a unsigned operation with negative numbers performed");
                }
                return vector1.remainder(vector2); // TODO: difference?
            case INT_SIGNED_REMAINDER:
                if (vector2.equals(BigInteger.ZERO)) {
                    throw new UndefinedArithmeticResult("divison by zero!");
                }
                return vector1.remainder(vector2);
            case INT_SHIFT_LEFT:
                if (vector2.longValue() >= type.getBitSize()) {
                    throw new UndefinedArithmeticResult("right operator is greater or eqal bitwidth");
                }
                return vector1.shiftLeft(vector2.intValue());
            case INT_LOGICAL_SHIFT_RIGHT:
                if (vector2.longValue() >= type.getBitSize()) {
                    throw new UndefinedArithmeticResult("right operator is greater or eqal bitwidth");
                }
                return vector1.shiftRight(vector2.intValue());
            case INT_ARITHMETIC_SHIFT_RIGHT:
                if (vector2.longValue() >= type.getBitSize()) {
                    throw new UndefinedArithmeticResult("right operator is greater or eqal bitwidth");
                }
                return vector1.shiftRight(vector2.intValue());
            case INT_AND:
                return vector1.and(vector2);
            case INT_OR:
                return vector1.or(vector2);
            case INT_XOR:
                return vector1.xor(vector2);
            default:
                fail("unexpected operator");
                return BigInteger.ZERO;
        }
    }

    public class UndefinedArithmeticResult extends Exception {

        private static final long serialVersionUID = 1L;

        public UndefinedArithmeticResult(String message) {
            super(message);
        }
    }
}
