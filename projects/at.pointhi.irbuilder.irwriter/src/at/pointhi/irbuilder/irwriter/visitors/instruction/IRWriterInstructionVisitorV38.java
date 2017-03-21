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

package at.pointhi.irbuilder.irwriter.visitors.instruction;

import com.oracle.truffle.llvm.parser.model.enums.AtomicOrdering;
import com.oracle.truffle.llvm.parser.model.enums.SynchronizationScope;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Call;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.GetElementPointerInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.LoadInstruction;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;
import at.pointhi.irbuilder.irwriter.IRWriter;
import at.pointhi.irbuilder.irwriter.IRWriterVersion;

public class IRWriterInstructionVisitorV38 extends IRWriterInstructionVisitor {

    public IRWriterInstructionVisitorV38(IRWriterVersion.IRWriterVisitors visitors, IRWriter.PrintTarget target) {
        super(visitors, target);
    }

    @Override
    public void visit(GetElementPointerInstruction gep) {
        write(INDENTATION);
        // <result> = getelementptr
        writef("%s = %s ", gep.getName(), LLVMIR_LABEL_GET_ELEMENT_POINTER);

        // [inbounds]
        if (gep.isInbounds()) {
            write(LLVMIR_LABEL_GET_ELEMENT_POINTER_INBOUNDS);
            write(" ");
        }

        writeType(((PointerType) gep.getBasePointer().getType()).getPointeeType());
        write(", ");

        // <pty>* <ptrval>
        writeType(gep.getBasePointer().getType());
        write(" ");
        writeInnerSymbolValue(gep.getBasePointer());

        // {, <ty> <idx>}*
        for (final Symbol sym : gep.getIndices()) {
            write(", ");
            writeType(sym.getType());
            write(" ");
            writeInnerSymbolValue(sym);
        }

        writeln();
    }

    @Override
    public void visit(LoadInstruction load) {
        write(INDENTATION);
        writef("%s = %s", load.getName(), LLVMIR_LABEL_LOAD);

        if (load.getAtomicOrdering() != AtomicOrdering.NOT_ATOMIC) {
            write(" ");
            write(LLVMIR_LABEL_ATOMIC);
        }

        if (load.isVolatile()) {
            write(" ");
            write(LLVMIR_LABEL_VOLATILE);
        }

        if (load.getAtomicOrdering() == AtomicOrdering.NOT_ATOMIC) {
            write(" ");
            writeType(load.getType());
            write(",");
        }

        write(" ");
        writeType(load.getSource().getType());

        write(" ");
        writeInnerSymbolValue(load.getSource());

        if (load.getAtomicOrdering() != AtomicOrdering.NOT_ATOMIC) {
            if (load.getSynchronizationScope() == SynchronizationScope.SINGLE_THREAD) {
                write(" ");
                write(LLVMIR_LABEL_SINGLETHREAD);
            }

            write(" ");
            write(load.getAtomicOrdering().getIrString());
        }

        if (load.getAlign() != 0) {
            write(String.format(", %s %d", LLVMIR_LABEL_ALIGN, 1 << (load.getAlign() - 1)));
        }

        writeln();
    }

    /**
     * @see <a href="http://releases.llvm.org/3.8.1/docs/LangRef.html#call-instruction">LangRef</a>
     */
    @Override
    protected void writeFunctionCall(Call call) {
        // [tail | musttail | notail ] call
        write(LLVMIR_LABEL_CALL);
        write(" ");

        // [fast-math flags] [cconv] [ret attrs]

        /*
         * <ty>
         *
         * 'ty': the type of the call instruction itself which is also the type of the return value.
         * Functions that return no value are marked void.
         */
        writeType(getCallSymbolType(call));
        write(" ");

        /*
         * [<fnty>*]
         *
         * 'fnty': shall be the signature of the pointer to function value being invoked. The
         * argument types must match the types implied by this signature. This type can be omitted
         * if the function is not varargs and if the function type does not return a pointer to a
         * function.
         */
        final FunctionType funcType = getFunctionType(call);
        final Type retType = funcType.getReturnType();
        if (funcType.isVarargs() || (retType instanceof PointerType && ((PointerType) retType).getPointeeType() instanceof FunctionType)) {
            writeFormalArguments(funcType);
            write(" ");
        }

        /*
         * <fnptrval>
         *
         * 'fnptrval': An LLVM value containing a pointer to a function to be invoked. In most
         * cases, this is a direct function invocation, but indirect call's are just as possible,
         * calling an arbitrary pointer to function value.
         */
        writeInnerSymbolValue(call.getCallTarget());

        /*
         * (<function args>)
         *
         * 'function args': argument list whose types match the function signature argument types
         * and parameter attributes. All arguments must be of first class type. If the function
         * signature indicates the function accepts a variable number of arguments, the extra
         * arguments can be specified.
         */
        writeActualArgs(call);

        // [fn attrs] [ operand bundles ]
    }

}
