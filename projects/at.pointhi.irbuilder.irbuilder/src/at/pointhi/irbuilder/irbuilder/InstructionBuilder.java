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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import com.oracle.truffle.llvm.parser.model.symbols.instructions.LoadInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ValueInstruction;
import com.oracle.truffle.llvm.runtime.types.AggregateType;
import com.oracle.truffle.llvm.runtime.types.ArrayType;
import com.oracle.truffle.llvm.runtime.types.DataSpecConverter;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.MetaType;
import com.oracle.truffle.llvm.runtime.types.OpaqueType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VariableBitWidthType;
import com.oracle.truffle.llvm.runtime.types.VectorType;
import com.oracle.truffle.llvm.runtime.types.VoidType;
import com.oracle.truffle.llvm.runtime.types.symbols.LLVMIdentifier;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;
import com.oracle.truffle.llvm.runtime.types.visitors.TypeVisitor;

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

    public InstructionBlock nextBlock() {
        final int nextBlockIdx = curBlock.getBlockIndex() + 1;

        ensureBlockExists(nextBlockIdx);

        curBlock = function.generateBlock();
        curBlock.setName("label_" + Integer.toString(nextBlockIdx));
        return curBlock;
    }

    public InstructionBlock getCurrentBlock() {
        return curBlock;
    }

    public InstructionBlock getNextBlock() {
        return getBlock(curBlock.getBlockIndex() + 1);
    }

    public InstructionBlock getBlock(int idx) {
        ensureBlockExists(idx);

        return function.getBlock(idx);
    }

    private void ensureBlockExists(int idx) {
        if (idx < function.getBlockCount()) {
            return; // block already exists, nothing to do
        }

        /*
         * it seems we need to manually allocate new blocks
         *
         * Because the required field is private, we rely on reflection for now.
         */
        try {
            // get private blocks field and make it public
            final Field dataField = function.getClass().getDeclaredField("blocks");
            dataField.setAccessible(true);

            // get InstructionBlock[] and reallocate to new size
            final InstructionBlock[] oldBlocks = (InstructionBlock[]) dataField.get(function);
            final InstructionBlock[] newBlocks = Arrays.copyOf(oldBlocks, idx + 1);

            // we need to initialize our new InstructionBlock elements
            for (int i = oldBlocks.length; i < newBlocks.length; i++) {
                newBlocks[i] = new InstructionBlock(function, i);
            }

            // write new InstructionBlock[] back into the object
            dataField.set(function, newBlocks);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Append a specific amount of blocks after the current one, and change all InstructionBlock
     * indexes accordingly.
     *
     * @param count number of Blocks inserted
     */
    public void insertBlocks(int count) {
        try {
            // get private blocks field and make it public
            final Field dataField = function.getClass().getDeclaredField("blocks");
            dataField.setAccessible(true);

            // get InstructionBlock[] and reallocate to new size
            final InstructionBlock[] oldBlocks = (InstructionBlock[]) dataField.get(function);
            final InstructionBlock[] newBlocks = Arrays.copyOf(oldBlocks, oldBlocks.length + count);

            final int insertIdx = curBlock.getBlockIndex() + 1;
            final int rearIdx = insertIdx + count;

            // copy the rear part of the array to the new position
            System.arraycopy(newBlocks, insertIdx, newBlocks, rearIdx, oldBlocks.length - insertIdx);

            // we need to initialize our new InstructionBlock elements
            for (int i = insertIdx; i < rearIdx; i++) {
                newBlocks[i] = new InstructionBlock(function, i);
            }

            // get private blockIndex field and make it public
            final Field blockIndexField = InstructionBlock.class.getDeclaredField("blockIndex");
            blockIndexField.setAccessible(true);

            // we need to update the index of the remaining instructions
            for (int i = rearIdx; i < newBlocks.length; i++) {
                blockIndexField.set(newBlocks[i], i);
            }

            // write new InstructionBlock[] back into the object
            dataField.set(function, newBlocks);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void exitFunction() {
        function.exitFunction();
    }

    public FunctionDefinition getFunctionDefinition() {
        return function;
    }

    public int getArgCounter() {
        return argCounter;
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

    public Instruction createBranch(InstructionBlock block) {
        curBlock.createBranch(block.getBlockIndex());
        return getLastInstruction();
    }

    public Instruction createBranch(Symbol condition, InstructionBlock ifBlock, InstructionBlock elseBlock) {
        int conditionIdx = addSymbol(condition);

        curBlock.createBranch(conditionIdx, ifBlock.getBlockIndex(), elseBlock.getBlockIndex());
        return getLastInstruction();
    }

    public Instruction createCall(Symbol target, Symbol[] arguments) {
        Type returnType;
        if (target.getType() instanceof FunctionType) {
            returnType = ((FunctionType) target.getType()).getReturnType();
        } else if (target instanceof LoadInstruction) {
            Type pointeeType = ((LoadInstruction) target).getSource().getType();
            while (pointeeType instanceof PointerType) {
                pointeeType = ((PointerType) pointeeType).getPointeeType();
            }
            if (pointeeType instanceof FunctionType) {
                returnType = ((FunctionType) pointeeType).getReturnType();
            } else {
                throw new RuntimeException("cannot handle target type: " + pointeeType.getClass().getName());
            }
        } else {
            throw new RuntimeException("cannot handle target type: " + target.getClass().getName());
        }
        int targetIdx = addSymbol(target);
        int[] argumentsIdx = new int[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            argumentsIdx[i] = addSymbol(arguments[i]);
        }
        curBlock.createCall(returnType, targetIdx, argumentsIdx);
        return getLastInstruction();
    }

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

    public Instruction createExtractValue(@SuppressWarnings("unused") Instruction struct, Symbol vector, int index) {
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

    public Instruction createGetElementPointer(Symbol base, Symbol[] indices, boolean isInbounds) {
        int pointerIdx = addSymbol(base);
        List<Integer> indicesIdx = new ArrayList<>(indices.length);
        Type instrType = base.getType();
        for (int i = 0; i < indices.length; i++) {
            indicesIdx.add(addSymbol(indices[i]));

            GetElementPointerTypeVisitor localTypeVisitor = new GetElementPointerTypeVisitor(instrType, indices[i]);
            instrType.accept(localTypeVisitor);
            instrType = localTypeVisitor.getNewType();
        }
        curBlock.createGetElementPointer(new PointerType(instrType), pointerIdx, indicesIdx, isInbounds);
        return getLastInstruction();
    }

    private static final class GetElementPointerTypeVisitor implements TypeVisitor {

        private final Symbol idx;
        private Type newType;

        GetElementPointerTypeVisitor(Type curType, Symbol idx) {
            this.newType = curType;
            this.idx = idx;
        }

        public Type getNewType() {
            return newType;
        }

        public void visit(OpaqueType opaqueType) {
        }

        public void visit(VoidType vectorType) {
        }

        public void visit(VariableBitWidthType vectorType) {
        }

        public void visit(VectorType vectorType) {
            newType = vectorType.getElementType();
        }

        public void visit(StructureType structureType) {
            IntegerConstant idxConst = (IntegerConstant) idx;
            newType = structureType.getElementType((int) idxConst.getValue());
        }

        public void visit(ArrayType arrayType) {
            newType = arrayType.getElementType();
        }

        public void visit(PointerType pointerType) {
            newType = pointerType.getPointeeType();
        }

        public void visit(MetaType metaType) {
        }

        public void visit(PrimitiveType primitiveType) {
        }

        public void visit(FunctionType functionType) {
        }
    }

    public Instruction createIndirectBranch(Symbol address, InstructionBlock[] successors) {
        int[] successorsIdx = Arrays.stream(successors).mapToInt(f -> f.getBlockIndex()).toArray();

        int addressIdx = addSymbol(address);
        curBlock.createIndirectBranch(addressIdx, successorsIdx);
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

    public Instruction createPhi(Type type, Symbol[] values, InstructionBlock[] blocks) {
        assert values.length == blocks.length;

        int[] valuesIdx = new int[values.length];
        int[] blocksIdx = new int[blocks.length];
        for (int i = 0; i < blocks.length; i++) {
            valuesIdx[i] = addSymbol(values[i]);
            blocksIdx[i] = blocks[i].getBlockIndex();
        }
        curBlock.createPhi(type, valuesIdx, blocksIdx);
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
