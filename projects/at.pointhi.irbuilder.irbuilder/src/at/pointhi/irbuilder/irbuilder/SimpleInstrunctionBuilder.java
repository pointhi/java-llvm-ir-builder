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

import com.oracle.truffle.llvm.parser.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.parser.model.enums.CastOperator;
import com.oracle.truffle.llvm.parser.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.TerminatingInstruction;
import com.oracle.truffle.llvm.runtime.types.AggregateType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VectorType;
import com.oracle.truffle.llvm.runtime.types.VoidType;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

import at.pointhi.irbuilder.irbuilder.helper.LLVMIntrinsics;
import at.pointhi.irbuilder.irbuilder.helper.LLVMIntrinsics.VA_LIST_TAG_TYPE;
import at.pointhi.irbuilder.irbuilder.util.ConstantUtil;

public class SimpleInstrunctionBuilder {
    private final ModelModuleBuilder modelBuilder;
    private final InstructionBuilder builder;

    public SimpleInstrunctionBuilder(ModelModuleBuilder modelBuilder, FunctionDefinition function) {
        this(modelBuilder, new InstructionBuilder(function));
    }

    public SimpleInstrunctionBuilder(ModelModuleBuilder modelBuilder, InstructionBuilder builder) {
        this.modelBuilder = modelBuilder;
        this.builder = builder;
    }

    public InstructionBuilder getInstructionBuilder() {
        return builder;
    }

    public InstructionBlock nextBlock() {
        if (!(builder.getLastInstruction() instanceof TerminatingInstruction)) {
            throw new AssertionError("The last instruction of a block has to be a terminating instruction!");
        }
        return builder.nextBlock();
    }

    public InstructionBlock getCurrentBlock() {
        return builder.getCurrentBlock();
    }

    public InstructionBlock getNextBlock() {
        return builder.getNextBlock();
    }

    public InstructionBlock getBlock(int idx) {
        return builder.getBlock(idx);
    }

    public void insertBlocks(int count) {
        builder.insertBlocks(count);
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
        if (!lhs.getType().equals(rhs.getType())) {
            throw new AssertionError("Both arguments must have same type! (" + lhs.getType() + " != " + rhs.getType() + ")");
        }
        final Type type = lhs.getType();
        if (op.isFloatingPoint() && (Type.isIntegerType(type) || (type instanceof VectorType && Type.isIntegerType(((VectorType) type).getElementType())))) {
            throw new AssertionError("You cannot use Floating Point operators with Integer Variables!");
        }
        if (!op.isFloatingPoint() && (Type.isFloatingpointType(type) || (type instanceof VectorType && Type.isFloatingpointType(((VectorType) type).getElementType())))) {
            throw new AssertionError("You cannot use Integer operators with Floating Point Variables!");
        }
        return builder.createBinaryOperation(lhs, rhs, op);
    }

    public Instruction binaryOperator(BinaryOperator op, boolean lhs, Symbol rhs) {
        return binaryOperator(op, ConstantUtil.getConst(rhs.getType(), lhs), rhs);
    }

    public Instruction binaryOperator(BinaryOperator op, Symbol lhs, boolean rhs) {
        return binaryOperator(op, lhs, ConstantUtil.getConst(lhs.getType(), rhs));
    }

    public Instruction binaryOperator(BinaryOperator op, long lhs, Symbol rhs) {
        return binaryOperator(op, ConstantUtil.getConst(rhs.getType(), lhs), rhs);
    }

    public Instruction binaryOperator(BinaryOperator op, Symbol lhs, long rhs) {
        return binaryOperator(op, lhs, ConstantUtil.getConst(lhs.getType(), rhs));
    }

    public Instruction binaryOperator(BinaryOperator op, BigInteger lhs, Symbol rhs) {
        return binaryOperator(op, ConstantUtil.getConst(rhs.getType(), lhs), rhs);
    }

    public Instruction binaryOperator(BinaryOperator op, Symbol lhs, BigInteger rhs) {
        return binaryOperator(op, lhs, ConstantUtil.getConst(lhs.getType(), rhs));
    }

    public Instruction binaryOperator(BinaryOperator op, double lhs, Symbol rhs) {
        return binaryOperator(op, ConstantUtil.getConst(rhs.getType(), lhs), rhs);
    }

