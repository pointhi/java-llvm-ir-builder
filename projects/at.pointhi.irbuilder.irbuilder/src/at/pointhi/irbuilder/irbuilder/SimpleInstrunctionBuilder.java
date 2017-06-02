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
package at.pointhi.irbuilder.irbuilder;

import java.math.BigInteger;

import com.oracle.truffle.llvm.parser.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.parser.model.enums.CastOperator;
import com.oracle.truffle.llvm.parser.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.floatingpoint.FloatingPointConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.BigIntegerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.runtime.types.AggregateType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VariableBitWidthType;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

public class SimpleInstrunctionBuilder {
    final InstructionBuilder builder;

    public SimpleInstrunctionBuilder(FunctionDefinition function) {
        this(new InstructionBuilder(function));
    }

    public SimpleInstrunctionBuilder(InstructionBuilder builder) {
        this.builder = builder;
    }

    private static Constant toConstant(Type type, double value) {
        if (!PrimitiveType.isFloatingpointType(type)) {
            throw new AssertionError("unexpected type: " + type);
        }
        return FloatingPointConstant.create(type, new long[]{Double.doubleToRawLongBits(value)});
    }

    private static Constant toConstant(Type type, boolean value) {
        if (PrimitiveType.isIntegerType(type)) {
            return new IntegerConstant(type, value ? 1 : 0);
        } else if (PrimitiveType.isFloatingpointType(type)) {
            return toConstant(type, value ? 1. : 0.);
        } else {
            throw new AssertionError("unexpected type: " + type);
        }
    }

    private static Constant toConstant(Type type, long value) {
        if (PrimitiveType.isIntegerType(type)) {
            return new IntegerConstant(type, value);
        } else if (PrimitiveType.isFloatingpointType(type)) {
            return toConstant(type, (double) value);
        } else {
            throw new AssertionError("unexpected type: " + type);
        }
    }

    private static Constant toConstant(Type type, BigInteger value) {
        if (PrimitiveType.isIntegerType(type)) {
            if (type instanceof VariableBitWidthType) {
                return new BigIntegerConstant(type, value); // TODO
            } else {
                return new IntegerConstant(type, value.longValue());
            }
        } else if (PrimitiveType.isFloatingpointType(type)) {
            return toConstant(type, (double) value.longValue());
        } else {
            throw new AssertionError("unexpected type: " + type);
        }
    }

    public InstructionBuilder getInstructionBuilder() {
        return builder;
    }

    public FunctionParameter nextParameter() {
        final Type[] types = builder.getFunctionDefinition().getType().getArgumentTypes();
        final int id = builder.getArgCounter();
        final Type paramType;
        if (types.length <= id) {
            paramType = types[id - 1];
        } else {
            paramType = types[types.length - 1];
        }

        return builder.createParameter(paramType);
    }

    // Allocate
    public Instruction allocate(Type type) {
        return builder.createAllocate(type);
    }

    // Binary Operator
    public Instruction binaryOperator(BinaryOperator op, Symbol lhs, Symbol rhs) {
        return builder.createBinaryOperation(lhs, rhs, op);
    }

    public Instruction binaryOperator(BinaryOperator op, boolean lhs, Symbol rhs) {
        return builder.createBinaryOperation(toConstant(rhs.getType(), lhs), rhs, op);
    }

    public Instruction binaryOperator(BinaryOperator op, Symbol lhs, boolean rhs) {
        return builder.createBinaryOperation(lhs, toConstant(lhs.getType(), rhs), op);
    }

    public Instruction binaryOperator(BinaryOperator op, long lhs, Symbol rhs) {
        return builder.createBinaryOperation(toConstant(rhs.getType(), lhs), rhs, op);
    }

    public Instruction binaryOperator(BinaryOperator op, Symbol lhs, long rhs) {
        return builder.createBinaryOperation(lhs, toConstant(lhs.getType(), rhs), op);
    }

