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

package at.pointhi.irbuilder.irwriter.visitors.model;

import com.oracle.truffle.llvm.parser.model.enums.CastOperator;
import com.oracle.truffle.llvm.parser.model.enums.Linkage;
import com.oracle.truffle.llvm.parser.model.enums.Visibility;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.model.globals.GlobalAlias;
import com.oracle.truffle.llvm.parser.model.globals.GlobalConstant;
import com.oracle.truffle.llvm.parser.model.globals.GlobalValueSymbol;
import com.oracle.truffle.llvm.parser.model.globals.GlobalVariable;
import com.oracle.truffle.llvm.parser.model.symbols.constants.CastConstant;
import com.oracle.truffle.llvm.parser.model.target.TargetDataLayout;
import com.oracle.truffle.llvm.parser.model.visitors.ModelVisitor;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.LLVMIdentifier;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

import at.pointhi.irbuilder.irwriter.IRWriter;
import at.pointhi.irbuilder.irwriter.IRWriterVersion;
import at.pointhi.irbuilder.irwriter.visitors.IRWriterBaseVisitor;

public class IRWriterModelVisitor extends IRWriterBaseVisitor implements ModelVisitor {

    public IRWriterModelVisitor(IRWriterVersion.IRWriterVisitors visitors, IRWriter.PrintTarget target) {
        super(visitors, target);
    }

    private void writeGlobal(String keyword, GlobalValueSymbol global) {
        write(global.getName());
        write(" = ");

        if (global.getLinkage() != Linkage.EXTERNAL || global.getValue() == null) {
            write(global.getLinkage().toString()); // sulong specific toString
            write(" ");
        }

        if (global.getVisibility() != Visibility.DEFAULT) {
            write(global.getVisibility().getIrString());
            write(" ");
        }

        write(keyword);
        write(" ");

        final Symbol value = global.getValue();
        if (value instanceof FunctionType) {
            writeType(new PointerType((FunctionType) value));
            write(" ");

        } else if (!(value instanceof CastConstant && ((CastConstant) value).getOperator() == CastOperator.BITCAST)) {
            writeType(((PointerType) global.getType()).getPointeeType());
            write(" ");
        }

        if (value != null) {
            writeInnerSymbolValue(value);
        }

        if (global.getAlign() > 1) {
            write(", align ");
            write(String.valueOf(1 << (global.getAlign() - 1)));
        }

        writeln();
    }

    private static final String LLVMIR_LABEL_ALIAS = "alias";

    @Override
    public void visit(GlobalAlias alias) {
        writeGlobal(LLVMIR_LABEL_ALIAS, alias);
    }

    private static final String LLVMIR_LABEL_CONSTANT = "constant";

    @Override
    public void visit(GlobalConstant constant) {
        writeGlobal(LLVMIR_LABEL_CONSTANT, constant);
    }

    private static final String LLVMIR_LABEL_GLOBAL = "global";

    @Override
    public void visit(GlobalVariable variable) {
        writeGlobal(LLVMIR_LABEL_GLOBAL, variable);
    }

    @Override
    public void visit(FunctionDeclaration function) {
        writeln();

        write("declare ");
        writeType(function.getReturnType());

        writef(" %s", function.getName());

        writeFormalArguments(function);
        writeln();
    }

    @Override
    public void visit(FunctionDefinition function) {
        writeln();

        write("define ");
        writeType(function.getReturnType());

        writef(" %s", function.getName());

        write("(");

        boolean firstIteration = true;
        for (FunctionParameter param : function.getParameters()) {
            if (!firstIteration) {
                write(", ");
            } else {
                firstIteration = false;
            }
            writeType(param.getType());
            write(" ");
            write(param.getName());
        }

        if (function.isVarArg()) {
            if (!firstIteration) {
                write(", ");
            }

            write("...");
        }

        write(")");

        writeln(" {");
        writeFunction(function);
        writeln("}");
    }

    @Override
    public void visit(TargetDataLayout layout) {
        writeln(String.format("target datalayout = \"%s\"", layout.getDataLayout()));
        writeln();
    }

    @Override
    public void visit(Type type) {
        if (type instanceof StructureType && !((StructureType) type).getName().equals(LLVMIdentifier.UNKNOWN)) {
            final StructureType actualType = (StructureType) type;
            writef("%%%s = type ", actualType.getName());
            writeStructDeclaration(actualType);
            writeln();
        }
    }
}