    public Instruction binaryOperator(BinaryOperator op, Symbol lhs, double rhs) {
        return binaryOperator(op, lhs, ConstantUtil.getConst(lhs.getType(), rhs));
    }

    // Branch
    public Instruction jump(InstructionBlock block) {
        return builder.createBranch(block);
    }

    public Instruction branch(Symbol condition, InstructionBlock ifBlock, InstructionBlock elseBlock) {
        return builder.createBranch(condition, ifBlock, elseBlock);
    }

    // Cast
    public Instruction cast(CastOperator op, Type type, Symbol value) {
        return builder.createCast(type, op, value);
    }

    // Compare
    public Instruction compare(CompareOperator op, Symbol lhs, Symbol rhs) {
        if (!lhs.getType().equals(rhs.getType())) {
            throw new AssertionError("Both arguments must have same type! (" + lhs.getType() + " != " + rhs.getType() + ")");
        }
        final Type type = lhs.getType();
        if (op.isFloatingPoint() && (Type.isIntegerType(type) || type instanceof PointerType || (type instanceof VectorType && Type.isIntegerType(((VectorType) type).getElementType())))) {
            throw new AssertionError("You cannot use Floating Point operators with Integer Variables!");
        }
        if (!op.isFloatingPoint() && (Type.isFloatingpointType(type) || (type instanceof VectorType && Type.isFloatingpointType(((VectorType) type).getElementType())))) {
            throw new AssertionError("You cannot use Integer operators with Floating Point Variables!");
        }
        return builder.createCompare(op, lhs, rhs);
    }

    public Instruction compare(CompareOperator op, boolean lhs, Symbol rhs) {
        return compare(op, ConstantUtil.getConst(rhs.getType(), lhs), rhs);
    }

    public Instruction compare(CompareOperator op, Symbol lhs, boolean rhs) {
        return compare(op, lhs, ConstantUtil.getConst(lhs.getType(), rhs));
    }

    public Instruction compare(CompareOperator op, long lhs, Symbol rhs) {
        return compare(op, ConstantUtil.getConst(rhs.getType(), lhs), rhs);
    }

    public Instruction compare(CompareOperator op, Symbol lhs, long rhs) {
        return compare(op, lhs, ConstantUtil.getConst(lhs.getType(), rhs));
    }

    public Instruction compare(CompareOperator op, BigInteger lhs, Symbol rhs) {
        return compare(op, ConstantUtil.getConst(rhs.getType(), lhs), rhs);
    }

    public Instruction compare(CompareOperator op, Symbol lhs, BigInteger rhs) {
        return compare(op, lhs, ConstantUtil.getConst(lhs.getType(), rhs));
    }

    public Instruction compare(CompareOperator op, double lhs, Symbol rhs) {
        return compare(op, ConstantUtil.getConst(rhs.getType(), lhs), rhs);
    }

    public Instruction compare(CompareOperator op, Symbol lhs, double rhs) {
        return compare(op, lhs, ConstantUtil.getConst(lhs.getType(), rhs));
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
        if (index < 0 || ((AggregateType) vector.getType()).getNumberOfElements() <= index) {
            throw new AssertionError("Cannot extract an element at index " + index + " of the type " + vector.getType());
        }
        return builder.createExtractElement(vector, index);
    }

    // Fill Vector (constructed node)
    public Instruction fillVector(Instruction source, Constant... values) {
        Instruction vector = load(source);
        for (int i = 0; i < values.length; i++) {
            vector = insertElement(vector, values[i], i);
        }
        store(source, vector);
        return vector;
    }

    public Instruction fillVector(Instruction source, boolean... values) {
        Instruction vector = load(source);
        for (int i = 0; i < values.length; i++) {
            vector = insertElement(vector, values[i], i);
        }
        store(source, vector);
        return vector;
    }

    public Instruction fillVector(Instruction source, long... values) {
        Instruction vector = load(source);
        for (int i = 0; i < values.length; i++) {
            vector = insertElement(vector, values[i], i);
        }
        store(source, vector);
        return vector;
    }

    public Instruction fillVector(Instruction source, BigInteger... values) {
        Instruction vector = load(source);
        for (int i = 0; i < values.length; i++) {
            vector = insertElement(vector, values[i], i);
        }
        store(source, vector);
        return vector;
    }

