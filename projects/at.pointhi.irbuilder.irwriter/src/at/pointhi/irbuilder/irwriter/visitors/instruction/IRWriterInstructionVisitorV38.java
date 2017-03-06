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
import com.oracle.truffle.llvm.parser.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.InlineAsmConstant;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Call;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.CallInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.GetElementPointerInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.LoadInstruction;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;
import com.oracle.truffle.llvm.runtime.types.symbols.ValueSymbol;

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
            write(load.getAtomicOrdering().toString()); // sulong specific toString
        }

        if (load.getAlign() != 0) {
            write(String.format(", %s %d", LLVMIR_LABEL_ALIGN, 1 << (load.getAlign() - 1)));
        }

        writeln();
    }

    @Override
    protected void writeFunctionCall(Call call) {
        write(LLVMIR_LABEL_CALL);
        write(" ");
        final Symbol callTarget = call.getCallTarget();
        if (callTarget instanceof FunctionType) {
            // <ty>
            final FunctionType decl = (FunctionType) call.getCallTarget();

            writeType(decl.getReturnType());

            if (decl.isVarArg() || (decl.getReturnType() instanceof PointerType && ((PointerType) decl.getReturnType()).getPointeeType() instanceof FunctionType)) {
                write(" ");
                writeFormalArguments(decl);
            }
            write(String.format(" %s", decl.getName()));

        } else if (callTarget instanceof CallInstruction) {
            // final FunctionType decl = ((CallInstruction) callTarget).getCallType();
            final FunctionType decl = (FunctionType) ((CallInstruction) callTarget).getCallTarget();
            writeType(decl.getReturnType());
            write(String.format(" %s", ((CallInstruction) callTarget).getName()));

        } else if (callTarget instanceof FunctionParameter) {
            callTarget.getType().accept(visitors.getTypeVisitor());
            write(String.format(" %s ", ((FunctionParameter) callTarget).getName()));

        } else if (callTarget instanceof ValueSymbol) {
            Type targetType;
            if (callTarget instanceof LoadInstruction) {
                targetType = ((LoadInstruction) callTarget).getSource().getType();
            } else {
                targetType = callTarget.getType();
            }

            while (targetType instanceof PointerType) {
                targetType = ((PointerType) targetType).getPointeeType();
            }

            if (targetType instanceof FunctionType) {
                final FunctionType decl = (FunctionType) targetType;

                writeType(decl.getReturnType());

                if (decl.isVarArg() || (decl.getReturnType() instanceof PointerType && ((PointerType) decl.getReturnType()).getPointeeType() instanceof FunctionType)) {
                    write(" ");
                    writeFormalArguments(decl);
                    write("*");
                }

                write(" ");
                writeInnerSymbolValue(callTarget);

            } else {
                throw new AssertionError("unexpected target type: " + targetType.getClass().getName());
            }

        } else if (callTarget instanceof InlineAsmConstant) {
            writeConstant((InlineAsmConstant) call.getCallTarget());

        } else if (callTarget instanceof Constant) {
            writeType(callTarget.getType());
            write(" ");
            writeConstant((Constant) callTarget);

        } else {
            throw new AssertionError("unexpected target type: " + call.getCallTarget().getClass().getName());
        }

        writeActualArgs(call);
    }

}
