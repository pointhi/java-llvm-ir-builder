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

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.llvm.parser.metadata.MDBaseNode;
import com.oracle.truffle.llvm.parser.metadata.MDNamedNode;
import com.oracle.truffle.llvm.parser.metadata.MetadataVisitor;
import com.oracle.truffle.llvm.parser.model.ModelModule;
import com.oracle.truffle.llvm.parser.model.attributes.AttributesGroup;
import com.oracle.truffle.llvm.parser.model.enums.Linkage;
import com.oracle.truffle.llvm.parser.model.enums.Visibility;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.model.symbols.globals.GlobalAlias;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

import at.pointhi.irbuilder.irwriter.IRWriter;
import at.pointhi.irbuilder.irwriter.IRWriterVersion;

public class IRWriterModelVisitorV38 extends IRWriterModelVisitor {

    public IRWriterModelVisitorV38(IRWriterVersion.IRWriterVisitors visitors, IRWriter.PrintTarget target) {
        super(visitors, target);
    }

    private final List<AttributesGroup> attributes = new ArrayList<>();
    private final List<MDBaseNode> metadata = new ArrayList<>();

    @Override
    public void writePrologue(ModelModule model) {

    }

    @Override
    public void writeEpilogue(ModelModule model) {
        if (!attributes.isEmpty()) {
            writeAttributes();
        }

        writeMetadata(model);
    }

    public int addAttribute(AttributesGroup a) {
        for (int i = 0; i < attributes.size(); i++) {
            final AttributesGroup paramAttr = attributes.get(i);
            if (paramAttr.equals(a)) {
                return i;
            }
        }
        attributes.add(a);
        return attributes.size() - 1;
    }

    private void writeAttributes() {
        writeln();

        for (int i = 0; i < attributes.size(); i++) {
            final AttributesGroup paramAttr = attributes.get(i);
            write("attributes #" + i + " = {");
            writeAttributesGroup(paramAttr);
            writeln(" }");
        }
    }

    public int addMetadata(MDBaseNode m) {
        for (int i = 0; i < metadata.size(); i++) {
            final MDBaseNode metadataAttr = metadata.get(i);
            if (metadataAttr.equals(m)) {
                return i;
            }
        }
        metadata.add(m);
        return metadata.size() - 1;
    }

    private void writeMetadata(ModelModule model) {
        writeln();

        // Write named nodes first
        model.getMetadata().accept(new MetadataVisitor() {
            @Override
            public void visit(MDNamedNode alias) {
                writef("!%s = ", alias.getName());
                writeMetadataValue(alias);
            }
        });

        writeln();

        // write nodes by id
        for (int i = 0; i < metadata.size(); i++) {
            final MDBaseNode metadataAttr = metadata.get(i);
            writef("!%d = ", i);
            metadataAttr.accept(visitors.getMetadataVisitor());
            writeln();
        }
    }

    private static final String UNRESOLVED_FORWARD_REFERENCE = "<unresolved>";

    /*
     * @see http://releases.llvm.org/3.8.0/docs/LangRef.html#aliases
     */
    @Override
    public void visit(GlobalAlias alias) {
        write(alias.getName());
        write(" = ");

        if (alias.getLinkage() != Linkage.EXTERNAL || alias.getValue() == null) {
            write(alias.getLinkage().getIrString());
            write(" ");
        }

        if (alias.getVisibility() != Visibility.DEFAULT) {
            write(alias.getVisibility().getIrString());
            write(" ");
        }

        write(LLVMIR_LABEL_ALIAS);
        write(" ");

        final Symbol val = alias.getValue();
        if (val == null) {
            writeln(UNRESOLVED_FORWARD_REFERENCE);
            return;
        }

        writeType(((PointerType) alias.getType()).getPointeeType());

        write(", ");
        writeSymbolType(val);

        write(" ");
        writeInnerSymbolValue(val);
        writeln();
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

        write("(");

        final Type[] argTypes = function.getType().getArgumentTypes();
        for (int i = 0; i < argTypes.length; i++) {
            if (i != 0) {
                write(", ");
            }

            writeType(argTypes[i]);
            writeAttributesGroupIfPresent(function.getParameterAttributesGroup(i));
        }

        if (function.getType().isVarargs()) {
            if (argTypes.length != 0) {
                write(", ");
            }

            write("...");
        }

        write(")");

        if (paramAttr != null) {
            write(" #" + addAttribute(paramAttr));
        }

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
        Linkage linkage = function.getLinkage();
        if (linkage != Linkage.EXTERNAL) {
            write(" ");
            write(linkage.getIrString());
        }
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

        if (paramAttr != null) {
            write(" #" + addAttribute(paramAttr));
        }

        writeln(" {");
        writeFunction(function);
        writeln("}");
    }

}
