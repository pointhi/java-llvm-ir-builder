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
package at.pointhi.irbuilder.testgenerator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.llvm.parser.model.ModelModule;
import com.oracle.truffle.llvm.parser.model.enums.AsmDialect;
import com.oracle.truffle.llvm.parser.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.parser.model.enums.CastOperator;
import com.oracle.truffle.llvm.parser.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;
import com.oracle.truffle.llvm.test.options.TestOptions;

import at.pointhi.irbuilder.irbuilder.ModelModuleBuilder;
import at.pointhi.irbuilder.irbuilder.SimpleInstrunctionBuilder;
import at.pointhi.irbuilder.irbuilder.util.ConstantUtil;
import at.pointhi.irbuilder.testgenerator.util.IntegerBinaryOperations.UndefinedArithmeticResult;

@RunWith(Parameterized.class)
public class IntegerAssemblyCasts extends BaseSuite {

    private static final Path SUITE_DIR = Paths.get(TestOptions.PROJECT_ROOT + "/../cache/tests/irbuilder/IAssemblyCasts");

    private final Type fromType;
    private final Type toType;
    private final CastOperator cast;

    public IntegerAssemblyCasts(CastOperator cast, int fromBits, int toBits) {
        this.fromType = PrimitiveType.getIntegerType(fromBits);
        this.toType = PrimitiveType.getIntegerType(toBits);
        this.cast = cast;
    }

    @Override
    public Path getSuiteDir() {
        return SUITE_DIR;
    }

    @Override
    public Path getFilename() {
        return Paths.get(String.format("cast_i%d_to_i%d_(%s).ll", fromType.getBitSize(), toType.getBitSize(), cast.getIrString()));
    }

    @Parameters(name = "{index}: VarICasts[cast=0}, from=i{1}, to=i{2}]")
    public static Collection<Object[]> data() {
        List<Object[]> parameters = new LinkedList<>();

        for (int toBits = 8; toBits <= Long.SIZE; toBits *= 2) {
            for (int fromBits = 8; fromBits < toBits; fromBits *= 2) {
                addParameter(parameters, CastOperator.SIGN_EXTEND, fromBits, toBits);
                if (toBits == Long.SIZE && fromBits == Integer.SIZE) {
                    continue; // movzlq does not exist
                }
                addParameter(parameters, CastOperator.ZERO_EXTEND, fromBits, toBits);
            }
        }

        return parameters;
    }

    private static void addParameter(List<Object[]> parameters, CastOperator cast, int fromBits, int toBits) {
        parameters.add(new Object[]{cast, fromBits, toBits});
    }

    @Override
    public ModelModule constructModelModule() throws UndefinedArithmeticResult {
        ModelModuleBuilder builder = new ModelModuleBuilder();
        createMain(builder);

        return builder.getModelModule();
    }

    private void createMain(ModelModuleBuilder builder) {
        FunctionDefinition main = builder.createFunctionDefinition("main", 1, new FunctionType(PrimitiveType.I1, new Type[]{}, false));
        SimpleInstrunctionBuilder instr = new SimpleInstrunctionBuilder(builder, main);

        // check if bit's are stored in a truncated way
        IntegerConstant bitTruncConst = new IntegerConstant(toType, 1L << fromType.getBitSize());
        Instruction bitTruncatInstr = instr.cast(CastOperator.TRUNCATE, fromType, bitTruncConst);
        Instruction bitTruncCast = inlineCast(instr, cast, toType, bitTruncatInstr);
        Instruction bitTruncRet = instr.compare(CompareOperator.INT_NOT_EQUAL, bitTruncCast, 0);

        // check if zero extension works as expected
        IntegerConstant bitExtensionConst = new IntegerConstant(fromType, (1L << (fromType.getBitSize() - 1)) | 0x1);
        Instruction bitExtensionCast = inlineCast(instr, cast, toType, bitExtensionConst);
        Instruction bitExtensionRet;
        switch (cast) {
            case ZERO_EXTEND:
                bitExtensionRet = instr.compare(CompareOperator.INT_NOT_EQUAL, bitExtensionCast, (1L << (fromType.getBitSize() - 1)) | 0x1);
                break;
            case SIGN_EXTEND:
                bitExtensionRet = instr.compare(CompareOperator.INT_NOT_EQUAL, bitExtensionCast, (-1L << (fromType.getBitSize() - 1)) | 0x1);
                break;
            default:
                throw new AssertionError("Unexpected cast!");
        }

        Instruction ret = instr.binaryOperator(BinaryOperator.INT_OR, bitTruncRet, bitExtensionRet);

        instr.returnx(ret); // 0=OK, 1=ERROR
    }

    private static Instruction inlineCast(SimpleInstrunctionBuilder instr, CastOperator cast, Type to, Symbol sym) {
        Constant asm = createInlineAssemblyOperator(cast, sym.getType(), to);
        return instr.call(asm, sym);
    }

    private static Constant createInlineAssemblyOperator(CastOperator cast, Type from, Type to) {

        StringBuilder asmStr = new StringBuilder("mov");
        switch (cast) {
            case ZERO_EXTEND:
                asmStr.append('z');
                break;
            case SIGN_EXTEND:
                asmStr.append('s');
                break;
            default:
                throw new RuntimeException("Unsupported Operator: " + cast);
        }
        asmStr.append(getTypeCharacter(from));
        asmStr.append(getTypeCharacter(to));
        asmStr.append(" $1, $0;");

        PointerType inlineType = new PointerType(new FunctionType(to, new Type[]{from}, false));

        String asmFlags = "=r,r,~{dirflag},~{fpsr},~{flags}"; // TODO: correct flags?

        return ConstantUtil.getInlineAssemblyConstant(inlineType, asmStr.toString(), asmFlags, true, false, AsmDialect.AT_T);
    }

    private static char getTypeCharacter(Type type) {
        if (Type.isIntegerType(type)) {
            switch (type.getBitSize()) {
                case 8:
                    return 'b';
                case 16:
                    return 'w';
                case 32:
                    return 'l';
                case 64:
                    return 'q';
            }
        }
        throw new RuntimeException("Unsupported Type: " + type);
    }

}