    public Instruction fillVector(Instruction source, double... values) {
        Instruction vector = load(source);
        for (int i = 0; i < values.length; i++) {
            vector = insertElement(vector, values[i], i);
        }
        store(source, vector);
        return vector;
    }

    // GetElementPointer
    public Instruction getElementPointer(Symbol base, Symbol... indices) {
        return builder.createGetElementPointer(base, indices, false);
    }

    public Instruction getElementPointer(Symbol base, int... indices) {
        Symbol[] symIndices = new Symbol[indices.length];
        for (int i = 0; i < indices.length; i++) {
            symIndices[i] = ConstantUtil.getI32Const(indices[i]);
        }
        return builder.createGetElementPointer(base, symIndices, false);
    }

    public Instruction getElementPointerInbounds(Symbol base, Symbol... indices) {
        return builder.createGetElementPointer(base, indices, true);
    }

    public Instruction getElementPointerInbounds(Symbol base, int... indices) {
        Symbol[] symIndices = new Symbol[indices.length];
        for (int i = 0; i < indices.length; i++) {
            symIndices[i] = ConstantUtil.getI32Const(indices[i]);
        }
        return builder.createGetElementPointer(base, symIndices, true);
    }

    // Indirect Branch
    public Instruction indirectBranch(Symbol address, InstructionBlock... successors) {
        return builder.createIndirectBranch(address, successors);
    }

    // Insert Element
    public Instruction insertElement(Instruction vector, Constant value, int index) {
        AggregateType type = (AggregateType) vector.getType();
        if (index < 0 || index >= type.getNumberOfElements()) {
            throw new AssertionError("Cannot insert an element at index " + index + " into the type " + type);
        }
        if (!type.getElementType(index).equals(value.getType())) {
            throw new AssertionError("Cannot insert an element with the type " + value.getType() + " at index " + index + " of " + type);
        }
        return builder.createInsertElement(vector, value, index);
    }

    public Instruction insertElement(Instruction vector, boolean value, int index) {
        AggregateType type = (AggregateType) vector.getType();
        return insertElement(vector, ConstantUtil.getConst(type.getElementType(index), value), index);
    }

    public Instruction insertElement(Instruction vector, long value, int index) {
        AggregateType type = (AggregateType) vector.getType();
        return insertElement(vector, ConstantUtil.getConst(type.getElementType(index), value), index);
    }

    public Instruction insertElement(Instruction vector, BigInteger value, int index) {
        AggregateType type = (AggregateType) vector.getType();
        return insertElement(vector, ConstantUtil.getConst(type.getElementType(index), value), index);
    }

    public Instruction insertElement(Instruction vector, double value, int index) {
        AggregateType type = (AggregateType) vector.getType();
        return insertElement(vector, ConstantUtil.getConst(type.getElementType(index), value), index);
    }

    // Insert Value
    public Instruction insertValue(Instruction struct, Symbol value, int index) {
        Symbol aggregate = struct; // TODO: works?
        return builder.createInsertValue(struct, aggregate, index, value);
    }

    public Instruction insertValue(Instruction struct, boolean value, int index) {
        AggregateType type = (AggregateType) struct.getType();
        return insertValue(struct, ConstantUtil.getConst(type.getElementType(index), value), index);
    }

    public Instruction insertValue(Instruction struct, long value, int index) {
        AggregateType type = (AggregateType) struct.getType();
        return insertValue(struct, ConstantUtil.getConst(type.getElementType(index), value), index);
    }

    public Instruction insertValue(Instruction struct, BigInteger value, int index) {
        AggregateType type = (AggregateType) struct.getType();
        return insertValue(struct, ConstantUtil.getConst(type.getElementType(index), value), index);
    }

    public Instruction insertValue(Instruction struct, double value, int index) {
        AggregateType type = (AggregateType) struct.getType();
        return insertValue(struct, ConstantUtil.getConst(type.getElementType(index), value), index);
    }

    // Load
    public Instruction load(Instruction source) {
        return builder.createLoad(source);
    }

