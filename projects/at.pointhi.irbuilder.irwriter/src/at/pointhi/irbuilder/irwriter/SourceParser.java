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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.ServiceLoader;

import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.polyglot.Context;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.llvm.BasicConfiguration;
import com.oracle.truffle.llvm.Configuration;
import com.oracle.truffle.llvm.parser.BitcodeParserResult;
import com.oracle.truffle.llvm.parser.model.ModelModule;
import com.oracle.truffle.llvm.parser.scanner.LLVMScanner;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;

@TruffleLanguage.Registration(id = "llvm", name = "llvm", version = "0.01", mimeType = {SourceParser.LLVM_BITCODE_MIME_TYPE, SourceParser.LLVM_BITCODE_BASE64_MIME_TYPE,
                SourceParser.SULONG_LIBRARY_MIME_TYPE})
public class SourceParser extends LLVMLanguage {

    private static final List<Configuration> configurations = new ArrayList<>();

    static {
        configurations.add(new BasicConfiguration());
        for (Configuration f : ServiceLoader.load(Configuration.class)) {
            configurations.add(f);
        }
    }

    @Override
    public LLVMContext findLLVMContext() {
        return getContextReference().get();
    }

    @Override
    protected LLVMContext createContext(Env env) {
        final LLVMContext context = new LLVMContext(env);
        return context;
    }

    @Override
    protected Object findExportedSymbol(LLVMContext context, String globalName, boolean onlyExplicit) {
        return null; // not required
    }

    @Override
    protected Object getLanguageGlobal(LLVMContext context) {
        return context;
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        throw new AssertionError();
    }

    @Override
    protected CallTarget parse(com.oracle.truffle.api.TruffleLanguage.ParsingRequest request) throws Exception {
        Source source = request.getSource();
        try {
            switch (source.getMimeType()) {
                case SourceParser.LLVM_BITCODE_MIME_TYPE:
                case SourceParser.LLVM_BITCODE_BASE64_MIME_TYPE:
                case "x-unknown":

                    ByteBuffer bytes;

                    if (source.getMimeType().equals(LLVMLanguage.LLVM_BITCODE_BASE64_MIME_TYPE)) {
                        ByteBuffer buffer = Charset.forName("ascii").newEncoder().encode(CharBuffer.wrap(source.getCharacters()));
                        bytes = Base64.getDecoder().decode(buffer);
                        assert LLVMScanner.isSupportedFile(bytes);
                    } else if (source.getPath() != null) {
                        try {
                            bytes = ByteBuffer.wrap(Files.readAllBytes(Paths.get(source.getPath())));
                        } catch (IOException ignore) {
                            bytes = ByteBuffer.allocate(0);
                        }
                        assert LLVMScanner.isSupportedFile(bytes);
                    } else {
                        throw new IllegalStateException();
                    }

                    assert bytes != null;

                    // we are only interested in the parsed model
                    final ModelModule model = BitcodeParserResult.getFromSource(source, bytes).getModel();

                    // TODO: add config options to change the behavior of the output function
                    PrintWriter writer = null;

                    try {
                        final String sourceFileName = source.getPath();
                        final String actualTarget = sourceFileName.substring(0, sourceFileName.length() - ".bc".length()) + ".out.ll";
                        writer = new PrintWriter(actualTarget);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Could not open File Stream");
                    }

                    final IRWriterVersion llvmVersion = IRWriterVersion.fromEnviromentVariables();
                    IRWriter.writeIRToStream(model, llvmVersion, writer);

                    // because we are only parsing the file, there is nothing to execute
                    return Truffle.getRuntime().createCallTarget(RootNode.createConstantNode(0));

                default:
                    throw new IllegalArgumentException("unexpected mime type: " + source.getMimeType());
            }
        } catch (Throwable t) {
            throw new IOException("Error while trying to parse " + source.getPath(), t);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Please provide a file which you want to parse");
        }
        final File file = new File(args[0]);
        final String[] otherArgs = new String[args.length - 1];
        System.arraycopy(args, 1, otherArgs, 0, otherArgs.length);

        parseAndOutputFile(file, otherArgs);

        System.exit(0);
    }

    /**
     * Parse a file and output the parser result as LLVM IR.
     *
     * @param file File to parse
     * @throws IOException
     */
    public static void parseAndOutputFile(File file, String[] args) throws IOException {
        org.graalvm.polyglot.Source source = org.graalvm.polyglot.Source.newBuilder(LLVMLanguage.NAME, file).build();
        Context context = Context.newBuilder().arguments(LLVMLanguage.NAME, args).build();

        try {
            context.eval(source);
        } finally {
            context.close();
        }
    }

    @Override
    protected OptionDescriptors getOptionDescriptors() {
        List<OptionDescriptor> optionDescriptors = new ArrayList<>();
        for (Configuration c : configurations) {
            optionDescriptors.addAll(c.getOptionDescriptors());
        }
        return OptionDescriptors.create(optionDescriptors);
    }
}
