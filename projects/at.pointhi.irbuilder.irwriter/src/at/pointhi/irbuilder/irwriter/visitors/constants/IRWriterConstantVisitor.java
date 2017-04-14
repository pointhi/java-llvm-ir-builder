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

package at.pointhi.irbuilder.irwriter.visitors.constants;

import java.math.BigInteger;

import com.oracle.truffle.llvm.parser.model.enums.AsmDialect;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.symbols.constants.BinaryOperationConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.BlockAddressConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.CastConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.CompareConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.GetElementPointerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.InlineAsmConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.NullConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.StringConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.UndefinedConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.aggregate.AggregateConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.aggregate.ArrayConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.aggregate.StructureConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.aggregate.VectorConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.floatingpoint.DoubleConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.floatingpoint.FloatConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.floatingpoint.X86FP80Constant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.BigIntegerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.model.visitors.ConstantVisitor;
import com.oracle.truffle.llvm.runtime.types.AggregateType;
import com.oracle.truffle.llvm.runtime.types.ArrayType;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

import at.pointhi.irbuilder.irwriter.IRWriter;
import at.pointhi.irbuilder.irwriter.IRWriterVersion;
import at.pointhi.irbuilder.irwriter.visitors.IRWriterBaseVisitor;

public class IRWriterConstantVisitor extends IRWriterBaseVisitor implements ConstantVisitor {

    public IRWriterConstantVisitor(IRWriterVersion.IRWriterVisitors visitors, IRWriter.PrintTarget target) {
        super(visitors, target);
    }

    private void writeAggregateElements(AggregateConstant aggregate) {
        for (int i = 0; i < aggregate.getElementCount(); i++) {
            if (i != 0) {
                write(", ");
            }

            final Symbol symbol = aggregate.getElement(i);

            writeSymbolType(symbol);
            write(" ");
            writeInnerSymbolValue(symbol);
        }
    }

    @Override
    public void visit(ArrayConstant arrayConstant) {
        write("[ ");
        writeAggregateElements(arrayConstant);
        write(" ]");
    }

    @Override
    public void visit(StructureConstant structureConstant) {
        if (structureConstant.isPacked()) {
            write("<");
        }
        write("{ ");
        writeAggregateElements(structureConstant);
        write(" }");
        if (structureConstant.isPacked()) {
            write(">");
        }
    }

    @Override
    public void visit(VectorConstant vectorConstant) {
        write("< ");
        writeAggregateElements(vectorConstant);
        write(" >");
    }

    @Override
    public void visit(BigIntegerConstant bigIntegerConstant) {
        final BigInteger value = bigIntegerConstant.getValue();

        if (bigIntegerConstant.getType().getBitSize() == 1) {
            write(value.equals(BigInteger.ZERO) ? "false" : "true");
        } else {
            write(value.toString());
        }
    }

    @Override
    public void visit(BinaryOperationConstant binaryOperationConstant) {
        write(binaryOperationConstant.getOperator().getIrString());
        write(" (");

        writeType(binaryOperationConstant.getLHS().getType());
        write(" ");
        writeInnerSymbolValue(binaryOperationConstant.getLHS());
        write(", ");

        writeType(binaryOperationConstant.getRHS().getType());
        write(" ");
        writeInnerSymbolValue(binaryOperationConstant.getRHS());

        write(")");
    }

    private static final String LLVMIR_LABEL_BLOCKADDRESS = "blockaddress";

    @Override
    public void visit(BlockAddressConstant blockAddressConstant) {
        write(LLVMIR_LABEL_BLOCKADDRESS);
        write(" (");
        write(blockAddressConstant.getFunction().getName());
        write(", ");
        writeBlockName(blockAddressConstant.getInstructionBlock());
        write(")");
    }

    @Override
    public void visit(CastConstant castConstant) {
        write(castConstant.getOperator().getIrString());
        write(" (");

        Symbol valueSymbol = castConstant.getValue();

        writeSymbolType(valueSymbol);
        write(" ");
        writeInnerSymbolValue(valueSymbol);
        write(" to ");
        writeType(castConstant.getType());
        write(")");
    }

    private static final String LLVMIR_LABEL_COMPARE = "icmp";
    private static final String LLVMIR_LABEL_COMPARE_FP = "fcmp";

    @Override
    public void visit(CompareConstant compareConstant) {
        if (compareConstant.getOperator().isFloatingPoint()) {
            write(LLVMIR_LABEL_COMPARE_FP);
        } else {
            write(LLVMIR_LABEL_COMPARE);
        }

        write(" ");
        write(compareConstant.getOperator().getIrString());
        write(" (");

        writeSymbolType(compareConstant.getLHS());
        write(" ");
        writeInnerSymbolValue(compareConstant.getLHS());
        write(", ");

        writeSymbolType(compareConstant.getRHS());
        write(" ");
        writeInnerSymbolValue(compareConstant.getRHS());

        write(")");
    }

    @Override
    public void visit(DoubleConstant doubleConstant) {
        // see http://llvm.org/releases/3.2/docs/LangRef.html#simpleconstants for
        // why we cannot use String.format(Locale.ROOT, "%e", doubleConstant.getValue())
        final long bits = Double.doubleToRawLongBits(doubleConstant.getValue());
        writef("0x%x", bits);
    }

    @Override
    public void visit(FloatConstant floatConstant) {
        // see http://llvm.org/releases/3.2/docs/LangRef.html#simpleconstants for
        // why we cannot use String.format(Locale.ROOT, "%e", doubleConstant.getValue())
        final long bits = Double.doubleToRawLongBits(floatConstant.getValue());
        writef("0x%x", bits);
    }