    // Phi
    public Instruction phi(Type type, Symbol[] values, InstructionBlock[] blocks) {
        if (values.length != blocks.length) {
            throw new AssertionError("number of values and blocks has to be equal");
        }
        return builder.createPhi(type, values, blocks);
    }

    // Return
    public void returnx(Symbol value) {
        final Type functionReturnType = builder.getFunctionDefinition().getType().getReturnType();
        if (!functionReturnType.equals(value.getType())) {
            throw new AssertionError("Return type does not match function definition (" + value.getType() + " != " + functionReturnType + ")");
        }
        builder.createReturn(value);
        builder.exitFunction();
    }

    public void returnxWithCast(Symbol value) {
        final Type returnType = builder.getFunctionDefinition().getType().getReturnType();
        final Symbol castedValue;
        if (returnType.equals(value.getType())) {
            castedValue = value;
        } else {
            castedValue = cast(CastOperator.ZERO_EXTEND, returnType, value);
        }
        returnx(castedValue);
    }

    public void returnx() {
        final Type functionReturnType = builder.getFunctionDefinition().getType().getReturnType();
        if (!functionReturnType.equals(VoidType.INSTANCE)) {
            throw new AssertionError("Function definition requires a non void return type: " + functionReturnType);
        }
        builder.createReturn();
        builder.exitFunction();
    }

    // Select
    public Instruction select(Type type, Symbol condition, Symbol trueValue, Symbol falseValue) {
        return builder.createSelect(type, condition, trueValue, falseValue);
    }

    // Shuffle Vector
    public Instruction shuffleVector(Type type, Symbol vector1, Symbol vector2, Symbol mask) {
        return builder.createShuffleVector(type, vector1, vector2, mask);
    }

    // Store
    public Instruction store(Symbol destination, Symbol source, int align) {
        if (source.getType().equals(destination.getType())) {
            throw new AssertionError("destinatination type is not compatible to source type");
        }
        return builder.createStore(destination, source, align);
    }

    public Instruction store(Symbol destination, Symbol source) {
        return store(destination, source, 0);
    }

    // Switch
    public Instruction switchx(Symbol condition, InstructionBlock defaultBlock, Symbol[] caseValues, InstructionBlock[] caseBlocks) {
        return builder.createSwitch(condition, defaultBlock, caseValues, caseBlocks);
    }

    // Unreachable
    public Instruction unreachable() {
        return builder.createUnreachable();
    }

    // va_arg for x86_64-unknown-linux-gnu
    public void vaStartAMD64(Symbol vaListTag) {
        Instruction vaArrayPtr = getElementPointerInbounds(vaListTag, 0, 0);
        Instruction vaBytePtr = cast(CastOperator.BITCAST, new PointerType(PrimitiveType.I8), vaArrayPtr);
        call(LLVMIntrinsics.getLlvmVaStart(modelBuilder), vaBytePtr);
    }

    public void vaEndAMD64(Symbol vaListTag) {
        Instruction vaArrayPtr = getElementPointerInbounds(vaListTag, 0, 0);
        Instruction vaBytePtr = cast(CastOperator.BITCAST, new PointerType(PrimitiveType.I8), vaArrayPtr);
        call(LLVMIntrinsics.getLlvmVaEnd(modelBuilder), vaBytePtr);
    }

