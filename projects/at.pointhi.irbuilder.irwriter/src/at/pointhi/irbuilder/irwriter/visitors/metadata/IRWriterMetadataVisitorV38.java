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

package at.pointhi.irbuilder.irwriter.visitors.metadata;

import com.oracle.truffle.llvm.parser.metadata.MDAttachment;
import com.oracle.truffle.llvm.parser.metadata.MDBasicType;
import com.oracle.truffle.llvm.parser.metadata.MDCompileUnit;
import com.oracle.truffle.llvm.parser.metadata.MDCompositeType;
import com.oracle.truffle.llvm.parser.metadata.MDDerivedType;
import com.oracle.truffle.llvm.parser.metadata.MDEmptyNode;
import com.oracle.truffle.llvm.parser.metadata.MDEnumerator;
import com.oracle.truffle.llvm.parser.metadata.MDExpression;
import com.oracle.truffle.llvm.parser.metadata.MDFile;
import com.oracle.truffle.llvm.parser.metadata.MDFnNode;
import com.oracle.truffle.llvm.parser.metadata.MDGenericDebug;
import com.oracle.truffle.llvm.parser.metadata.MDGlobalVariable;
import com.oracle.truffle.llvm.parser.metadata.MDGlobalVariableExpression;
import com.oracle.truffle.llvm.parser.metadata.MDImportedEntity;
import com.oracle.truffle.llvm.parser.metadata.MDKind;
import com.oracle.truffle.llvm.parser.metadata.MDLexicalBlock;
import com.oracle.truffle.llvm.parser.metadata.MDLexicalBlockFile;
import com.oracle.truffle.llvm.parser.metadata.MDLocalVariable;
import com.oracle.truffle.llvm.parser.metadata.MDLocation;
import com.oracle.truffle.llvm.parser.metadata.MDMacro;
import com.oracle.truffle.llvm.parser.metadata.MDMacroFile;
import com.oracle.truffle.llvm.parser.metadata.MDModule;
import com.oracle.truffle.llvm.parser.metadata.MDNamedNode;
import com.oracle.truffle.llvm.parser.metadata.MDNamespace;
import com.oracle.truffle.llvm.parser.metadata.MDNode;
import com.oracle.truffle.llvm.parser.metadata.MDObjCProperty;
import com.oracle.truffle.llvm.parser.metadata.MDOldNode;
import com.oracle.truffle.llvm.parser.metadata.MDReference;
import com.oracle.truffle.llvm.parser.metadata.MDString;
import com.oracle.truffle.llvm.parser.metadata.MDSubprogram;
import com.oracle.truffle.llvm.parser.metadata.MDSubrange;
import com.oracle.truffle.llvm.parser.metadata.MDSubroutine;
import com.oracle.truffle.llvm.parser.metadata.MDSymbolReference;
import com.oracle.truffle.llvm.parser.metadata.MDTemplateType;
import com.oracle.truffle.llvm.parser.metadata.MDTemplateTypeParameter;
import com.oracle.truffle.llvm.parser.metadata.MDTemplateValue;
import com.oracle.truffle.llvm.parser.metadata.MDValue;
import com.oracle.truffle.llvm.parser.metadata.MetadataVisitor;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

import at.pointhi.irbuilder.irwriter.IRWriter;
import at.pointhi.irbuilder.irwriter.IRWriterVersion;
import at.pointhi.irbuilder.irwriter.visitors.IRWriterBaseVisitor;

public class IRWriterMetadataVisitorV38 extends IRWriterBaseVisitor implements MetadataVisitor {

    public IRWriterMetadataVisitorV38(IRWriterVersion.IRWriterVisitors visitors, IRWriter.PrintTarget target) {
        super(visitors, target);
    }

    @Override
    public void visit(MDAttachment alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDBasicType alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDCompileUnit alias) {
        write("!DICompileUnit(");

        writef("language: %s, ", alias.getLanguage().toString());

        write("file: ");
        writeMetadataReference(alias.getFile().get());
        write(", ");

        writef("producer: ");
        writeMetadataValue(alias.getProducer().get());
        write(", ");

        writef("isOptimized: %b, ", alias.isOptimized());

        writef("runtimeVersion: %d", alias.getRuntimeVersion());

        // TODO: finish

        write(")");
    }

    @Override
    public void visit(MDCompositeType alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDDerivedType alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDEmptyNode alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDEnumerator alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDExpression alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDFile alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDFnNode alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDGenericDebug alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDGlobalVariable alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDImportedEntity alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDKind alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDLexicalBlock alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDLexicalBlockFile alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDLocalVariable alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDMacro alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDMacroFile alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDModule alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDNamedNode alias) {
        write("!{");
        boolean first = true;
        for (MDReference ref : alias) {
            if (first) {
                first = false;
            } else {
                write(", ");
            }

            writeMetadataReference(ref.get());
        }
        writeln("}");
    }

    @Override
    public void visit(MDNamespace alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDNode alias) {
        write("!{");
        boolean first = true;
        for (MDReference ref : alias) {
            if (first) {
                first = false;
            } else {
                write(", ");
            }

            writeMetadataValue(ref.get());
        }
        write("}");
    }

    @Override
    public void visit(MDObjCProperty alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDOldNode alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDString alias) {
        writef("!\"%s\"", alias.getString()); // TODO: escaping
    }

    @Override
    public void visit(MDSubprogram alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDSubrange alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDSubroutine alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDSymbolReference alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDTemplateType alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDTemplateTypeParameter alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDTemplateValue alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDValue alias) {
        final Symbol sym = alias.getValue().get();
        writeSymbolType(sym);
        write(" ");
        writeInnerSymbolValue(sym);
    }

    @Override
    public void visit(MDReference alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDLocation alias) {
        write("!DILocation(");
        writef("line: %d, ", alias.getLine());
        writef("column: %d, ", alias.getColumn());
        write("scope: ");
        writeMetadataReference(alias.getScope().get());
        write(")");
    }

    @Override
    public void visit(MDGlobalVariableExpression alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }
}
