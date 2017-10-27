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
import com.oracle.truffle.llvm.parser.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;
import com.oracle.truffle.llvm.test.options.TestOptions;

import at.pointhi.irbuilder.irbuilder.ModelModuleBuilder;
import at.pointhi.irbuilder.irbuilder.SimpleInstrunctionBuilder;
import at.pointhi.irbuilder.irbuilder.util.ConstantUtil;
import at.pointhi.irbuilder.testgenerator.util.IntegerBinaryOperations.UndefinedArithmeticResult;

@RunWith(Parameterized.class)
public class FloatCompareOperators extends BaseSuite {

    private static final Path SUITE_DIR = Paths.get(TestOptions.PROJECT_ROOT + "/../cache/tests/irbuilder/FloatCompareOperator");

    private final PrimitiveType type;

    public FloatCompareOperators(PrimitiveType type) {
        this.type = type;
    }

    @Override
    public Path getSuiteDir() {
        return SUITE_DIR;
    }

    @Override
    public Path getFilename() {
        return Paths.get(String.format("cmp_%s.ll", type.toString()));
    }

    @Parameters(name = "{index}: CompareFloat[type={0}]")
    public static Collection<Object[]> data() {
        List<Object[]> parameters = new LinkedList<>();

        // addParameter(parameters, PrimitiveType.HALF);
        addParameter(parameters, PrimitiveType.FLOAT);
        addParameter(parameters, PrimitiveType.DOUBLE);
        addParameter(parameters, PrimitiveType.X86_FP80);
        // addParameter(parameters, PrimitiveType.F128);
        // addParameter(parameters, PrimitiveType.PPC_FP128);

        return parameters;
    }