    private static final int HEX_MASK = 0xf;

    private static final int BYTE_MSB_SHIFT = 4;

    @Override
    public void visit(X86FP80Constant x86fp80Constant) {
        final byte[] value = x86fp80Constant.getValue();
        write("0xK");
        for (byte aValue : value) {
            write(String.valueOf((aValue >>> BYTE_MSB_SHIFT) & HEX_MASK));
            write(String.valueOf(aValue & HEX_MASK));
        }
    }

    private static final String LLVMIR_LABEL_DECLARE_FUNCTION = "declare";

    @Override
    public void visit(FunctionDeclaration functionDeclaration) {
        write(LLVMIR_LABEL_DECLARE_FUNCTION);
        write(" ");
        writeType(functionDeclaration.getType().getReturnType());
        writef(" %s", functionDeclaration.getName());
        writeFormalArguments(functionDeclaration.getType());
    }

    private static final String LLVMIR_LABEL_DEFINE_FUNCTION = "define";

    @Override
    public void visit(FunctionDefinition functionDefinition) {
        write(LLVMIR_LABEL_DEFINE_FUNCTION);
        write(" ");
        writeType(functionDefinition.getType().getReturnType());
        writef(" %s", functionDefinition.getName());
        writeFormalArguments(functionDefinition.getType());
    }

    protected static final String LLVMIR_LABEL_GET_ELEMENT_POINTER = "getelementptr";

    @Override
    public void visit(GetElementPointerConstant getElementPointerConstant) {
        // getelementptr
        write(LLVMIR_LABEL_GET_ELEMENT_POINTER);

        // [inbounds]
        if (getElementPointerConstant.isInbounds()) {
            write(" inbounds");
        }

        // <pty>* <ptrval>
        write(" (");
        writeType(getElementPointerConstant.getBasePointer().getType());
        write(" ");
        writeInnerSymbolValue(getElementPointerConstant.getBasePointer());

        // {, <ty> <idx>}*
        for (final Symbol sym : getElementPointerConstant.getIndices()) {
            write(", ");
            writeType(sym.getType());
            write(" ");
            writeInnerSymbolValue(sym);
        }

        write(")");
    }

    private static final String LLVMIR_LABEL_ASM = "asm";

    private static final String LLVMIR_ASM_KEYWORD_SIDEEFFECT = "sideeffect";

    private static final String LLVMIR_ASM_KEYWORD_ALIGNSTACK = "alignstack";

    @Override
    public void visit(InlineAsmConstant inlineAsmConstant) {
        final FunctionType decl = (FunctionType) ((PointerType) inlineAsmConstant.getType()).getPointeeType();

        writeType(decl.getReturnType());
        write(" ");

        if (decl.isVarargs() || (decl.getReturnType() instanceof PointerType && ((PointerType) decl.getReturnType()).getPointeeType() instanceof FunctionType)) {
            writeFormalArguments(decl);
            write(" ");
        }

        write(LLVMIR_LABEL_ASM);

        if (inlineAsmConstant.hasSideEffects()) {
            write(" ");
            write(LLVMIR_ASM_KEYWORD_SIDEEFFECT);
        }

        if (inlineAsmConstant.needsAlignedStack()) {
            write(" ");
            write(LLVMIR_ASM_KEYWORD_ALIGNSTACK);
        }

        if (inlineAsmConstant.getDialect() != AsmDialect.AT_T) {
            write(" ");
            write(inlineAsmConstant.getDialect().getIrString());
        }

        write(" ");
        write(inlineAsmConstant.getAsmExpression());

        write(", ");
        write(inlineAsmConstant.getAsmFlags());
    }

    @Override
    public void visit(IntegerConstant integerConstant) {
        final long value = integerConstant.getValue();
        if (integerConstant.getType().getBitSize() == 1) {
            write(value == 0 ? "false" : "true");
        } else {
            write(String.valueOf(value));
        }
    }

    private static final String LLVMIR_LABEL_ZEROINITIALIZER = "zeroinitializer";

    @Override
    public void visit(NullConstant nullConstant) {
        if (Type.isIntegerType(nullConstant.getType())) {
            if (nullConstant.getType().getBitSize() == 1) {
                write("false");
            } else {
                write(String.valueOf(0));
            }

        } else if (Type.isFloatingpointType(nullConstant.getType())) {
            switch (((PrimitiveType) nullConstant.getType()).getPrimitiveKind()) {
                case X86_FP80:
                    write("0xK00000000000000000000");
                    break;

                default:
                    write(String.valueOf(0.0));
                    break;
            }
        } else if (nullConstant.getType() instanceof AggregateType) {
            write(LLVMIR_LABEL_ZEROINITIALIZER);
        } else {
            write("null");
        }
    }

    @Override
    public void visit(StringConstant stringConstant) {
        write("c\"");
        for (int i = 0; i < stringConstant.getString().length(); i++) {
            byte b = (byte) stringConstant.getString().charAt(i);
            if (b < ' ' || b >= '~' || b == '"' || b == '\\') {
                writef("\\%02X", b);
            } else {
                write(Character.toString((char) b));
            }
        }
        if (stringConstant.getType() instanceof ArrayType && ((ArrayType) stringConstant.getType()).getNumberOfElements() > stringConstant.getString().length()) {
            write("\\00");
        }
        write("\"");
    }

    @Override
    public void visit(UndefinedConstant undefinedConstant) {
        write("undef");
    }

}