    public Instruction binaryOperator(BinaryOperator op, BigInteger lhs, Symbol rhs) {
        return builder.createBinaryOperation(toConstant(rhs.getType(), lhs), rhs, op);
    }

    public Instruction binaryOperator(BinaryOperator op, Symbol lhs, BigInteger rhs) {
        return builder.createBinaryOperation(lhs, toConstant(lhs.getType(), rhs), op);
    }

    public Instruction binaryOperator(BinaryOperator op, double lhs, Symbol rhs) {
        return builder.createBinaryOperation(toConstant(rhs.getType(), lhs), rhs, op);
    }

    public Instruction binaryOperator(BinaryOperator op, Symbol lhs, double rhs) {
        return builder.createBinaryOperation(lhs, toConstant(lhs.getType(), rhs), op);
    }

    // Compare
    public Instruction compare(CompareOperator op, Symbol lhs, Symbol rhs) {
        return builder.createCompare(op, lhs, rhs);
    }

    public Instruction compare(CompareOperator op, boolean lhs, Symbol rhs) {
        return builder.createCompare(op, toConstant(rhs.getType(), lhs), rhs);
    }

    public Instruction compare(CompareOperator op, Symbol lhs, boolean rhs) {
        return builder.createCompare(op, lhs, toConstant(lhs.getType(), rhs));
    }

    public Instruction compare(CompareOperator op, long lhs, Symbol rhs) {
        return builder.createCompare(op, toConstant(rhs.getType(), lhs), rhs);
    }

    public Instruction compare(CompareOperator op, Symbol lhs, long rhs) {
        return builder.createCompare(op, lhs, toConstant(lhs.getType(), rhs));
    }

    public Instruction compare(CompareOperator op, BigInteger lhs, Symbol rhs) {
        return builder.createCompare(op, toConstant(rhs.getType(), lhs), rhs);
    }

    public Instruction compare(CompareOperator op, Symbol lhs, BigInteger rhs) {
        return builder.createCompare(op, lhs, toConstant(lhs.getType(), rhs));
    }

    public Instruction compare(CompareOperator op, double lhs, Symbol rhs) {
        return builder.createCompare(op, toConstant(rhs.getType(), lhs), rhs);
    }

    public Instruction compare(CompareOperator op, Symbol lhs, double rhs) {
        return builder.createCompare(op, lhs, toConstant(lhs.getType(), rhs));
    }

    // Compare Vector (constructed node)
    public Instruction compareVector(CompareOperator op, Symbol lhs, Symbol rhs) {
        Instruction cmpVec = compare(op, lhs, rhs);
        AggregateType type = (AggregateType) cmpVec.getType();

        final BinaryOperator resultComperator;
        switch (op) {
            case INT_NOT_EQUAL:
            case FP_ORDERED_NOT_EQUAL:
            case FP_UNORDERED_NOT_EQUAL:
                resultComperator = BinaryOperator.INT_OR;
                break;
            default:
                resultComperator = BinaryOperator.INT_AND;
                break;
        }

        Instruction cmpRes = extractElement(cmpVec, 0);
        for (int i = 1; i < type.getNumberOfElements(); i++) {
            Instruction extr = extractElement(cmpVec, i);
            // all vector results needs to be joined together
            cmpRes = binaryOperator(resultComperator, cmpRes, extr);
        }

        return cmpRes;
    }

    // Call
    public Instruction call(Symbol target, Symbol... arguments) {
        return builder.createCall(target, arguments);
    }

    // Extract Element
    public Instruction extractElement(Instruction vector, int index) {
        return builder.createExtractElement(vector, index);
    }

    // Fill Vector (constructed node)
    public Instruction fillVector(Instruction source, Constant... values) {
        Instruction vector = load(source);
        for (int i = 0; i < values.length; i++) {
            vector = insertElement(vector, values[i], i);
        }
        return vector;
    }

