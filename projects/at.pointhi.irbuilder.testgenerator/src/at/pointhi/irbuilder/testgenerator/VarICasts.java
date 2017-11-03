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
import com.oracle.truffle.llvm.parser.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.parser.model.enums.CastOperator;
import com.oracle.truffle.llvm.parser.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VariableBitWidthType;
import com.oracle.truffle.llvm.test.options.TestOptions;

import at.pointhi.irbuilder.irbuilder.ModelModuleBuilder;
import at.pointhi.irbuilder.irbuilder.SimpleInstrunctionBuilder;
import at.pointhi.irbuilder.testgenerator.util.IntegerBinaryOperations.UndefinedArithmeticResult;

@RunWith(Parameterized.class)
public class VarICasts extends BaseSuite {

    private static final Path SUITE_DIR = Paths.get(TestOptions.PROJECT_ROOT + "/../cache/tests/irbuilder/VarICasts");

    private final VariableBitWidthType type1;
    private final VariableBitWidthType type2;
    private final CastOperator cast;

    public VarICasts(int type1Bits, int type2Bits, CastOperator cast) {
        this.type1 = new VariableBitWidthType(type1Bits);
        this.type2 = new VariableBitWidthType(type2Bits);
        this.cast = cast;
    }

    @Override
    public Path getSuiteDir() {
        return SUITE_DIR;
    }

    @Override
    public Path getFilename() {
        return Paths.get(String.format("cast_i%d_i%d_(%s).ll", type1.getBitSize(), type2.getBitSize(), cast.getIrString()));
    }

    @Parameters(name = "{index}: VarICasts[type1=i{0}, type2=i{1}, cast={2}]")
    public static Collection<Object[]> data() {
        List<Object[]> parameters = new LinkedList<>();

        for (int i = 8; i <= Long.SIZE; i += 4) {
            for (int j = 4; j < i; j += 4) {
                addParameter(parameters, i, j, CastOperator.ZERO_EXTEND);
                addParameter(parameters, i, j, CastOperator.SIGN_EXTEND);
                addParameter(parameters, i - 1, j - 1, CastOperator.ZERO_EXTEND);
                addParameter(parameters, i - 1, j - 1, CastOperator.SIGN_EXTEND);
            }
        }

        return parameters;
    }

    private static void addParameter(List<Object[]> parameters, int type1Bits, int type2Bits, CastOperator cast) {
        parameters.add(new Object[]{type1Bits, type2Bits, cast});
    }

    @Override
    public ModelModule constructModelModule() throws UndefinedArithmeticResult {
        ModelModuleBuilder builder = new ModelModuleBuilder();
        createMain(builder);

        return builder.getModelModule();
    }

    private void createMain(ModelModuleBuilder builder) {
        FunctionDefinition main = builder.createFunctionDefinition("main", 1, new FunctionType(PrimitiveType.I32, new Type[]{}, false));
        SimpleInstrunctionBuilder instr = new SimpleInstrunctionBuilder(builder, main);

        // check if bit's are stored in a truncated way
        IntegerConstant bitTruncConst = new IntegerConstant(type1, 1L << type2.getBitSize());
        Instruction bitTruncatInstr = instr.cast(CastOperator.TRUNCATE, type2, bitTruncConst);
        Instruction bitTruncCast = instr.cast(cast, type1, bitTruncatInstr);
        Instruction bitTruncRet = instr.compare(CompareOperator.INT_NOT_EQUAL, bitTruncCast, 0);

        // check if zero extension works as expected
        IntegerConstant bitExtensionConst = new IntegerConstant(type2, (1L << (type2.getBitSize() - 1)) | 0x1);
        Instruction bitExtensionCast = instr.cast(cast, type1, bitExtensionConst);
        Instruction bitExtensionRet;
        switch (cast) {
            case ZERO_EXTEND:
                bitExtensionRet = instr.compare(CompareOperator.INT_NOT_EQUAL, bitExtensionCast, (1L << (type2.getBitSize() - 1)) | 0x1);
                break;
            case SIGN_EXTEND:
                bitExtensionRet = instr.compare(CompareOperator.INT_NOT_EQUAL, bitExtensionCast, (-1L << (type2.getBitSize() - 1)) | 0x1);
                break;
            default:
                throw new AssertionError("Unexpected cast!");
        }

        Instruction ret = instr.binaryOperator(BinaryOperator.INT_OR, bitTruncRet, bitExtensionRet);

        instr.returnxWithCast(ret); // 0=OK, 1=ERROR
    }

}
