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
package at.pointhi.irbuilder.irbuilder.util;

import java.util.Optional;
import java.util.function.Predicate;

import com.oracle.truffle.llvm.parser.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.globals.GlobalConstant;
import com.oracle.truffle.llvm.parser.model.visitors.ModelVisitor;

public abstract class ModelExtractor<T extends Object> implements ModelVisitor {
    private Optional<T> match = Optional.empty();
    protected final Predicate<? super T> predicate;

    private ModelExtractor(Predicate<? super T> predicate) {
        this.predicate = predicate;
    }

    public void onMatch(@SuppressWarnings("unused") T obj) {
    }

    public Optional<T> getMatch() {
        return match;
    }

    protected void onVisit(T obj) {
        if (predicate.test(obj)) {
            if (match.isPresent()) {
                throw new AssertionError("the extractor visitor should only match for one object!");
            }
            match = Optional.of(obj);

            onMatch(obj);
        }
    }

    @Override
    public void ifVisitNotOverwritten(Object obj) {
    }

    public static class FunctionDeclarationExtractor extends ModelExtractor<FunctionDeclaration> {

        public FunctionDeclarationExtractor(Predicate<? super FunctionDeclaration> predicate) {
            super(predicate);
        }

        @Override
        public void visit(FunctionDeclaration function) {
            onVisit(function);
        }
    }

    public static class FunctionDefinitionExtractor extends ModelExtractor<FunctionDefinition> {

        public FunctionDefinitionExtractor(Predicate<? super FunctionDefinition> predicate) {
            super(predicate);
        }

        @Override
        public void visit(FunctionDefinition function) {
            onVisit(function);
        }
    }

    public static class GlobalConstantExtractor extends ModelExtractor<GlobalConstant> {

        public GlobalConstantExtractor(Predicate<? super GlobalConstant> predicate) {
            super(predicate);
        }

        @Override
        public void visit(GlobalConstant constant) {
            onVisit(constant);
        }
    }

}
