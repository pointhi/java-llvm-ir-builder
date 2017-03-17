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

import com.oracle.truffle.llvm.parser.model.enums.Visibility;
import com.oracle.truffle.llvm.parser.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.model.globals.GlobalAlias;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

import at.pointhi.irbuilder.irwriter.IRWriter;
import at.pointhi.irbuilder.irwriter.IRWriterVersion;

public class IRWriterModelVisitorV38 extends IRWriterModelVisitor {

    public IRWriterModelVisitorV38(IRWriterVersion.IRWriterVisitors visitors, IRWriter.PrintTarget target) {
        super(visitors, target);
    }

    private static final String UNRESOLVED_FORWARD_REFERENCE = "<unresolved>";

    @Override
    public void visit(GlobalAlias alias) {
        write(String.format("%s = %s ", alias.getName(), alias.getLinkage().getIrString()));

        if (alias.getVisibility() != Visibility.DEFAULT) {
            write(String.format("%s ", alias.getVisibility().getIrString()));
        }

        write("alias ");

        final Symbol val = alias.getValue();
        if (val == null) {
            writeln(UNRESOLVED_FORWARD_REFERENCE);
            return;
        }
        writeType(alias.getType());

        write(", ");
        writeType(val.getType());

        write(" ");
        writeInnerSymbolValue(val);
        writeln();
    }

    @Override
    protected void writeFunctionParameter(FunctionParameter param) {
        writeType(param.getType());
        if (!param.getName().matches("%\\d+")) {
            write(" ");
            write(param.getName());
        }
    }
}
