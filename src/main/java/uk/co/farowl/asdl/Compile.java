package uk.co.farowl.asdl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import uk.co.farowl.asdl.ASDLParser.ModuleContext;
import uk.co.farowl.asdl.ast.AsdlTree;

/**
 * A compiler for ASDL that may be invoked at at the command prompt.
 */
public class Compile {

    // TODO JUnit tests

    /**
     * Program that may be invoked as:
     *
     * <pre>
     * java -cp ... uk.co.farowl.asdl.Compile filename
     *</pre>
     *
     * At present, this just dumps the parse tree.
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // Parse the command line
        Compile.Options options = new Compile.Options(args);

        // Help if asked (or if there was an error).
        if (options.commandLineError != null) {
            options.giveHelp();
            System.err.println(options.commandLineError);
            System.exit(1);
        }

        else if (options.giveHelp) {
            options.giveHelp();

        } else {
            // Options ok apparently. Let's get on with it.
            InputStream inputStream = new FileInputStream(options.inputName);
            ANTLRInputStream input = new ANTLRInputStream(inputStream);

            Compile compiler = new Compile(options, input);
            compiler.buildParseTree();
            inputStream.close();

            compiler.buildAST();
            if (options.dumpASDL) {
                System.out.println(compiler.emitASDL());
            }

            // Output generated code as specified (possibly to stdout)
            PrintStream outputStream = System.out;
            if (options.outputName != null) {
                outputStream = new PrintStream(options.outputName);
            }
            if (options.outputType == OutputType.JAVA) {
                outputStream.println(compiler.emitJava());
            }
            if (outputStream != System.out) {
                outputStream.close();
            }
        }
    }

    private enum OutputType {
        NONE, JAVA
    }

    private static class Options {

        /** If not null, there was an error and this is the description. */
        String commandLineError;
        boolean giveHelp;
        String inputName;
        OutputType outputType = OutputType.NONE;
        private boolean dumpASDL;
        String outputName;

        /** Construct from command-line arguments. */
        Options(String[] args) {
            parseCommand(args);
        }

        /**
         * Parse command arguments to local variables.
         *
         * @return true iff the program should run the compiler. (False=give help instead.)
         */
        private void parseCommand(String[] args) {
            int files = 0;
            argloop : for (String arg : args) {
                if (arg.length() >= 2 && arg.charAt(0) == '-') {
                    // It's a switch
                    switch (arg) {
                        case "-h":
                            giveHelp = true;
                            break;
                        case "-a":
                            dumpASDL = true;
                            break;
                        case "-j":
                            outputType = OutputType.JAVA;
                            break;
                        default:
                            setError("Unknown option: " + arg);
                            break argloop;
                    }
                } else {
                    // It's a file
                    switch (files++) {
                        case 0:
                            inputName = arg;
                            break;
                        case 1:
                            outputName = arg;
                            break;
                        default:
                            setError("Spurious file name: " + arg);
                            break argloop;
                    }
                }
            }

            // Consistency checks
            if (outputType == OutputType.NONE && outputName != null) {
                setError("Cannot specify <outfile> when not generating code");
            }

            if (outputType == OutputType.NONE && !giveHelp && !dumpASDL) {
                setError("No actions specified");
            }

            if (inputName == null && !giveHelp) {
                setError("Must specify <infile> when generating code");
            }

            // If there was an error, give help unasked.
            giveHelp |= error();
        }

        /** Declare there was an error, but do not overwrite existing error. */
        void setError(String msg) {
            if (!error() && msg != null) {
                commandLineError = msg;
            }
        }

        /** True iff an error has been declared. */
        boolean error() {
            return commandLineError != null;
        }

        void giveHelp() {
            System.out.println("Arguments:");
            System.out.println(" ...    -ahj <infile> <outfile>");
            System.out.println("-a  Output ASDL equivalent to <infile> to stdout");
            System.out.println("-h  Output this help and stop");
            System.out.println("-j  Output Java to <outfile>");
        }
    }

    Options options;
    ANTLRInputStream input;
    ModuleContext parseTree;
    AsdlTree ast;

    /**
     * Create a compiler attached to the given source stream. This source must represent one
     * complete ASDL module. We require an <code>ANTLRInputStream</code> rather than support several
     * different source types (<code>String</code>, <code>InputStream</code>, <code>Reader</code>,
     * etc.).
     *
     * @param options specifying input name etc.
     * @param input representing the source module
     */
    public Compile(Options options, ANTLRInputStream input) {
        this.options = options;
        this.input = input;
    }

    /**
     * Compile the source (actually an ANTLR input stream) into a new parse tree.
     *
     * @throws IOException from reading the input
     */
    public void buildParseTree() throws IOException {

        // Wrap the input in a Lexer
        ASDLLexer lexer = new ASDLLexer(input);

        // Parse the token stream with the generated ASDL parser
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASDLParser parser = new ASDLParser(tokens);
        parseTree = parser.module();

        // System.out.println(parseTree.toStringTree(parser));
    }

    /**
     * Build an AST from the source already parsed by
     * {@link #buildParseTree(ANTLRInputStream, String)}.
     */
    public void buildAST() {
        if (parseTree == null) {
            throw new java.lang.IllegalStateException("No source has been parsed");
        }
        ast = new AsdlTree(parseTree);
        // System.out.println(ast.root.toString());
    }

    /** Emit reconstructed source from enclosed AST using StringTemplate */
    public String emitASDL() {
        return emitASDL(ast.root);
    }

    /** Emit Java from enclosed AST using StringTemplate */
    public String emitJava() {
        URL url = AsdlTree.class.getResource("Java.stg");
        STGroup stg = new STGroupFile(url, "UTF-8", '<', '>');
        ST st = stg.getInstanceOf("ASDFile");
        String toolName = getClass().getSimpleName();
        st.addAggr("command.{tool, file}", toolName, options.inputName);
        st.add("mod", ast.root);
        return st.render();
    }

    /** Emit reconstructed source from arbitrary sub-tree using StringTemplate */
    static String emitASDL(AsdlTree.Node node) {
        URL url = AsdlTree.class.getResource("ASDL.stg");
        STGroup stg = new STGroupFile(url, "UTF-8", '<', '>');
        ST st = stg.getInstanceOf("emit");
        st.add("node", node);
        return st.render();
    }
}
