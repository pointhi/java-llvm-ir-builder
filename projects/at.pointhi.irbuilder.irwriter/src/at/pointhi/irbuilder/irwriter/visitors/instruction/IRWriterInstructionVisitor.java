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

package at.pointhi.irbuilder.irwriter.visitors.instruction;

import com.oracle.truffle.llvm.parser.model.enums.Flag;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.AllocateInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.BinaryOperationInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.BranchInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.CallInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.CastInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.CompareInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ConditionalBranchInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ExtractElementInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ExtractValueInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.GetElementPointerInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.IndirectBranchInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.InsertElementInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.InsertValueInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.LoadInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.PhiInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ReturnInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.SelectInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ShuffleVectorInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.StoreInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.SwitchInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.SwitchOldInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.UnreachableInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.VoidCallInstruction;
import com.oracle.truffle.llvm.parser.model.visitors.InstructionVisitor;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

import at.pointhi.irbuilder.irwriter.IRWriter;
import at.pointhi.irbuilder.irwriter.IRWriterVersion;
import at.pointhi.irbuilder.irwriter.visitors.IRWriterBaseVisitor;

public class IRWriterInstructionVisitor extends IRWriterBaseVisitor implements InstructionVisitor {

    public IRWriterInstructionVisitor(IRWriterVersion.IRWriterVisitors visitors, IRWriter.PrintTarget target) {
        super(visitors, target);
    }

    protected static final String LLVMIR_LABEL_ALIGN = "align";

    protected static final String INDENTATION = "  ";

    protected void writeIndent() {
        write(INDENTATION);
    }

    private static final String LLVMIR_LABEL_ALLOCATE = "alloca";

    public void visit(AllocateInstruction allocate) {
        writeIndent();

        // <result> = alloca <type>
        writef("%s = %s ", allocate.getName(), LLVMIR_LABEL_ALLOCATE);
        writeType(allocate.getPointeeType());

        // [, <ty> <NumElements>]
        if (!(allocate.getCount() instanceof IntegerConstant && ((IntegerConstant) allocate.getCount()).getValue() == 1)) {
            write(", ");
            writeType(allocate.getCount().getType());
            write(" ");
            writeInnerSymbolValue(allocate.getCount());
        }

        // [, align <alignment>]
        if (allocate.getAlign() != 0) {
            writef(", %s %d", LLVMIR_LABEL_ALIGN, 1 << (allocate.getAlign() - 1));
        }

        writeln();
    }

    public void visit(BinaryOperationInstruction operation) {
        writeIndent();

        // <result> = <op>
        // sulong specific toString
        writef("%s = %s ", operation.getName(), operation.getOperator());

        // { <flag>}*
        for (Flag flag : operation.getFlags()) {
            write(flag.toString()); // sulong specific toString
            write(" ");
        }

        // <ty> <op1>, <op2>
        writeType(operation.getType());
        write(" ");
        writeInnerSymbolValue(operation.getLHS());
        write(", ");
        writeInnerSymbolValue(operation.getRHS());

        writeln();
    }

    private static final String LLVMIR_LABEL_BRANCH = "br";

    private static final String LLVMIR_LABEL_BRANCH_LABEL = "label";

    public void visit(BranchInstruction branch) {
        writeIndent();

        writef("%s %s ", LLVMIR_LABEL_BRANCH, LLVMIR_LABEL_BRANCH_LABEL);
        writeBlockName(branch.getSuccessor());

        writeln();
    }

    static final String LLVMIR_LABEL_CALL = "call";

    public void visit(CallInstruction call) {
        writeIndent();

        // <result> = [tail] call
        writef("%s = ", call.getName());

        // printFunctionCall(call); // TODO

        writeln();
    }

    public void visit(CastInstruction cast) {
        writeIndent();

        // sulong specific toString
        writef("%s = %s ", cast.getName(), cast.getOperator());
        writeType(cast.getValue().getType());
        write(" ");
        writeInnerSymbolValue(cast.getValue());
        write(" to ");
        writeType(cast.getType());

        writeln();
    }

    private static final String LLVMIR_LABEL_COMPARE = "icmp";
    private static final String LLVMIR_LABEL_COMPARE_FP = "fcmp";

    public void visit(CompareInstruction operation) {
        writeIndent();

        // <result> = <icmp|fcmp> <cond> <ty> <op1>, <op2>
        write(operation.getName());
        write(" = ");

        if (operation.getOperator().isFloatingPoint()) {
            write(LLVMIR_LABEL_COMPARE_FP);
        } else {
            write(LLVMIR_LABEL_COMPARE);
        }

        write(" ");
        write(operation.getOperator().toString()); // sulong specific toString
        write(" ");
        writeType(operation.getLHS().getType());
        write(" ");
        writeInnerSymbolValue(operation.getLHS());
        write(", ");
        writeInnerSymbolValue(operation.getRHS());

        writeln();
    }

    private static final String LLVMIR_LABEL_CONDITIONAL_BRANCH = "br";