    private static void addParameter(List<Object[]> parameters, PrimitiveType type) {
        if (!Type.isFloatingpointType(type)) {
            throw new AssertionError("only floating point types are allowed!");
        }
        parameters.add(new Object[]{type});
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

        int errorCode = 1;

        // TODO: check with SNaN as well

        // check if 0 equals -0
        appendCheck(instr, errorCode++, CompareOperator.FP_ORDERED_EQUAL, 0.0, -0.0);
        appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED_EQUAL, 0.0, -0.0);
        appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED_NOT_EQUAL, 0.0, -0.0);
        appendCheckInv(instr, errorCode++, CompareOperator.FP_UNORDERED_NOT_EQUAL, 0.0, -0.0);

        // check if NaN equals NaN
        appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED_EQUAL, Double.NaN, Double.NaN);
        appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED_EQUAL, Double.NaN, Double.NaN);
        appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED_NOT_EQUAL, Double.NaN, Double.NaN);
        appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED_NOT_EQUAL, Double.NaN, Double.NaN);

        // check if 1 equals NaN
        appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED_EQUAL, 1.0, Double.NaN);
        appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED_EQUAL, 1.0, Double.NaN);
        appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED_NOT_EQUAL, 1.0, Double.NaN);
        appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED_NOT_EQUAL, 1.0, Double.NaN);

        // check if NaN equals 1
        appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED_EQUAL, 1.0, Double.NaN);
        appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED_EQUAL, 1.0, Double.NaN);
        appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED_NOT_EQUAL, 1.0, Double.NaN);
        appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED_NOT_EQUAL, 1.0, Double.NaN);

        // check if Positive Infinite equals Positive Infinite
        appendCheck(instr, errorCode++, CompareOperator.FP_ORDERED_EQUAL, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED_EQUAL, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED_NOT_EQUAL, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        appendCheckInv(instr, errorCode++, CompareOperator.FP_UNORDERED_NOT_EQUAL, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

        // check if Negative Infinite equals Negative Infinite
        appendCheck(instr, errorCode++, CompareOperator.FP_ORDERED_EQUAL, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED_EQUAL, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED_NOT_EQUAL, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        appendCheckInv(instr, errorCode++, CompareOperator.FP_UNORDERED_NOT_EQUAL, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

        // check if Positive Infinite equals Negative Infinite
        appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED_EQUAL, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
        appendCheckInv(instr, errorCode++, CompareOperator.FP_UNORDERED_EQUAL, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
        appendCheck(instr, errorCode++, CompareOperator.FP_ORDERED_NOT_EQUAL, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
        appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED_NOT_EQUAL, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);

        // check if 1 greater than NaN
        appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED_GREATER_THAN, 1.0, Double.NaN);
        appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED_GREATER_THAN, 1.0, Double.NaN);

        // check if 1 greater or equal than NaN
        appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED_GREATER_OR_EQUAL, 1.0, Double.NaN);
        appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED_GREATER_OR_EQUAL, 1.0, Double.NaN);

        // check if 1 less than NaN
        appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED_LESS_THAN, 1.0, Double.NaN);
        appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED_LESS_THAN, 1.0, Double.NaN);

        // check if 1 less or equal than NaN
        appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED_LESS_OR_EQUAL, 1.0, Double.NaN);
        appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED_LESS_OR_EQUAL, 1.0, Double.NaN);

        // check if ordered
        appendCheck(instr, errorCode++, CompareOperator.FP_ORDERED, 1.0, 1.0);
        appendCheck(instr, errorCode++, CompareOperator.FP_ORDERED, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
        appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED, 1.0, Double.NaN);
        appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED, Double.NaN, 1.0);
        appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED, Double.NaN, Double.NaN);

        // check if unordered
        appendCheckInv(instr, errorCode++, CompareOperator.FP_UNORDERED, 1.0, 1.0);
        appendCheckInv(instr, errorCode++, CompareOperator.FP_UNORDERED, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
        appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED, 1.0, Double.NaN);
        appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED, Double.NaN, 1.0);
        appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED, Double.NaN, Double.NaN);

        if (PrimitiveType.X86_FP80.equals(type)) {
            // check if ordered
            appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED, ConstantUtil.getConst(type, 1.0), ConstantUtil.X86_FP80_SNaN);
            appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED, ConstantUtil.X86_FP80_SNaN, ConstantUtil.getConst(type, 1.0));
            appendCheckInv(instr, errorCode++, CompareOperator.FP_ORDERED, ConstantUtil.X86_FP80_SNaN, ConstantUtil.X86_FP80_SNaN);

            // check if unordered
            appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED, ConstantUtil.getConst(type, 1.0), ConstantUtil.X86_FP80_SNaN);
            appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED, ConstantUtil.X86_FP80_SNaN, ConstantUtil.getConst(type, 1.0));
            appendCheck(instr, errorCode++, CompareOperator.FP_UNORDERED, ConstantUtil.X86_FP80_SNaN, ConstantUtil.X86_FP80_SNaN);
        }

        // return success
        instr.returnx(ConstantUtil.getI32Const(0));

    }

    private void appendCheck(SimpleInstrunctionBuilder builder, int errorCode, CompareOperator op, double left, double right) {
        appendCheck(builder, errorCode, op, ConstantUtil.getConst(type, left), ConstantUtil.getConst(type, right));
    }

    private static void appendCheck(SimpleInstrunctionBuilder builder, int errorCode, CompareOperator op, Symbol left, Symbol right) {
        Instruction instr = builder.compare(op, left, right);

        builder.insertBlocks(2);
        builder.branch(instr, builder.getBlock(builder.getCurrentBlock().getBlockIndex() + 2), builder.getNextBlock());

        builder.nextBlock();
        builder.returnx(ConstantUtil.getI32Const(errorCode));

        builder.nextBlock();
    }

    private void appendCheckInv(SimpleInstrunctionBuilder builder, int errorCode, CompareOperator op, double left, double right) {
        appendCheckInv(builder, errorCode, op, ConstantUtil.getConst(type, left), ConstantUtil.getConst(type, right));
    }

    private static void appendCheckInv(SimpleInstrunctionBuilder builder, int errorCode, CompareOperator op, Symbol left, Symbol right) {
        Instruction instr = builder.compare(op, left, right);

        builder.insertBlocks(2);
        builder.branch(instr, builder.getNextBlock(), builder.getBlock(builder.getCurrentBlock().getBlockIndex() + 2));

        builder.nextBlock();
        builder.returnx(ConstantUtil.getI32Const(errorCode));

        builder.nextBlock();
    }

}
