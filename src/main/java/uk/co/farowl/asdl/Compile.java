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
     * java -cp ... uk.co.farowl.asdl.Compile filename
     *
     * At present, this just dumps the parse tree.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        InputStream is;
        String inputFile;

        // Open the file named on the command line
        if (args.length > 0) {
            inputFile = args[0];
            is = new FileInputStream(inputFile);
        } else {
            inputFile = "<stdin>";
            is = System.in;
        }

        // Wrap the input in a Lexer
        ANTLRInputStream input = new ANTLRInputStream(is);
        ASDLLexer lexer = new ASDLLexer(input);

        // Parse the token stream with the generated ASDL parser
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASDLParser parser = new ASDLParser(tokens);
        ModuleContext tree = parser.module();
        if (is != System.in) {
            is.close();
        }

        // We now have a parse tree in memory: dump it out.
        // System.out.println(tree.toStringTree(parser));

        // Using a visitor to the parse tree, construct an AST
        ASTBuilderParseVisitor astBuilder = new ASTBuilderParseVisitor();
        AsdlTree.Module module = astBuilder.visitModule(tree);

        // Dump out the AST
        // System.out.println(module.toString());

        // Emit reconstructed source using StringTemplate
        URL url = AsdlTree.class.getResource("ASDL.stg");
        STGroup stg = new STGroupFile(url, "UTF-8", '<', '>');
        ST st = stg.getInstanceOf("emitModule");
        st.add("module", module);
        String source =  st.render();
        System.out.println(source);
    }
}
