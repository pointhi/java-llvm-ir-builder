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
package at.pointhi.irbuilder.irbuilder.util;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.oracle.truffle.llvm.parser.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.floatingpoint.FloatingPointConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.BigIntegerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.runtime.floating.LLVM80BitFloat;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VariableBitWidthType;

public final class ConstantUtil {
    private ConstantUtil() {
    }

    public static IntegerConstant getI1Const(boolean val) {
        return new IntegerConstant(PrimitiveType.I1, val ? 1 : 0);
    }

    public static IntegerConstant getI8Const(byte val) {
        return new IntegerConstant(PrimitiveType.I8, val);
    }

    public static IntegerConstant getI16Const(short val) {
        return new IntegerConstant(PrimitiveType.I16, val);
    }

    public static IntegerConstant getI32Const(int val) {
        return new IntegerConstant(PrimitiveType.I32, val);
    }

    public static IntegerConstant getI64Const(long val) {
        return new IntegerConstant(PrimitiveType.I64, val);
    }

    public static FloatingPointConstant getFloatConst(float val) {
        return FloatingPointConstant.create(PrimitiveType.FLOAT, new long[]{Float.floatToRawIntBits(val)});
    }

    public static FloatingPointConstant getDoubleConst(double val) {
        return FloatingPointConstant.create(PrimitiveType.DOUBLE, new long[]{Double.doubleToRawLongBits(val)});
    }

    public static FloatingPointConstant getFP80Const(double val) {
        ByteBuffer buffer = ByteBuffer.allocate(PrimitiveType.X86_FP80.getBitSize() / Byte.SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(LLVM80BitFloat.fromDouble(val).getBytes());
        buffer.flip();

        long byte2 = buffer.getShort(); // TODO: Why in reversed order?
        long byte1 = buffer.getLong();

        long[] bytes = new long[]{byte1, byte2};

        return FloatingPointConstant.create(PrimitiveType.X86_FP80, bytes);
    }

    public static final FloatingPointConstant X86_FP80_SNaN = FloatingPointConstant.create(PrimitiveType.X86_FP80, new long[]{0x7FFFA000_00000000L, 0x0});

    public static Constant getConst(Type type, double value) {
        if (!PrimitiveType.isFloatingpointType(type)) {
            throw new AssertionError("unexpected type: " + type);
        }
        if (type.equals(PrimitiveType.FLOAT)) {
            return getFloatConst((float) value);
        } else if (type.equals(PrimitiveType.DOUBLE)) {
            return getDoubleConst(value);
        } else if (type.equals(PrimitiveType.X86_FP80)) {
            return getFP80Const(value);
        } else {
            throw new AssertionError("unsuported floatingpoint type: " + type);
        }

    }

    public static Constant getConst(Type type, boolean value) {
        if (PrimitiveType.isIntegerType(type)) {
            return new IntegerConstant(type, value ? 1 : 0);
        } else if (PrimitiveType.isFloatingpointType(type)) {
            return getConst(type, value ? 1. : 0.);
        } else {
            throw new AssertionError("unexpected type: " + type);
        }
    }

    public static Constant getConst(Type type, long value) {
        if (PrimitiveType.isIntegerType(type)) {
            return new IntegerConstant(type, value);
        } else if (PrimitiveType.isFloatingpointType(type)) {
            return getConst(type, (double) value);
        } else {
            throw new AssertionError("unexpected type: " + type);
        }
    }

    public static Constant getConst(Type type, BigInteger value) {
        if (PrimitiveType.isIntegerType(type)) {
            if (type instanceof VariableBitWidthType) {
                return new BigIntegerConstant(type, value); // TODO
            } else {
                return new IntegerConstant(type, value.longValue());
            }
        } else if (PrimitiveType.isFloatingpointType(type)) {
            return getConst(type, (double) value.longValue());
        } else {
            throw new AssertionError("unexpected type: " + type);
        }
    }
}
