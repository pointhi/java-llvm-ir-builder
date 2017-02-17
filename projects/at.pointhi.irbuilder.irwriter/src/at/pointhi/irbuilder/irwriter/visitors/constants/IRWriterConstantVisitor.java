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

package at.pointhi.irbuilder.irwriter.visitors.constants;

import com.oracle.truffle.llvm.parser.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.symbols.constants.BinaryOperationConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.BlockAddressConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.CastConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.CompareConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.GetElementPointerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.InlineAsmConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.NullConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.StringConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.UndefinedConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.aggregate.ArrayConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.aggregate.StructureConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.aggregate.VectorConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.floatingpoint.DoubleConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.floatingpoint.FloatConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.floatingpoint.X86FP80Constant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.BigIntegerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.model.visitors.ConstantVisitor;

import at.pointhi.irbuilder.irwriter.IRWriter;
import at.pointhi.irbuilder.irwriter.IRWriterVersion;
import at.pointhi.irbuilder.irwriter.visitors.IRWriterBaseVisitor;

public class IRWriterConstantVisitor extends IRWriterBaseVisitor implements ConstantVisitor {

    public IRWriterConstantVisitor(IRWriterVersion.IRWriterVisitors visitors, IRWriter.PrintTarget target) {
        super(visitors, target);
    }

    public void visit(ArrayConstant arrayConstant) {
        // TODO Auto-generated method stub

    }

    public void visit(StructureConstant structureConstant) {
        // TODO Auto-generated method stub

    }

    public void visit(VectorConstant vectorConstant) {
        // TODO Auto-generated method stub

    }

    public void visit(BigIntegerConstant bigIntegerConstant) {
        // TODO Auto-generated method stub

    }

    public void visit(BinaryOperationConstant binaryOperationConstant) {
        // TODO Auto-generated method stub

    }

    public void visit(BlockAddressConstant blockAddressConstant) {
        // TODO Auto-generated method stub

    }

    public void visit(CastConstant castConstant) {
        // TODO Auto-generated method stub

    }

    public void visit(CompareConstant compareConstant) {
        // TODO Auto-generated method stub

    }

    public void visit(DoubleConstant doubleConstant) {
        // TODO Auto-generated method stub

    }

    public void visit(FloatConstant floatConstant) {
        // TODO Auto-generated method stub

    }

    public void visit(X86FP80Constant x86fp80Constant) {
        // TODO Auto-generated method stub

    }

    public void visit(FunctionDeclaration functionDeclaration) {
        // TODO Auto-generated method stub

    }

    public void visit(FunctionDefinition functionDefinition) {
        // TODO Auto-generated method stub

    }

    public void visit(GetElementPointerConstant getElementPointerConstant) {
        // TODO Auto-generated method stub

    }

    public void visit(InlineAsmConstant inlineAsmConstant) {
        // TODO Auto-generated method stub

    }

    public void visit(IntegerConstant integerConstant) {
        // TODO Auto-generated method stub

    }

    public void visit(NullConstant nullConstant) {
        // TODO Auto-generated method stub

    }

    public void visit(StringConstant stringConstant) {
        // TODO Auto-generated method stub

    }

    public void visit(UndefinedConstant undefinedConstant) {
        // TODO Auto-generated method stub

    }

}
