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

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runners.Parameterized;

import com.oracle.truffle.llvm.parser.model.ModelModule;
import com.oracle.truffle.llvm.parser.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.runtime.types.ArrayType;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;
import com.oracle.truffle.llvm.test.options.TestOptions;

import at.pointhi.irbuilder.irbuilder.InstructionBuilder;
import at.pointhi.irbuilder.irbuilder.ModelModuleBuilder;
import at.pointhi.irbuilder.irbuilder.SimpleInstrunctionBuilder;
import at.pointhi.irbuilder.irbuilder.helper.LLVMIntrinsics;
import at.pointhi.irbuilder.irbuilder.util.ConstantUtil;
import at.pointhi.irbuilder.irwriter.IRWriter;
import at.pointhi.irbuilder.irwriter.IRWriterVersion;

public class VarArgFunctionCallTest extends BaseSuite {

    private static final Path VAR_ARG_SUITE_DIR = Paths.get(TestOptions.PROJECT_ROOT + "/../cache/tests/irbuilder/vararg");

    @Parameterized.Parameter(value = 0) public Path path;

    @Override
    public Path getSuiteDir() {
        return VAR_ARG_SUITE_DIR;
    }

    @Override
    public Path getFilename() {
        return Paths.get("test_vararg.ll");
    }

    /*
     * This is a workaround, to allow mx unittest to execute this testsuite.
     */
    @Override
    @Test(timeout = 1000)
    public void test() throws Exception {
        super.test();
    }

    @Override
    public ModelModule constructModelModule() {
        ModelModuleBuilder builder = new ModelModuleBuilder();

        StructureType struct = new StructureType(false, new Type[]{PrimitiveType.I64, PrimitiveType.I64, PrimitiveType.I64, PrimitiveType.I64, PrimitiveType.I64});
        struct.setName("struct._asdf");

        builder.createType(struct);

        FunctionDefinition foo = createFoo(builder, struct);
        createMain(builder, foo, struct);

        IRWriter.writeIRToStream(builder.getModelModule(), IRWriterVersion.fromEnviromentVariables(), new PrintWriter(System.out));

        return builder.getModelModule();
    }

    private static FunctionDefinition createFoo(ModelModuleBuilder builder, StructureType struct) {
        /*
         * TODO: http://llvm.org/docs/LangRef.html#i-va-arg
         *
         * It seems llvm now has a specific keyword for that
         */
        // TOOD: 4
        FunctionDefinition foo = builder.createFunctionDefinition("foo", 4, new FunctionType(PrimitiveType.I32, new Type[]{PrimitiveType.I32}, true));
        SimpleInstrunctionBuilder instr = new SimpleInstrunctionBuilder(builder, foo);

        InstructionBlock returnOkBlock = instr.getBlock(1);
        InstructionBlock returnFailBlock = instr.getBlock(2);

        // FunctionParameter length = instr.nextParameter();

        StructureType vaListTag = LLVMIntrinsics.registerVaListTagType(builder);
        // TODO: align should be 16, not 8
        Instruction vaArray = instr.allocate(new ArrayType(vaListTag, 1));

        instr.vaStartAMD64(vaArray);

        Instruction loadRes = instr.vaArgAMD64(vaArray, PrimitiveType.I32);
        Instruction cmpRes = instr.compare(CompareOperator.INT_EQUAL, loadRes, 32);

        instr.insertBlocks(1);
        instr.branch(cmpRes, instr.getNextBlock(), returnFailBlock);
        instr.nextBlock();

        Instruction loadRes2 = instr.vaArgAMD64(vaArray, PrimitiveType.DOUBLE);
        Instruction cmpRes2 = instr.compare(CompareOperator.FP_ORDERED_EQUAL, loadRes2, 1.2);

        instr.insertBlocks(1);
        instr.branch(cmpRes2, instr.getNextBlock(), returnFailBlock);
        instr.nextBlock();

        Instruction loadRes3 = instr.vaArgAMD64StackOnly(vaArray, struct);
        Instruction loadRes3Var = instr.getElementPointer(loadRes3, 0, 1);
        Instruction cmpRes3 = instr.compare(CompareOperator.INT_EQUAL, instr.load(loadRes3Var), 42);

        instr.insertBlocks(1);
        instr.branch(cmpRes3, instr.getNextBlock(), returnFailBlock);
        instr.nextBlock();

        Instruction loadRes4 = instr.vaArgAMD64StackOnly(vaArray, struct);
        Instruction loadRes4Var = instr.getElementPointer(loadRes4, 0, 1);
        Instruction cmpRes4 = instr.compare(CompareOperator.INT_EQUAL, instr.load(loadRes4Var), 42);

        instr.branch(cmpRes4, returnOkBlock, returnFailBlock);

        instr.nextBlock();
        assert instr.getCurrentBlock() == returnOkBlock;

        instr.vaEndAMD64(vaArray);

        instr.returnx(ConstantUtil.getI32Const(0));

        instr.nextBlock();
        assert instr.getCurrentBlock() == returnFailBlock;

        instr.vaEndAMD64(vaArray);

        instr.returnx(ConstantUtil.getI32Const(1));

        return foo;
    }

    private static FunctionDefinition createMain(ModelModuleBuilder builder, FunctionDefinition foo, StructureType struct) {
        FunctionDefinition main = builder.createFunctionDefinition("main", 1, new FunctionType(PrimitiveType.I32, new Type[]{}, true));
        SimpleInstrunctionBuilder instr = new SimpleInstrunctionBuilder(builder, main);

        Instruction structType = instr.allocate(struct);
        Instruction structFirstElement = instr.getElementPointer(structType, 0, 1);
        instr.store(structFirstElement, ConstantUtil.getI32Const(42));

        Instruction res = instr.call(foo, ConstantUtil.getI32Const(1), ConstantUtil.getI32Const(32), ConstantUtil.getDoubleConst(1.2), structType, structType);

        instr.returnx(res); // 0=OK, 1=ERROR

        return main;
    }

}
