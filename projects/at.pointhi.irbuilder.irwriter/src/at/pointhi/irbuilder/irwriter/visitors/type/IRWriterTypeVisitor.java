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

package at.pointhi.irbuilder.irwriter.visitors.type;

import java.math.BigInteger;

import com.oracle.truffle.llvm.runtime.types.ArrayType;
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
import com.oracle.truffle.llvm.runtime.types.visitors.TypeVisitor;

import at.pointhi.irbuilder.irwriter.IRWriter;
import at.pointhi.irbuilder.irwriter.IRWriterVersion;
import at.pointhi.irbuilder.irwriter.visitors.IRWriterBaseVisitor;

public class IRWriterTypeVisitor extends IRWriterBaseVisitor implements TypeVisitor {

    public IRWriterTypeVisitor(IRWriterVersion.IRWriterVisitors visitors, IRWriter.PrintTarget target) {
        super(visitors, target);
    }

    @Override
    public void visit(PrimitiveType primitiveType) {
        if (Type.isIntegerType(primitiveType)) {
            writef("i%d", primitiveType.getBitSize());
        } else {
            write(primitiveType.getPrimitiveKind().name().toLowerCase()); // TODO: sulong specific
        }
    }

    @Override
    public void visit(VariableBitWidthType vectorType) {

        if (vectorType.getBitSize() == 1) {
            write(vectorType.getConstant().equals(BigInteger.ZERO) ? "i1 false" : "i1 true"); // TODO
            return;
        }

        writef("i%d", vectorType.getBitSize());
    }

    @Override
    public void visit(VoidType vectorType) {
        write("void");
    }

    @Override
    public void visit(FunctionType functionType) {
        writeType(functionType.getReturnType());
        write(" ");
        writeFormalArguments(functionType);
    }

    @Override
    public void visit(MetaType metaType) {
        if (MetaType.UNKNOWN.equals(metaType)) {
            write("unknown");
        } else if (MetaType.LABEL.equals(metaType)) {
            write("label");
        } else if (MetaType.TOKEN.equals(metaType)) {
            write("token");
        } else if (MetaType.METADATA.equals(metaType)) {
            write("metadata");
        } else if (MetaType.X86MMX.equals(metaType)) {
            write("x86mmx");
        } else if (MetaType.DEBUG.equals(metaType)) {
            write("metadata");
        } else {
            throw new IllegalStateException("unexpected MetaType: " + metaType);
        }
    }

    @Override
    public void visit(PointerType pointerType) {
        writeType(pointerType.getPointeeType());
        write("*");
    }

    @Override
    public void visit(ArrayType arrayType) {
        writef("[%d", arrayType.getNumberOfElements());
        write(" x ");
        writeType(arrayType.getElementType());
        write("]");
    }

    @Override
    public void visit(StructureType structureType) {
        if (LLVMIdentifier.UNKNOWN.equals(structureType.getName())) {
            writeStructDeclaration(structureType);
        } else {
            write(structureType.getName());
        }
    }

    @Override
    public void visit(VectorType vectorType) {
        writef("<%d", vectorType.getNumberOfElements());
        write(" x ");
        writeType(vectorType.getElementType());
        write(">");
    }

    public void visit(OpaqueType opaqueType) {
        if (LLVMIdentifier.UNKNOWN.equals(opaqueType.getName())) {
            write("opaque");
        } else {
            write(opaqueType.getName());
        }
    }
}
