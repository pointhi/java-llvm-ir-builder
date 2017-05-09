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

import com.oracle.truffle.llvm.parser.datalayout.DataLayoutConverter;
import com.oracle.truffle.llvm.parser.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.parser.model.enums.CastOperator;
import com.oracle.truffle.llvm.parser.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.model.symbols.Symbols;
import com.oracle.truffle.llvm.parser.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ValueInstruction;
import com.oracle.truffle.llvm.runtime.types.AggregateType;
import com.oracle.truffle.llvm.runtime.types.DataSpecConverter;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.LLVMIdentifier;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

// TODO: https://github.com/pointhi/sulong/blob/1cc13ee850034242fd3406e29cd003b06f065c15/projects/com.oracle.truffle.llvm.writer/src/com/oracle/truffle/llvm/writer/facades/InstructionGeneratorFacade.java
public class InstructionBuilder {
    private static final String x86TargetDataLayout = "e-p:64:64:64-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:128:128-a0:0:64-s0:64:64-f80:128:128-n8:16:32:64-S128";
    public static final DataSpecConverter targetDataLayout = DataLayoutConverter.getConverter(x86TargetDataLayout);

    private final FunctionDefinition function;

    private InstructionBlock curBlock;

    private int counter = 1;
    private int argCounter = 1;

    public InstructionBuilder(FunctionDefinition function) {
        this.function = function;
        this.curBlock = function.generateBlock();
    }

    public FunctionParameter createParameter(Type type) {
        function.createParameter(type);
        FunctionParameter newParam = function.getParameters().get(function.getParameters().size() - 1);
        newParam.setName("arg_" + Integer.toString(argCounter++));
        return newParam;
    }

    public void nextBlock() {
        curBlock = function.generateBlock();
        curBlock.setName(Integer.toString(counter++)); // TODO: required?
    }

    public void exitFunction() {
        function.exitFunction();
    }

    private static IntegerConstant createI32Constant(int value) {
        return new IntegerConstant(PrimitiveType.I32, value);
    }

    /**
     * Add a new Symbol to the Symbol list, and return it's given symbol position.
     */
    private int addSymbol(Symbol sym) {
        Symbols symbols = function.getSymbols();
        symbols.addSymbol(sym);
        return symbols.getSize() - 1; // return index of new symbol
    }

    /**
     * Get the last instruction added to the function.
     */
    private Instruction getLastInstruction() {
        Instruction lastInstr = curBlock.getInstruction(curBlock.getInstructionCount() - 1);
        if (lastInstr instanceof ValueInstruction) {
            ValueInstruction lastValueInstr = (ValueInstruction) lastInstr;
            if (lastValueInstr.getName().equals(LLVMIdentifier.UNKNOWN)) {
                lastValueInstr.setName(Integer.toString(counter++));
            }
        }
        return lastInstr;
    }

    private static int calculateAlign(int align) {
        assert Integer.highestOneBit(align) == align;

        return align == 0 ? 0 : Integer.numberOfTrailingZeros(align) + 1;
    }

    public Instruction createAllocate(Type type) {
        Type pointerType = new PointerType(type);
        int count = addSymbol(createI32Constant(1));
        int align = type.getAlignment(targetDataLayout);
        curBlock.createAllocation(pointerType, count, calculateAlign(align));
        return getLastInstruction();
    }

    public Instruction createAtomicLoad(Type type, Instruction source, int align, boolean isVolatile, long atomicOrdering, long synchronizationScope) {
        int sourceIdx = addSymbol(source);
        curBlock.createAtomicLoad(type, sourceIdx, calculateAlign(align), isVolatile, atomicOrdering, synchronizationScope);
        return getLastInstruction();
    }

    public Instruction createAtomicStore(Instruction destination, Instruction source, int align, boolean isVolatile, long atomicOrdering, long synchronizationScope) {
        int destinationIdx = addSymbol(destination);
        int sourceIdx = addSymbol(source);
        curBlock.createAtomicStore(destinationIdx, sourceIdx, calculateAlign(align), isVolatile, atomicOrdering, synchronizationScope);
        return getLastInstruction();
    }