    public void visit(ConditionalBranchInstruction branch) {
        writeIndent();

        // br i1 <cond>, label <iftrue>, label <iffalse>
        write(LLVMIR_LABEL_CONDITIONAL_BRANCH);
        write(" ");
        writeType(branch.getCondition().getType());
        write(" ");
        writeInnerSymbolValue(branch.getCondition());
        write(", ");
        write(LLVMIR_LABEL_BRANCH_LABEL);
        write(" ");
        writeBlockName(branch.getTrueSuccessor());
        write(", ");
        write(LLVMIR_LABEL_BRANCH_LABEL);
        write(" ");
        writeBlockName(branch.getFalseSuccessor());

        writeln();
    }

    private static final String LLVMIR_LABEL_EXTRACT_ELEMENT = "extractelement";

    public void visit(ExtractElementInstruction extract) {
        writeIndent();

        // <result> = extractelement <n x <ty>> <val>, i32 <idx>
        write(extract.getName());
        write(" = ");
        write(LLVMIR_LABEL_EXTRACT_ELEMENT);
        write(" ");
        writeType(extract.getVector().getType());
        write(" ");
        writeInnerSymbolValue(extract.getVector());
        write(", ");
        writeType(extract.getIndex().getType());
        write(" ");
        writeInnerSymbolValue(extract.getIndex());

        writeln();
    }

    private static final String LLVMIR_LABEL_EXTRACT_VALUE = "extractvalue";

    public void visit(ExtractValueInstruction extract) {
        writeIndent();

        // <result> = extractvalue <aggregate type> <val>, <idx>{, <idx>}*
        write(extract.getName());
        write(" = ");
        write(LLVMIR_LABEL_EXTRACT_VALUE);
        write(" ");
        writeType(extract.getAggregate().getType());
        write(" ");
        writeInnerSymbolValue(extract.getAggregate());
        writef(", %d", extract.getIndex());

        writeln();
    }

    private static final String LLVMIR_LABEL_GET_ELEMENT_POINTER = "getelementptr";

    private static final String LLVMIR_LABEL_GET_ELEMENT_POINTER_INBOUNDS = "inbounds";

    public void visit(GetElementPointerInstruction gep) {
        writeIndent();

        // <result> = getelementptr
        writef("%s = %s ", gep.getName(), LLVMIR_LABEL_GET_ELEMENT_POINTER);

        // [inbounds]
        if (gep.isInbounds()) {
            write(LLVMIR_LABEL_GET_ELEMENT_POINTER_INBOUNDS);
            write(" ");
        }

        // <pty>* <ptrval>
        writeType(gep.getBasePointer().getType());
        write(" ");
        writeInnerSymbolValue(gep.getBasePointer());

        // {, <ty> <idx>}*
        for (final Symbol sym : gep.getIndices()) {
            write(", ");
            writeType(sym.getType());
            write(" ");
            writeInnerSymbolValue(sym);
        }

        writeln();
    }

    private static final String LLVMIR_LABEL_INDIRECT_BRANCH = "indirectbr";

    public void visit(IndirectBranchInstruction branch) {
        writeIndent();

        // indirectbr <somety>* <address>, [ label <dest1>, label <dest2>, ... ]
        write(LLVMIR_LABEL_INDIRECT_BRANCH);
        write(" ");
        writeType(branch.getAddress().getType());
        write(" ");
        writeInnerSymbolValue(branch.getAddress());
        write(", [ ");
        for (int i = 0; i < branch.getSuccessorCount(); i++) {
            if (i != 0) {
                write(", ");
            }
            write(LLVMIR_LABEL_BRANCH_LABEL);
            write(" ");
            writeBlockName(branch.getSuccessor(i));
        }
        write(" ]");

        writeln();
    }

    private static final String LLVMIR_LABEL_INSERT_ELEMENT = "insertelement";

    public void visit(InsertElementInstruction insert) {
        writeIndent();

        // <result> = insertelement <n x <ty>> <val>, <ty> <elt>, i32 <idx>
        write(insert.getName());
        write(" = ");
        write(LLVMIR_LABEL_INSERT_ELEMENT);
        write(" ");
        writeType(insert.getVector().getType());
        write(" ");
        writeInnerSymbolValue(insert.getVector());
        write(", ");
        writeType(insert.getValue().getType());
        write(" ");
        writeInnerSymbolValue(insert.getValue());
        write(", ");
        writeType(insert.getIndex().getType());
        write(" ");
        writeInnerSymbolValue(insert.getIndex());

        writeln();
    }

    public void visit(InsertValueInstruction insert) {
        // TODO Auto-generated method stub

    }

    public void visit(LoadInstruction load) {
        // TODO Auto-generated method stub

    }

    public void visit(PhiInstruction phi) {
        // TODO Auto-generated method stub

    }

    public void visit(ReturnInstruction ret) {
        // TODO Auto-generated method stub

    }

    public void visit(SelectInstruction select) {
        // TODO Auto-generated method stub

    }

    public void visit(ShuffleVectorInstruction shuffle) {
        // TODO Auto-generated method stub

    }

    public void visit(StoreInstruction store) {
        // TODO Auto-generated method stub

    }

    public void visit(SwitchInstruction select) {
        // TODO Auto-generated method stub

    }

    public void visit(SwitchOldInstruction select) {
        // TODO Auto-generated method stub

    }

    public void visit(UnreachableInstruction unreachable) {
        // TODO Auto-generated method stub

    }

    public void visit(VoidCallInstruction call) {
        // TODO Auto-generated method stub

    }

}
