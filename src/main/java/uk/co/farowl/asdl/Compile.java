package uk.co.farowl.asdl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
     * Program that may be invoked as:<pre>
     * java -cp ... uk.co.farowl.asdl.Compile filename
     *</pre> At present, this just dumps the parse tree.
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        String inputFile;
        Compile compiler;
        ANTLRInputStream input;

        // Open the file named on the command line
        if (args.length > 0) {
            inputFile = args[0];
            InputStream is = new FileInputStream(inputFile);
            input = new ANTLRInputStream(is);
        } else {
            inputFile = "<stdin>";
            input = new ANTLRInputStream(System.in);
        }
        compiler = new Compile();

        compiler.buildParseTree(input, inputFile);
        compiler.buildAST();

        // Dump out the AST
        System.out.println(compiler.emitASDL());
    }

    String inputName;
    ANTLRInputStream input;
    ModuleContext parseTree;
    AsdlTree ast;

    /**
     * Compile the source (actually an ANTLR input stream) into a new parse tree. This source must
     * represent one complete ASDL module. We require an <code>ANTLRInputStream</code> rather than
     * support several different source types (<code>String</code>, <code>InputStream</code>,
     * <code>Reader</code>, etc.).
     *
     * @param input representing the source module: client may close on return
     * @param inputName name to use in messages
     * @throws IOException from reading the input
     */
    public void buildParseTree(ANTLRInputStream input, String inputName) throws IOException {
        this.input = input;
        this.inputName = (inputName != null) ? inputName : "<input>";
        this.ast = null;

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

    /** Emit reconstructed source from arbitrary sub-tree using StringTemplate */
    static String emitASDL(AsdlTree.Node node) {
        URL url = AsdlTree.class.getResource("ASDL.stg");
        STGroup stg = new STGroupFile(url, "UTF-8", '<', '>');
        ST st = stg.getInstanceOf("emit");
        st.add("node", node);
        return st.render();
    }
}
