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
import com.oracle.truffle.llvm.parser.metadata.MDBaseNode;
import com.oracle.truffle.llvm.parser.metadata.MDBasicType;
import com.oracle.truffle.llvm.parser.metadata.MDCompileUnit;
import com.oracle.truffle.llvm.parser.metadata.MDCompositeType;
import com.oracle.truffle.llvm.parser.metadata.MDDerivedType;
import com.oracle.truffle.llvm.parser.metadata.MDEnumerator;
import com.oracle.truffle.llvm.parser.metadata.MDExpression;
import com.oracle.truffle.llvm.parser.metadata.MDFile;
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
import com.oracle.truffle.llvm.parser.metadata.MDString;
import com.oracle.truffle.llvm.parser.metadata.MDSubprogram;
import com.oracle.truffle.llvm.parser.metadata.MDSubrange;
import com.oracle.truffle.llvm.parser.metadata.MDSubroutine;
import com.oracle.truffle.llvm.parser.metadata.MDTemplateType;
import com.oracle.truffle.llvm.parser.metadata.MDTemplateTypeParameter;
import com.oracle.truffle.llvm.parser.metadata.MDTemplateValue;
import com.oracle.truffle.llvm.parser.metadata.MDValue;
import com.oracle.truffle.llvm.parser.metadata.MDVoidNode;
import com.oracle.truffle.llvm.parser.metadata.MetadataVisitor;
import com.oracle.truffle.llvm.parser.metadata.MDBasicType.DwarfEncoding;
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
        write("!");
        write(alias.getKind().getName());

        write(" ");
        writeMetadataValueReference(alias.getValue());
    }

    @Override
    public void visit(MDBasicType alias) {
        MDNodeWriter writer = new MDNodeWriter("DIBasicType");

        // TODO: tag
        writer.writeKeyValue("name", alias.getName());
        writer.writeKeyValue("size", alias.getSize());
        if (alias.getAlign() != 0) {
            writer.writeKeyValue("align", alias.getAlign());
        }

        if (alias.getEncoding() != DwarfEncoding.UNKNOWN) {
            writer.writeRawKeyValue("encoding", MetadataUtil.decode(alias.getEncoding()));
        }

        writer.writeEplioge();
    }

    @Override
    public void visit(MDCompileUnit alias) {
        MDNodeWriter writer = new MDNodeWriter("DICompileUnit", true);

        writer.writeRawKeyValue("language", MetadataUtil.decode(alias.getLanguage()));
        writer.writeKeyValue("file", alias.getFile());
        writer.writeKeyValue("producer", alias.getProducer());
        writer.writeKeyValue("isOptimized", alias.isOptimized());
        writer.writeKeyValueIfNotEmpty("flags", alias.getFlags());
        writer.writeKeyValue("runtimeVersion", alias.getRuntimeVersion());
        writer.writeKeyValueIfNotEmpty("splitDebugFilename", alias.getSplitdebugFilename());
        // TODO: emissionKind
        writer.writeKeyValue("enums", alias.getEnums());
        writer.writeKeyValueIfNotEmpty("retainedTypes", alias.getRetainedTypes());
        writer.writeKeyValueIfNotEmpty("globals", alias.getGlobalVariables());
        writer.writeKeyValueIfNotEmpty("imports", alias.getImportedEntities());
        writer.writeKeyValueIfNotEmpty("macros", alias.getMacros());
        if (alias.getDwoId() != 0) {
            writer.writeKeyValue("dwoId", alias.getDwoId());
        }

        writer.writeEplioge();
    }

    @Override
    public void visit(MDCompositeType alias) {
        MDNodeWriter writer = new MDNodeWriter("DICompositeType");

        writer.writeRawKeyValue("tag", MetadataUtil.decode(alias.getTag()));
        writer.writeKeyValueIfNotEmpty("baseType", alias.getBaseType());
        writer.writeKeyValue("name", alias.getName());
        writer.writeKeyValue("file", alias.getFile());
        writer.writeKeyValue("line", alias.getLine());
        writer.writeKeyValue("size", alias.getSize());
        if (alias.getAlign() != 0) {
            writer.writeKeyValue("align", alias.getAlign());
        }
        writer.writeKeyValueIfNotEmpty("identifier", alias.getIdentifier());
        writer.writeKeyValue("elements", alias.getMembers());

        writer.writeEplioge();
    }

    @Override
    public void visit(MDDerivedType alias) {
        MDNodeWriter writer = new MDNodeWriter("DIDerivedType");

        writer.writeRawKeyValue("tag", MetadataUtil.decode(alias.getTag()));
        writer.writeKeyValueIfNotEmpty("baseType", alias.getBaseType());
        writer.writeKeyValueIfNotEmpty("name", alias.getName());
        writer.writeKeyValue("size", alias.getSize());
        if (alias.getAlign() != 0) {
            writer.writeKeyValue("align", alias.getAlign());
        }

        writer.writeEplioge();
    }

    @Override
    public void visit(MDEnumerator alias) {
        MDNodeWriter writer = new MDNodeWriter("DIEnumerator");

        writer.writeKeyValue("name", alias.getName());
        writer.writeKeyValue("value", alias.getValue());

        writer.writeEplioge();
    }

    @Override
    public void visit(MDExpression alias) {
        MDNodeWriter writer = new MDNodeWriter("DIExpression");

        // TODO: elements

        writer.writeEplioge();
    }

    @Override
    public void visit(MDFile alias) {
        MDNodeWriter writer = new MDNodeWriter("DIFile");

        writer.writeKeyValue("filename", alias.getFile());
        writer.writeKeyValue("directory", alias.getDirectory());

        writer.writeEplioge();
    }

    @Override
    public void visit(MDGenericDebug alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDGlobalVariable alias) {
        MDNodeWriter writer = new MDNodeWriter("DIGlobalVariable");

        writer.writeKeyValue("name", alias.getName());
        writer.writeKeyValue("linkageName", alias.getLinkageName());
        writer.writeKeyValue("scope", alias.getScope());
        writer.writeKeyValue("file", alias.getFile());
        writer.writeKeyValue("line", alias.getLine());
        writer.writeKeyValue("type", alias.getType());
        writer.writeKeyValue("isLocal", alias.isLocalToCompileUnit());
        writer.writeKeyValue("isDefinition", alias.isDefinedInCompileUnit());
        writer.writeKeyValue("variable", alias.getVariable());
        // TODO: declaration

        writer.writeEplioge();
    }

    @Override
    public void visit(MDImportedEntity alias) {
        MDNodeWriter writer = new MDNodeWriter("DIImportedEntity");

        // TODO: tag
        writer.writeKeyValue("name", alias.getName());
        writer.writeKeyValue("scope", alias.getScope());
        writer.writeKeyValue("entity", alias.getEntity());
        writer.writeKeyValue("line", alias.getLine());

        writer.writeEplioge();
    }

    @Override
    public void visit(MDKind alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDLexicalBlock alias) {
        MDNodeWriter writer = new MDNodeWriter("DILexicalBlock", true);

        writer.writeKeyValue("scope", alias.getScope());
        writer.writeKeyValue("file", alias.getFile());
        writer.writeKeyValue("line", alias.getLine());
        writer.writeKeyValue("column", alias.getColumn());

        writer.writeEplioge();
    }

    @Override
    public void visit(MDLexicalBlockFile alias) {
        MDNodeWriter writer = new MDNodeWriter("DILexicalBlock");

        // writer.writeKeyValue("scope", alias.getScope()); // TODO: wait until available in sulong
        writer.writeKeyValue("file", alias.getFile());
        writer.writeKeyValue("discriminator", alias.getDiscriminator());

        writer.writeEplioge();
    }

    @Override
    public void visit(MDLocalVariable alias) {
        MDNodeWriter writer = new MDNodeWriter("DILocalVariable");

        writer.writeKeyValue("name", alias.getName());
        if (alias.getArg() != 0) {
            writer.writeKeyValue("arg", alias.getArg());
        }
        writer.writeKeyValue("scope", alias.getScope());
        writer.writeKeyValue("file", alias.getFile());
        writer.writeKeyValue("line", alias.getLine());
        writer.writeKeyValue("type", alias.getType());
        // TODO: flags

        writer.writeEplioge();
    }

    @Override
    public void visit(MDMacro alias) {
        MDNodeWriter writer = new MDNodeWriter("DIMacro");

        // TODO: macinfo
        writer.writeKeyValue("line", alias.getLine());
        writer.writeKeyValue("name", alias.getName());
        writer.writeKeyValue("value", alias.getValue());

        writer.writeEplioge();
    }

    @Override
    public void visit(MDMacroFile alias) {
        MDNodeWriter writer = new MDNodeWriter("DIMacroFile");

        // TODO: macinfo
        writer.writeKeyValue("line", alias.getLine());
        writer.writeKeyValue("file", alias.getFile());
        writer.writeKeyValue("nodes", alias.getElements());

        writer.writeEplioge();
    }

    @Override
    public void visit(MDModule alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDNamedNode alias) {
        write("!{");
        boolean first = true;
        for (MDBaseNode ref : alias) {
            if (first) {
                first = false;
            } else {
                write(", ");
            }

            writeMetadataValueReference(ref);
        }
        writeln("}");
    }

    @Override
    public void visit(MDNamespace alias) {
        MDNodeWriter writer = new MDNodeWriter("DINamespace");

        writer.writeKeyValue("name", alias.getName());
        writer.writeKeyValue("scope", alias.getScope());
        writer.writeKeyValue("file", alias.getFile());
        writer.writeKeyValue("line", alias.getLine());

        writer.writeEplioge();
    }

    @Override
    public void visit(MDNode alias) {
        write("!{");
        boolean first = true;
        for (MDBaseNode ref : alias) {
            if (first) {
                first = false;
            } else {
                write(", ");
            }

            writeMetadataValueReference(ref);
        }
        write("}");
    }

    @Override
    public void visit(MDObjCProperty alias) {
        MDNodeWriter writer = new MDNodeWriter("DIObjCProperty");

        writer.writeKeyValue("name", alias.getName());
        writer.writeKeyValue("file", alias.getFile());
        writer.writeKeyValue("line", alias.getLine());
        writer.writeKeyValue("setter", alias.getSetterName());
        writer.writeKeyValue("getter", alias.getGetterName());
        writer.writeKeyValue("attributes", alias.getAttribute());
        writer.writeKeyValue("type", alias.getType());

        writer.writeEplioge();
    }

    @Override
    public void visit(MDString alias) {
        writef("!\"%s\"", alias.getString()); // TODO: escaping
    }

    @Override
    public void visit(MDSubprogram alias) {
        MDNodeWriter writer = new MDNodeWriter("DISubprogram", true);

        writer.writeKeyValue("name", alias.getName());
        writer.writeKeyValueIfNotEmpty("linkageName", alias.getLinkageName());
        writer.writeKeyValue("scope", alias.getScope());
        writer.writeKeyValue("file", alias.getFile());
        writer.writeKeyValue("line", alias.getLine());
        writer.writeKeyValue("type", alias.getType());
        writer.writeKeyValue("isLocal", alias.isLocalToUnit());
        writer.writeKeyValue("isDefinition", alias.isDefinedInCompileUnit());
        writer.writeKeyValue("scopeLine", alias.getScopeLine());
        writer.writeKeyValueIfNotEmpty("containingType", alias.getContainingType());
        // TODO: virtuality
        if (alias.getVirtualIndex() != 0) {
            writer.writeKeyValue("virtualIndex", alias.getVirtualIndex());
        }
        // TOOD: flags
        writer.writeKeyValue("isOptimized", alias.isOptimized());
        writer.writeKeyValue("unit", alias.getCompileUnit());
        writer.writeKeyValueIfNotEmpty("templateParams", alias.getTemplateParams());
        writer.writeKeyValueIfNotEmpty("declaration", alias.getDeclaration());
        writer.writeKeyValue("variables", alias.getVariables());
        // TODO: thrownTypes

        writer.writeEplioge();
    }

    @Override
    public void visit(MDSubrange alias) {
        MDNodeWriter writer = new MDNodeWriter("DISubrange");

        writer.writeKeyValue("count", alias.getSize());
        if (alias.getSize() != -1) {
            writer.writeKeyValue("lowerBound", alias.getLowerBound());
        }

        writer.writeEplioge();
    }

    @Override
    public void visit(MDSubroutine alias) {
        MDNodeWriter writer = new MDNodeWriter("DISubroutineType");

        writer.writeKeyValue("types", alias.getTypes());

        writer.writeEplioge();
    }

    @Override
    public void visit(MDTemplateType alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    @Override
    public void visit(MDTemplateTypeParameter alias) {
        MDNodeWriter writer = new MDNodeWriter("DITemplateTypeParameter");

        writer.writeKeyValue("name", alias.getName());
        writer.writeKeyValue("type", alias.getBaseType());
        // TODO: scope, file

        writer.writeEplioge();
    }

    @Override
    public void visit(MDTemplateValue alias) {
        MDNodeWriter writer = new MDNodeWriter("DITemplateValueParameter");

        writer.writeKeyValue("name", alias.getName());
        writer.writeKeyValue("type", alias.getType());
        writer.writeKeyValue("value", alias.getValue());

        writer.writeEplioge();
    }

    @Override
    public void visit(MDValue alias) {
        final Symbol sym = alias.getValue();
        writeSymbolType(sym);
        write(" ");
        writeInnerSymbolValue(sym);
    }

    @Override
    public void visit(MDLocation alias) {
        MDNodeWriter writer = new MDNodeWriter("DILocation");

        writer.writeKeyValue("line", alias.getLine());
        writer.writeKeyValue("column", alias.getColumn());
        writer.writeKeyValue("scope", alias.getScope());
        writer.writeKeyValueIfNotEmpty("inlineAt", alias.getInlinedAt());

        writer.writeEplioge();
    }

    @Override
    public void visit(MDGlobalVariableExpression alias) {
        write("!{} ; TODO: " + alias.getClass().getSimpleName()); // TODO: implement
    }

    public void visit(MDVoidNode alias) {
        write("!{}");
    }

    protected class MDNodeWriter {
        private boolean first = true;

        protected MDNodeWriter(String nodeName, boolean distinct) {
            writeProloge(nodeName, distinct);
        }

        protected MDNodeWriter(String nodeName) {
            this(nodeName, false);
        }

        private void writeProloge(String nodeName, boolean distinct) {
            if (distinct) {
                write("distinct ");
            }

            write("!");
            write(nodeName);
            write("(");
        }

        protected void writeEplioge() {
            write(")");
        }

        private void writeSeperator() {
            if (first) {
                first = false;
            } else {
                write(", ");
            }
        }

        protected void writeRawKeyValue(String key, String value) {
            writeSeperator();
            writef("%s: %s", key, value);
        }

        protected void writeKeyValue(String key, boolean value) {
            writeSeperator();
            writef("%s: %b", key, value);
        }

        protected void writeKeyValue(String key, long value) {
            writeSeperator();
            writef("%s: %d", key, value);
        }

        protected void writeKeyValue(String key, double value) {
            writeSeperator();
            writef("%s: %f", key, value);
        }

        protected void writeKeyValue(String key, MDBaseNode value) {
            writeSeperator();
            writef("%s: ", key);
            if (value instanceof MDString) {
                writeMetadataString(value);
            } else {
                writeMetadataValueReference(value);
            }
        }

        protected void writeKeyValueIfNotEmpty(String key, MDBaseNode value) {
            if (!(value instanceof MDVoidNode)) {
                writeKeyValue(key, value);
            }
        }

    }
}
