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

import at.pointhi.irbuilder.irwriter.IRWriter;
import at.pointhi.irbuilder.irwriter.IRWriterVersion;
import at.pointhi.irbuilder.irwriter.visitors.IRWriterBaseVisitor;

public class IRWriterInstructionVisitor extends IRWriterBaseVisitor implements InstructionVisitor {

    public IRWriterInstructionVisitor(IRWriterVersion.IRWriterVisitors visitors, IRWriter.PrintTarget target) {
        super(visitors, target);
    }

    public void visit(AllocateInstruction allocate) {
        // TODO Auto-generated method stub

    }

    public void visit(BinaryOperationInstruction operation) {
        // TODO Auto-generated method stub

    }

    public void visit(BranchInstruction branch) {
        // TODO Auto-generated method stub

    }

    public void visit(CallInstruction call) {
        // TODO Auto-generated method stub

    }

    public void visit(CastInstruction cast) {
        // TODO Auto-generated method stub

    }

    public void visit(CompareInstruction operation) {
        // TODO Auto-generated method stub

    }

    public void visit(ConditionalBranchInstruction branch) {
        // TODO Auto-generated method stub

    }

    public void visit(ExtractElementInstruction extract) {
        // TODO Auto-generated method stub

    }

    public void visit(ExtractValueInstruction extract) {
        // TODO Auto-generated method stub

    }

    public void visit(GetElementPointerInstruction gep) {
        // TODO Auto-generated method stub

    }

    public void visit(IndirectBranchInstruction branch) {
        // TODO Auto-generated method stub

    }

    public void visit(InsertElementInstruction insert) {
        // TODO Auto-generated method stub

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