    public Instruction fillVector(Instruction source, boolean... values) {
        Instruction vector = load(source);
        for (int i = 0; i < values.length; i++) {
            vector = insertElement(vector, values[i], i);
        }
        return vector;
    }

    public Instruction fillVector(Instruction source, long... values) {
        Instruction vector = load(source);
        for (int i = 0; i < values.length; i++) {
            vector = insertElement(vector, values[i], i);
        }
        return vector;
    }

    public Instruction fillVector(Instruction source, BigInteger... values) {
        Instruction vector = load(source);
        for (int i = 0; i < values.length; i++) {
            vector = insertElement(vector, values[i], i);
        }
        return vector;
    }

    public Instruction fillVector(Instruction source, double... values) {
        Instruction vector = load(source);
        for (int i = 0; i < values.length; i++) {
            vector = insertElement(vector, values[i], i);
        }
        return vector;
    }

    // Insert Element
    public Instruction insertElement(Instruction vector, Constant value, int index) {
        return builder.createInsertElement(vector, value, index);
    }

    public Instruction insertElement(Instruction vector, boolean value, int index) {
        AggregateType type = (AggregateType) vector.getType();
        return builder.createInsertElement(vector, toConstant(type.getElementType(index), value), index);
    }

    public Instruction insertElement(Instruction vector, long value, int index) {
        AggregateType type = (AggregateType) vector.getType();
        return builder.createInsertElement(vector, toConstant(type.getElementType(index), value), index);
    }

    public Instruction insertElement(Instruction vector, BigInteger value, int index) {
        AggregateType type = (AggregateType) vector.getType();
        return builder.createInsertElement(vector, toConstant(type.getElementType(index), value), index);
    }

    public Instruction insertElement(Instruction vector, double value, int index) {
        AggregateType type = (AggregateType) vector.getType();
        return builder.createInsertElement(vector, toConstant(type.getElementType(index), value), index);
    }

    // Load
    public Instruction load(Instruction source) {
        return builder.createLoad(source);
    }

    // Return
    public void returnx(Symbol value) {
        builder.createReturn(value);
    }

    public void returnx() {
        builder.createReturn();
    }

    // va_arg
    public void vaStart(FunctionDeclaration vaStartDecl, Symbol vaListTag) {
        Instruction vaArrayPtr = builder.createGetElementPointer(vaListTag, new Symbol[]{new IntegerConstant(PrimitiveType.I32, 0), new IntegerConstant(PrimitiveType.I32, 0)}, true);
        Instruction vaBytePtr = builder.createCast(new PointerType(PrimitiveType.I8), CastOperator.BITCAST, vaArrayPtr);
        call(vaStartDecl, vaBytePtr);
    }

    public void vaEnd(FunctionDeclaration vaEndDecl, Symbol vaListTag) {
        Instruction vaArrayPtr = builder.createGetElementPointer(vaListTag, new Symbol[]{new IntegerConstant(PrimitiveType.I32, 0), new IntegerConstant(PrimitiveType.I32, 0)}, true);
        Instruction vaBytePtr = builder.createCast(new PointerType(PrimitiveType.I8), CastOperator.BITCAST, vaArrayPtr);
        call(vaEndDecl, vaBytePtr);
    }

    public Instruction vaArgWorkaround(Symbol vaListTag, @SuppressWarnings("unused") Type type) {
        Instruction vaListPtr = builder.createGetElementPointer(vaListTag, new Symbol[]{new IntegerConstant(PrimitiveType.I32, 0), new IntegerConstant(PrimitiveType.I32, 0)}, true);
        Instruction intPtr = builder.createGetElementPointer(vaListPtr, new Symbol[]{new IntegerConstant(PrimitiveType.I32, 0), new IntegerConstant(PrimitiveType.I32, 0)}, true);
        Instruction loadRes = this.load(intPtr);
        return loadRes;
    }

}
