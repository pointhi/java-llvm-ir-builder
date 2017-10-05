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

import com.oracle.truffle.llvm.parser.model.attributes.AttributesGroup;
import com.oracle.truffle.llvm.parser.model.enums.Linkage;
import com.oracle.truffle.llvm.parser.model.enums.Visibility;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.model.symbols.globals.GlobalAlias;
import com.oracle.truffle.llvm.parser.model.symbols.globals.GlobalConstant;
import com.oracle.truffle.llvm.parser.model.symbols.globals.GlobalValueSymbol;
import com.oracle.truffle.llvm.parser.model.symbols.globals.GlobalVariable;
import com.oracle.truffle.llvm.parser.model.target.TargetDataLayout;
import com.oracle.truffle.llvm.parser.model.visitors.ModelVisitor;
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
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;
import com.oracle.truffle.llvm.runtime.types.visitors.TypeVisitor;

import at.pointhi.irbuilder.irwriter.IRWriter;
import at.pointhi.irbuilder.irwriter.IRWriterVersion;
import at.pointhi.irbuilder.irwriter.visitors.IRWriterBaseVisitor;

public class IRWriterModelVisitor extends IRWriterBaseVisitor implements ModelVisitor {

    public IRWriterModelVisitor(IRWriterVersion.IRWriterVisitors visitors, IRWriter.PrintTarget target) {
        super(visitors, target);
    }

    public void writePrologue() {
    }

    public void writeEpilogue() {
    }

    private void writeGlobal(String keyword, GlobalValueSymbol global) {
        write(global.getName());
        write(" = ");

        if (global.getLinkage() != Linkage.EXTERNAL || global.getValue() == null) {
            write(global.getLinkage().getIrString());
            write(" ");
        }

        if (global.getVisibility() != Visibility.DEFAULT) {
            write(global.getVisibility().getIrString());
            write(" ");
        }

        write(keyword);
        write(" ");

        final Symbol value = global.getValue();

        if (value != null) {
            writeSymbolType(value);
            write(" ");
            writeInnerSymbolValue(value);
        } else {
            writeType(((PointerType) global.getType()).getPointeeType());
        }

        if (global.getAlign() > 1) {
            write(", align ");
            write(String.valueOf(1 << (global.getAlign() - 1)));
        }

        writeln();
    }

    protected static final String LLVMIR_LABEL_ALIAS = "alias";

    /*
     * @see http://releases.llvm.org/3.2/docs/LangRef.html#aliasstructure
     */
    @Override
    public void visit(GlobalAlias alias) {
        write(alias.getName());
        write(" = ");

        write(LLVMIR_LABEL_ALIAS);
        write(" ");

        if (alias.getLinkage() != Linkage.EXTERNAL || alias.getValue() == null) {
            write(alias.getLinkage().getIrString());
            write(" ");
        }

        if (alias.getVisibility() != Visibility.DEFAULT) {
            write(alias.getVisibility().getIrString());
            write(" ");
        }

        final Symbol value = alias.getValue();

        if (value != null) {
            writeSymbolType(value);
            write(" ");
            writeInnerSymbolValue(value);
        } else {
            writeType(((PointerType) alias.getType()).getPointeeType());
        }

        writeln();
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

        AttributesGroup paramAttr = function.getFunctionAttributesGroup();
        if (paramAttr != null) {
            write("; Function Attrs:");
            writeKnownAttributesGroup(paramAttr);
            writeln();
        }

        write("declare");
        writeAttributesGroupIfPresent(function.getReturnAttributesGroup());
        write(" ");
        writeType(function.getType().getReturnType());

        writef(" %s", function.getName());

        writeFormalArguments(function.getType());

        writeln();
    }

    @Override
    public void visit(FunctionDefinition function) {
        writeln();

        AttributesGroup paramAttr = function.getFunctionAttributesGroup();
        if (paramAttr != null) {
            write("; Function Attrs:");
            writeKnownAttributesGroup(paramAttr);
            writeln();
        }

        write("define");
        writeAttributesGroupIfPresent(function.getReturnAttributesGroup());
        write(" ");
        writeType(function.getType().getReturnType());

        writef(" %s", function.getName());

        write("(");

        boolean firstIteration = true;
        for (FunctionParameter param : function.getParameters()) {
            if (!firstIteration) {
                write(", ");
            } else {
                firstIteration = false;
            }
            writeFunctionParameter(param);
        }

        if (function.getType().isVarargs()) {
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

    protected void writeFunctionParameter(FunctionParameter param) {
        writeType(param.getType());
        writeAttributesGroupIfPresent(param.getParameterAttribute());
        if (!param.getName().matches("%\\d+")) {
            write(" ");
            write(param.getName());
        }
    }

    @Override
    public void visit(TargetDataLayout layout) {
        writeln(String.format("target datalayout = \"%s\"", layout.getDataLayout()));
        writeln();
    }

    @Override
    public void visit(Type type) {
        type.accept(new TypeVisitor() {

            public void visit(OpaqueType opaqueType) {
                if (!opaqueType.getName().equals(LLVMIdentifier.UNKNOWN)) {
                    writef("%s = type opaque", opaqueType.getName());
                    writeln();
                }
            }

            public void visit(StructureType structureType) {
                if (!structureType.getName().equals(LLVMIdentifier.UNKNOWN)) {
                    writef("%s = type ", structureType.getName());
                    writeStructDeclaration(structureType);
                    writeln();
                }
            }

            public void visit(VoidType vectorType) {
            }

            public void visit(VariableBitWidthType vectorType) {
            }

            public void visit(VectorType vectorType) {
            }

            public void visit(ArrayType arrayType) {
            }

            public void visit(PointerType pointerType) {
            }

            public void visit(MetaType metaType) {
            }

            public void visit(PrimitiveType primitiveType) {
            }

            public void visit(FunctionType functionType) {
            }
        });
    }
}
