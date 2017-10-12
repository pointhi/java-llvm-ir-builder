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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.oracle.truffle.llvm.parser.model.ModelModule;

import at.pointhi.irbuilder.irwriter.visitors.model.IRWriterModelVisitor;

public class IRWriter {

    /**
     * This interface is used for actual writing of the LLVM IR representation.
     */
    public interface PrintTarget {
        void print(String s);

        default void println(String s) {
            print(s);
            println();
        }

        void println();
    }

    /**
     * Write the LLVM IR representation of a model into a file.
     *
     * @param model the model which we want to parse
     * @param version actual version of the LLVM IR we want to write
     * @param filename name of file where we want to write the generated LLVM IR
     */
    public static void writeIRToFile(ModelModule model, IRWriterVersion version, String filename) {
        writeIRToFile(model, version, Paths.get(filename));
    }

    /**
     * Write the LLVM IR representation of a model into a file.
     *
     * @param model the model which we want to parse
     * @param version actual version of the LLVM IR we want to write
     * @param file file where we want to write the generated LLVM IR
     */
    public static void writeIRToFile(ModelModule model, IRWriterVersion version, Path file) {
        final PrintWriter fileWriter;
        try {
            fileWriter = new PrintWriter(file.toAbsolutePath().toFile());
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Cannot print LLVMIR to this file: " + file.toAbsolutePath(), e);
        }
        writeIRToStream(model, version, fileWriter); // TODO: Exceptions?
    }

    /**
     * Write the LLVM IR representation of a model into a custom stream.
     *
     * @param model the model which we want to parse
     * @param version actual version of the LLVM IR we want to write
     * @param targetWriter our stream where the actual data is written to
     */
    public static void writeIRToStream(ModelModule model, IRWriterVersion version, PrintWriter targetWriter) {
        writeIR(model, version, new PrintTarget() {
            @Override
            public void print(String s) {
                targetWriter.print(s);
            }

            @Override
            public void println() {
                targetWriter.println();
                targetWriter.flush();
            }
        });
    }

    /**
     * Write the LLVM IR representation of a model into a custom LLVMPrintTarget.
     *
     * @param model the model which we want to parse
     * @param version actual version of the LLVM IR we want to write
     * @param printer our PrintTarget where the actual data is written to
     */
    private static void writeIR(ModelModule model, IRWriterVersion version, PrintTarget printer) {
        final IRWriterVersion.IRWriterVisitors visitors = version.createIRWriterVisitors(printer);
        final IRWriterModelVisitor modelVisitor = visitors.getModelVisitor();

        modelVisitor.writePrologue(model);
        model.accept(modelVisitor);
        modelVisitor.writeEpilogue(model);
    }
}
