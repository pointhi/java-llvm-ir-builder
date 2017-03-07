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
import com.oracle.truffle.llvm.runtime.types.BigIntegerConstantType;
import com.oracle.truffle.llvm.runtime.types.FloatingPointType;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.IntegerConstantType;
import com.oracle.truffle.llvm.runtime.types.IntegerType;
import com.oracle.truffle.llvm.runtime.types.MetaType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.VectorType;
import com.oracle.truffle.llvm.runtime.types.metadata.MetadataConstantPointerType;
import com.oracle.truffle.llvm.runtime.types.metadata.MetadataConstantType;
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
    public void visit(BigIntegerConstantType bigIntegerConstantType) {
        if (bigIntegerConstantType.getType().getBits() == 1) {
            write(bigIntegerConstantType.getValue().equals(BigInteger.ZERO) ? "i1 false" : "i1 true");
            return;
        }

        writeType(bigIntegerConstantType.getType());
        writef(" %s", bigIntegerConstantType.getValue());
    }

    @Override
    public void visit(FloatingPointType floatingPointType) {
        write(floatingPointType.name().toLowerCase());
    }

    @Override
    public void visit(FunctionType functionType) {
        writeType(functionType.getReturnType());

        write(" (");

        for (int i = 0; i < functionType.getArgumentTypes().length; i++) {
            if (i > 0) {
                write(", ");
            }
            writeType(functionType.getArgumentTypes()[i]);
        }

        if (functionType.isVarArg()) {
            if (functionType.getArgumentTypes().length > 0) {
                write(", ");
            }
            write("...");
        }
        write(")");
    }

    @Override
    public void visit(IntegerConstantType integerConstantType) {
        if (integerConstantType.getType().getBits() == 1) {
            write(integerConstantType.getValue() == 0 ? "i1 false" : "i1 true");
            return;
        }

        writeType(integerConstantType.getType());
        writef(" %d", integerConstantType.getValue());
    }

    @Override
    public void visit(IntegerType integerType) {
        writef("i%d", integerType.getBits());
    }

    @Override
    public void visit(MetadataConstantType metadataConstantType) {
        writeType(metadataConstantType.getType());
        writef(" %d", metadataConstantType.getValue());
    }

    @Override
    public void visit(MetadataConstantPointerType metadataConstantPointerType) {
        writef("!!%d", metadataConstantPointerType.getSymbolIndex());
    }

    @Override
    public void visit(MetaType metaType) {
        write(metaType.name().toLowerCase());
    }

    @Override
    public void visit(PointerType pointerType) {
        writeType(pointerType.getPointeeType());
        write("*");
    }

    @Override
    public void visit(ArrayType arrayType) {
        writef("[%d", arrayType.getLength());
        write(" x ");
        writeType(arrayType.getElementType());
        write("]");
    }

    @Override
    public void visit(StructureType structureType) {
        if (LLVMIdentifier.UNKNOWN.equals(structureType.getName())) {
            writeStructDeclaration(structureType);
        } else {
            write("%" + structureType.getName());
        }
    }

    @Override
    public void visit(VectorType vectorType) {
        writef("<%d", vectorType.getLength());
        write(" x ");
        writeType(vectorType.getElementType());
        write(">");
    }
}
