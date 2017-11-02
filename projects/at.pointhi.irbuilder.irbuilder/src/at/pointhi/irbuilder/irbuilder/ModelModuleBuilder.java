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
package at.pointhi.irbuilder.irbuilder;

import com.oracle.truffle.llvm.parser.model.ModelModule;
import com.oracle.truffle.llvm.parser.model.attributes.AttributesCodeEntry;
import com.oracle.truffle.llvm.parser.model.enums.Linkage;
import com.oracle.truffle.llvm.parser.model.enums.Visibility;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.symbols.Symbols;
import com.oracle.truffle.llvm.parser.model.symbols.constants.StringConstant;
import com.oracle.truffle.llvm.parser.model.symbols.globals.GlobalConstant;
import com.oracle.truffle.llvm.runtime.types.ArrayType;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.LLVMIdentifier;
import com.oracle.truffle.llvm.runtime.types.symbols.ValueSymbol;

public class ModelModuleBuilder {
    private final ModelModule model;

    public ModelModuleBuilder() {
        this(new ModelModule());
    }

    public ModelModuleBuilder(ModelModule model) {
        this.model = model;
    }

    public ModelModule getModelModule() {
        return model;
    }

    public FunctionDefinition createFunctionDefinition(String name, int blocks, FunctionType type) {
        String globalName = LLVMIdentifier.toGlobalIdentifier(name);
        FunctionDefinition definition = new FunctionDefinition(model, type, globalName, Linkage.EXTERNAL, AttributesCodeEntry.EMPTY);
        definition.allocateBlocks(blocks);

        model.addFunctionDefinition(definition);

        return definition;
    }

    public FunctionDeclaration createFunctionDeclaration(String name, FunctionType type) {
        String globalName = LLVMIdentifier.toGlobalIdentifier(name);
        FunctionDeclaration declaration = new FunctionDeclaration(type, Linkage.EXTERNAL, AttributesCodeEntry.EMPTY);
        declaration.setName(globalName);

        model.addFunctionDeclaration(declaration);

        return declaration;
    }

    public ValueSymbol createGlobalConstant(String name, Type type, int valueIdx) {
        String globalName = LLVMIdentifier.toGlobalIdentifier(name);
        GlobalConstant global = GlobalConstant.create(type, valueIdx, 0, Linkage.INTERNAL.ordinal(), Visibility.DEFAULT.ordinal());
        global.setName(globalName);

        model.addGlobalSymbol(global);
        model.exitModule();

        return global;
    }

    public ValueSymbol createGlobalStringConstant(String name, String value) {
        Type strType = new PointerType(new ArrayType(PrimitiveType.I8, value.length()));

        Symbols symbols = model.getSymbols();
        symbols.addSymbol(new StringConstant(strType, value, false));

        return createGlobalConstant(name, strType, symbols.getSize());
    }

    public void createType(Type type) {
        model.addGlobalType(type);
    }
}