    /**
     * Get the next VarArg of a AMD64 compiled function declared by the %struct.__va_list_tag
     * variable.
     *
     * Please note because of the complexity of this function, that this function is adding
     * additional InstructionBlocks into the function. This needs to be considered when a branch is
     * jumping over this function.
     *
     * @see "https://software.intel.com/sites/default/files/article/402129/mpx-linux64-abi.pdf"
     *
     * @param vaListTag our %struct.__va_list_tag
     * @param type type of the variable we want to get
     * @return the next vararg
     */
    public Instruction vaArgAMD64(Symbol vaListTag, Type type) {
        final int curBlockIdx = getCurrentBlock().getBlockIndex();
        insertBlocks(3);
        InstructionBlock i11 = getBlock(curBlockIdx + 1);
        InstructionBlock i17 = getBlock(curBlockIdx + 2);
        InstructionBlock i22 = getBlock(curBlockIdx + 3);

        // Is register available?
        Instruction i7 = getElementPointerInbounds(vaListTag, 0, 0);
        final Instruction i8;
        final Instruction i9;
        final Instruction i10;
        if (Type.isIntegerType(type)) {
            i8 = getElementPointerInbounds(i7, ConstantUtil.getI32Const(0), ConstantUtil.getI32Const(VA_LIST_TAG_TYPE.GP_OFFSET.getIdx()));
            i9 = this.load(i8);
            i10 = compare(CompareOperator.INT_UNSIGNED_LESS_OR_EQUAL, i9, 40);
        } else if (Type.isFloatingpointType(type)) {
            i8 = getElementPointerInbounds(i7, ConstantUtil.getI32Const(0), ConstantUtil.getI32Const(VA_LIST_TAG_TYPE.FP_OFFSET.getIdx()));
            i9 = this.load(i8);
            i10 = compare(CompareOperator.INT_UNSIGNED_LESS_OR_EQUAL, i9, 160);
        } else {
            throw new AssertionError("type not implemented yet: " + type);
        }
        branch(i10, i11, i17);

        nextBlock(); // 11
        assert getCurrentBlock() == i11;

        // Address of saved register
        Instruction i12 = getElementPointerInbounds(i7, ConstantUtil.getI32Const(0), ConstantUtil.getI32Const(VA_LIST_TAG_TYPE.REG_SAVE_AREA.getIdx()));
        Instruction i13 = load(i12);
        Instruction i14 = getElementPointer(i13, i9);
        Instruction i15 = cast(CastOperator.BITCAST, new PointerType(type), i14);

        // Update gp_offset
        final int offset;
        if (Type.isIntegerType(type)) {
            offset = 8;
        } else if (Type.isFloatingpointType(type)) {
            offset = 16;
        } else {
            throw new AssertionError("type not implemented yet: " + type);
        }
        Instruction i16 = binaryOperator(BinaryOperator.INT_ADD, i9, offset);
        store(i8, i16, 16);
        jump(i22);

        nextBlock(); // 17
        assert getCurrentBlock() == i17;

        // Address of stack slot
        Instruction i18 = getElementPointerInbounds(i7, ConstantUtil.getI32Const(0), ConstantUtil.getI32Const(VA_LIST_TAG_TYPE.OVERFLOW_ARG_AREA.getIdx()));
        Instruction i19 = load(i18);
        Instruction i20 = cast(CastOperator.BITCAST, new PointerType(type), i19);
        Instruction i21 = getElementPointer(i19, 8);
        // update to next available stack slot
        store(i18, i21, 8);
        jump(i22);

        nextBlock(); // 22
        assert getCurrentBlock() == i22;
        Instruction i23 = phi(new PointerType(type), new Symbol[]{i15, i20}, new InstructionBlock[]{i11, i17});
        // Load argument
        Instruction i24 = load(i23);

        return i24;
    }

    // TODO: private
    public Instruction vaArgAMD64StackOnly(Symbol vaListTag, AggregateType type) {
        Instruction i7 = getElementPointerInbounds(vaListTag, 0, 0);

        // Address of stack slot
        Instruction i8 = getElementPointerInbounds(i7, ConstantUtil.getI32Const(0), ConstantUtil.getI32Const(VA_LIST_TAG_TYPE.OVERFLOW_ARG_AREA.getIdx()));
        Instruction i9 = load(i8);
        Instruction i10 = cast(CastOperator.BITCAST, new PointerType(type), i9);
        // TODO: align
        Instruction i11 = getElementPointer(i9, ConstantUtil.getI32Const(type.getSize(InstructionBuilder.targetDataLayout)));

        // update to next available stack slot
        store(i8, i11, 8);

        // copy into new object
        Instruction i4 = allocate(type);
        Instruction i12 = cast(CastOperator.BITCAST, new PointerType(PrimitiveType.I8), i4);
        Instruction i13 = cast(CastOperator.BITCAST, new PointerType(PrimitiveType.I8), i10);

        // TODO: align
        call(LLVMIntrinsics.getLlvmMemcpyP0i8P0i8i64(modelBuilder),
                        i12,
                        i13,
                        ConstantUtil.getI64Const(type.getSize(InstructionBuilder.targetDataLayout)),
                        ConstantUtil.getI32Const(4),
                        ConstantUtil.getI1Const(false));

        return i4;
    }

}