    public Instruction createBinaryOperation(Symbol lhs, Symbol rhs, BinaryOperator op) {
        Type type = lhs.getType();
        int flagbits = 0; // TODO: flags are not supported yet
        int lhsIdx = addSymbol(lhs);
        int rhsIdx = addSymbol(rhs);
        curBlock.createBinaryOperation(type, op.ordinal(), flagbits, lhsIdx, rhsIdx);
        return getLastInstruction();
    }

    public Instruction createBranch(int block) {
        curBlock.createBranch(block);
        return getLastInstruction();
    }

    public Instruction createBranch(Symbol condition, int ifBlock, int elseBlock) {
        int conditionIdx = addSymbol(condition);

        curBlock.createBranch(conditionIdx, ifBlock, elseBlock);
        return getLastInstruction();
    }

    // TODO: createCall

    public Instruction createCast(Type type, CastOperator op, Symbol value) {
        int valueIdx = addSymbol(value);
        curBlock.createCast(type, op.ordinal(), valueIdx);
        return getLastInstruction();
    }

    public Instruction createCompare(CompareOperator op, Symbol lhs, Symbol rhs) {
        Type type = lhs.getType();
        int lhsIdx = addSymbol(lhs);
        int rhsIdx = addSymbol(rhs);
        curBlock.createCompare(type, op.getIrIndex(), lhsIdx, rhsIdx);
        return getLastInstruction();
    }

    @SuppressWarnings("unused")
    public Instruction createExtractValue(Instruction struct, Symbol vector, int index) {
        Type type = ((AggregateType) vector.getType()).getElementType(index); // TODO: correct?
        int vectorIdx = addSymbol(vector);
        int indexIdx = addSymbol(createI32Constant(index));
        curBlock.createExtractElement(type, vectorIdx, indexIdx);
        return getLastInstruction();
    }

    public Instruction createExtractElement(Instruction vector, int index) {
        Type type = ((AggregateType) vector.getType()).getElementType(index);
        int vectorIdx = addSymbol(vector);
        int indexIdx = addSymbol(createI32Constant(index));
        curBlock.createExtractElement(type, vectorIdx, indexIdx);
        return getLastInstruction();
    }

