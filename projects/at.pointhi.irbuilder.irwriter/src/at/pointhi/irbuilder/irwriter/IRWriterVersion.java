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

package at.pointhi.irbuilder.irwriter;

import at.pointhi.irbuilder.irwriter.visitors.constants.IRWriterConstantVisitor;
import at.pointhi.irbuilder.irwriter.visitors.constants.IRWriterConstantVisitorV38;
import at.pointhi.irbuilder.irwriter.visitors.function.IRWriterFunctionVisitor;
import at.pointhi.irbuilder.irwriter.visitors.function.IRWriterFunctionVisitorV38;
import at.pointhi.irbuilder.irwriter.visitors.instruction.IRWriterInstructionVisitor;
import at.pointhi.irbuilder.irwriter.visitors.instruction.IRWriterInstructionVisitorV38;
import at.pointhi.irbuilder.irwriter.visitors.model.IRWriterModelVisitor;
import at.pointhi.irbuilder.irwriter.visitors.model.IRWriterModelVisitorV38;
import at.pointhi.irbuilder.irwriter.visitors.type.IRWriterTypeVisitor;
import at.pointhi.irbuilder.irwriter.visitors.type.IRWriterTypeVisitorV38;

public enum IRWriterVersion {
    LLVM_IR_3_2(
                    IRWriterModelVisitor::new,
                    IRWriterFunctionVisitor::new,
                    IRWriterInstructionVisitor::new,
                    IRWriterConstantVisitor::new,
                    IRWriterTypeVisitor::new),

    LLVM_IR_3_8(
                    IRWriterModelVisitorV38::new,
                    IRWriterFunctionVisitorV38::new,
                    IRWriterInstructionVisitorV38::new,
                    IRWriterConstantVisitorV38::new,
                    IRWriterTypeVisitorV38::new);

    @FunctionalInterface
    private interface ModelWriter {
        IRWriterModelVisitor instantiate(IRWriterVisitors visitors, IRWriter.PrintTarget target);
    }

    @FunctionalInterface
    private interface FunctionWriter {
        IRWriterFunctionVisitor instantiate(IRWriterVisitors out, IRWriter.PrintTarget target);
    }

    @FunctionalInterface
    private interface InstructionWriter {
        IRWriterInstructionVisitor instantiate(IRWriterVisitors out, IRWriter.PrintTarget target);
    }

    @FunctionalInterface
    private interface ConstantWriter {
        IRWriterConstantVisitor instantiate(IRWriterVisitors out, IRWriter.PrintTarget target);
    }

    @FunctionalInterface
    private interface TypeWriter {
        IRWriterTypeVisitor instantiate(IRWriterVisitors out, IRWriter.PrintTarget target);
    }

    private final ModelWriter modelVisitor;
    private final FunctionWriter functionVisitor;
    private final InstructionWriter instructionVisitor;
    private final ConstantWriter constantVisitor;
    private final TypeWriter typeVisitor;

    IRWriterVersion(ModelWriter modelVisitor, FunctionWriter functionVisitor, InstructionWriter instructionVisitor, ConstantWriter constantVisitor,
                    TypeWriter typeVisitor) {
        this.modelVisitor = modelVisitor;
        this.functionVisitor = functionVisitor;
        this.instructionVisitor = instructionVisitor;
        this.constantVisitor = constantVisitor;
        this.typeVisitor = typeVisitor;
    }

    private IRWriterModelVisitor createModelPrintVisitor(IRWriterVisitors out, IRWriter.PrintTarget target) {
        return modelVisitor.instantiate(out, target);
    }

    private IRWriterFunctionVisitor createFunctionPrintVisitor(IRWriterVisitors out, IRWriter.PrintTarget target) {
        return functionVisitor.instantiate(out, target);
    }

    private IRWriterInstructionVisitor createInstructionPrintVisitor(IRWriterVisitors out, IRWriter.PrintTarget target) {
        return instructionVisitor.instantiate(out, target);
    }

    private IRWriterConstantVisitor createConstantPrintVisitor(IRWriterVisitors out, IRWriter.PrintTarget target) {
        return constantVisitor.instantiate(out, target);
    }

    private IRWriterTypeVisitor createTypePrintVisitor(IRWriterVisitors out, IRWriter.PrintTarget target) {
        return typeVisitor.instantiate(out, target);
    }

    IRWriterVisitors createIRWriterVisitors(IRWriter.PrintTarget target) {
        return new IRWriterVisitors(this, target);
    }

    public static final class IRWriterVisitors {

        private final IRWriterModelVisitor modelVisitor;
        private final IRWriterFunctionVisitor functionVisitor;
        private final IRWriterInstructionVisitor instructionVisitor;
        private final IRWriterConstantVisitor constantVisitor;
        private final IRWriterTypeVisitor typeVisitor;

        private IRWriterVisitors(IRWriterVersion version, IRWriter.PrintTarget target) {
            this.modelVisitor = version.createModelPrintVisitor(this, target);
            this.functionVisitor = version.createFunctionPrintVisitor(this, target);
            this.instructionVisitor = version.createInstructionPrintVisitor(this, target);
            this.constantVisitor = version.createConstantPrintVisitor(this, target);
            this.typeVisitor = version.createTypePrintVisitor(this, target);
        }

        public IRWriterModelVisitor getModelVisitor() {
            return modelVisitor;
        }

        public IRWriterFunctionVisitor getFunctionVisitor() {
            return functionVisitor;
        }

        public IRWriterInstructionVisitor getInstructionVisitor() {
            return instructionVisitor;
        }

        public IRWriterConstantVisitor getConstantVisitor() {
            return constantVisitor;
        }

        public IRWriterTypeVisitor getTypeVisitor() {
            return typeVisitor;
        }
    }
}
