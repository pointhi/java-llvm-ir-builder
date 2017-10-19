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

package at.pointhi.irbuilder.irwriter.visitors;

import com.oracle.truffle.llvm.parser.metadata.MDBaseNode;
import com.oracle.truffle.llvm.parser.metadata.MDNamedNode;
import com.oracle.truffle.llvm.parser.metadata.MDNode;
import com.oracle.truffle.llvm.parser.model.attributes.Attribute;
import com.oracle.truffle.llvm.parser.model.attributes.AttributesGroup;
import com.oracle.truffle.llvm.parser.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.LLVMIdentifier;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;
import com.oracle.truffle.llvm.runtime.types.symbols.ValueSymbol;

import at.pointhi.irbuilder.irwriter.IRWriter;
import at.pointhi.irbuilder.irwriter.IRWriterVersion;
import at.pointhi.irbuilder.irwriter.visitors.model.IRWriterModelVisitorV38;

public class IRWriterBaseVisitor {

    protected final IRWriterVersion.IRWriterVisitors visitors;

    private final IRWriter.PrintTarget out;

    public IRWriterBaseVisitor(IRWriterVersion.IRWriterVisitors visitors, IRWriter.PrintTarget target) {
        this.visitors = visitors;
        this.out = target;
    }

    /**
     * Append string to output stream.
     *
     * @param s String which we want to append
     */
    protected void write(String s) {
        out.print(s);
    }

    /**
     * Append string to output stream, followed by a newline.
     *
     * @param s String which we want to append
     */
    protected void writeln(String s) {
        out.println(s);
    }

    /**
     * Append newline to output stream.
     */
    protected void writeln() {
        out.println();
    }

    /**
     * Append a formated string to the output stream.
     *
     * @param format A format string
     * @param args Arguments referenced by the format specifiers in the format string
     */
    protected void writef(String format, Object... args) {
        out.print(String.format(format, args)); // TOOD: deprecate this function
    }

    protected void writeSymbolType(Symbol sym) {
        Type type = sym.getType();

        // TODO: workaround or expected behavior?
        if (sym instanceof FunctionDeclaration || sym instanceof FunctionDefinition) {
            type = new PointerType(sym.getType());
        }

        writeType(type);
    }

    protected void writeType(Type type) {
        type.accept(visitors.getTypeVisitor());
    }

    protected void writeConstant(Constant constant) {
        constant.accept(visitors.getConstantVisitor());
    }

    protected void writeFunction(FunctionDefinition function) {
        function.accept(visitors.getFunctionVisitor());
    }

    protected void writeFormalArguments(FunctionType function) {
        write("(");

        final Type[] argTypes = function.getArgumentTypes();
        for (int i = 0; i < argTypes.length; i++) {
            if (i != 0) {
                write(", ");
            }

            writeType(argTypes[i]);
        }

        if (function.isVarargs()) {
            if (argTypes.length != 0) {
                write(", ");
            }

            write("...");
        }

        write(")");
    }

    protected void writeStructDeclaration(StructureType structureType) {
        if (structureType.isPacked()) {
            write("<");
        }
        write("{ ");

        for (int i = 0; i < structureType.getNumberOfElements(); i++) {
            if (i > 0) {
                write(", ");
            }

            writeType(structureType.getElementType(i));
        }

        write(" }");
        if (structureType.isPacked()) {
            write(">");
        }
    }

    protected void writeInstructionBlock(InstructionBlock block) {
        block.accept(visitors.getInstructionVisitor());
    }

    protected void writeInnerSymbolValue(Symbol symbol) {
        if (symbol instanceof ValueSymbol) {
            final String value = ((ValueSymbol) symbol).getName();
            write(value);
        } else if (symbol instanceof Constant) {
            writeConstant((Constant) symbol);
        } else {
            throw new IllegalStateException("Cannot write this value: " + symbol + " of type: " + symbol.getClass().getName());
        }
    }

    protected void writeBlockName(InstructionBlock block) {
        final String name = block.getName();
        if (LLVMIdentifier.isImplicitBlockName(name)) {
            write("%");
            write(LLVMIdentifier.extractLabelFromImplicitBlockName(name));
        } else {
            write(name);
        }
    }

    protected void writeAttributesGroupIfPresent(AttributesGroup attrGroup) {
        if (attrGroup != null) {
            writeAttributesGroup(attrGroup);
        }
    }

    protected void writeAttributesGroup(AttributesGroup attr) {
        for (Attribute a : attr.getAttributes()) {
            write(" ");
            write(a.getIrString());
        }
    }

    protected void writeKnownAttributesGroup(AttributesGroup attr) {
        for (Attribute a : attr.getAttributes()) {
            if (a instanceof Attribute.KnownAttribute) {
                write(" ");
                write(a.getIrString());
            }
        }
    }

    protected void writeMetadataReference(MDBaseNode node) {
        if (node instanceof MDNamedNode) {
            write("!");
            write(((MDNamedNode) node).getName());
        } else {
            writef("!%d", ((IRWriterModelVisitorV38) visitors.getModelVisitor()).addMetadata(node));
        }
    }

    protected void writeMetadataValue(MDBaseNode node) {
        if (node instanceof MDNode) {
            writef("!%d", ((IRWriterModelVisitorV38) visitors.getModelVisitor()).addMetadata(node));
        } else {
            node.accept(visitors.getMetadataVisitor());
        }
    }

}