    public Instruction createGetElementPointer(Type type, Symbol base, Symbol[] indices, boolean isInbounds) {
        int pointerIdx = addSymbol(base);
        int[] indicesIdx = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            indicesIdx[i] = addSymbol(indices[i]);
        }
        curBlock.createGetElementPointer(type, pointerIdx, indicesIdx, isInbounds);
        return getLastInstruction();
    }

    public Instruction createIndirectBranch(Symbol address, int[] successors) {
        int addressIdx = addSymbol(address);
        curBlock.createIndirectBranch(addressIdx, successors);
        return getLastInstruction();
    }

    public Instruction createInsertElement(Instruction vector, Constant value, int index) {
        Type type = vector.getType();
        int vectorIdx = addSymbol(vector);
        int valueIdx = addSymbol(value);
        int indexIdx = addSymbol(new IntegerConstant(PrimitiveType.I32, index));
        curBlock.createInsertElement(type, vectorIdx, indexIdx, valueIdx);
        return getLastInstruction();
    }

    public Instruction createInsertValue(Instruction struct, Symbol aggregate, int index, Symbol value) {
        Type type = struct.getType(); // TODO: correct?
        int valueIdx = addSymbol(value);
        int aggregateIdx = addSymbol(aggregate);
        curBlock.createInsertValue(type, aggregateIdx, index, valueIdx);
        return getLastInstruction();
    }

    public Instruction createLoad(Instruction source) {
        Type type = ((PointerType) source.getType()).getPointeeType();
        int sourceIdx = addSymbol(source);
        int align = type.getAlignment(targetDataLayout);
        // because we don't have any optimizations, we can set isVolatile to false
        boolean isVolatile = false;
        curBlock.createLoad(type, sourceIdx, calculateAlign(align), isVolatile);
        return getLastInstruction();
    }

    public Instruction createPhi(Type type, int[] values, InstructionBlock[] blocks) {
        assert values.length == blocks.length;

        int[] valuesIdx = new int[values.length];
        int[] blocksIdx = new int[blocks.length];
        for (int i = 0; i < blocks.length; i++) {
            valuesIdx[i] = addSymbol(createI32Constant(values[i]));
            blocksIdx[i] = addSymbol(blocks[i]);
        }
        curBlock.createPhi(type, values, blocksIdx);
        return getLastInstruction();
    }

    public Instruction createReturn() {
        curBlock.createReturn();
        return getLastInstruction();
    }

    public Instruction createReturn(Symbol value) {
        int valueIdx = addSymbol(value);
        curBlock.createReturn(valueIdx);
        return getLastInstruction();
    }

    public Instruction createSelect(Type type, Symbol condition, Symbol trueValue, Symbol falseValue) {
        int conditionIdx = addSymbol(condition);
        int trueValueIdx = addSymbol(trueValue);
        int falseValueIdx = addSymbol(falseValue);
        curBlock.createSelect(type, conditionIdx, trueValueIdx, falseValueIdx);
        return getLastInstruction();
    }

    public Instruction createShuffleVector(Type type, Symbol vector1, Symbol vector2, Symbol mask) {
        int vector1Idx = addSymbol(vector1);
        int vector2Idx = addSymbol(vector2);
        int maskIdx = addSymbol(mask);
        curBlock.createShuffleVector(type, vector1Idx, vector2Idx, maskIdx);
        return getLastInstruction();
    }

    public Instruction createStore(Symbol destination, Symbol source, int align) {
        int destinationIdx = addSymbol(destination);
        int sourceIdx = addSymbol(source);
        // because we don't have any optimizations, we can set isVolatile to false
        boolean isVolatile = false;
        curBlock.createStore(destinationIdx, sourceIdx, calculateAlign(align), isVolatile);
        return getLastInstruction();
    }

    public Instruction createSwitch(Symbol condition, InstructionBlock defaultBlock, Symbol[] caseValues, InstructionBlock[] caseBlocks) {
        assert caseValues.length == caseBlocks.length;

        int conditionIdx = addSymbol(condition);
        int defaultBlockIdx = defaultBlock.getBlockIndex();

        int[] caseValuesIdx = new int[caseValues.length];
        int[] caseBlocksIdx = new int[caseBlocks.length];
        for (int i = 0; i < caseBlocks.length; i++) {
            caseValuesIdx[i] = addSymbol(caseValues[i]);
            caseBlocksIdx[i] = caseBlocks[i].getBlockIndex();
        }

        curBlock.createSwitch(conditionIdx, defaultBlockIdx, caseValuesIdx, caseBlocksIdx);
        return getLastInstruction();
    }

    public Instruction createSwitchOld(Symbol condition, InstructionBlock defaultBlock, long[] caseConstants, InstructionBlock[] caseBlocks) {
        assert caseConstants.length == caseBlocks.length;

        int conditionIdx = addSymbol(condition);
        int defaultBlockIdx = defaultBlock.getBlockIndex();

        int[] caseBlocksIdx = new int[caseBlocks.length];
        for (int i = 0; i < caseBlocks.length; i++) {
            caseBlocksIdx[i] = caseBlocks[i].getBlockIndex();
        }

        curBlock.createSwitchOld(conditionIdx, defaultBlockIdx, caseConstants, caseBlocksIdx); // TODO
        return getLastInstruction();
    }

    public Instruction createUnreachable() {
        curBlock.createUnreachable();
        return getLastInstruction();
    }
}
