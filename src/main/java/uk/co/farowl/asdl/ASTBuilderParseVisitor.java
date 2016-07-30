package uk.co.farowl.asdl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.antlr.v4.runtime.Token;

import uk.co.farowl.asdl.ASDLParser.*;
import uk.co.farowl.asdl.ast.AsdlTree;
import uk.co.farowl.asdl.ast.AsdlTree.*;
import uk.co.farowl.asdl.ast.AsdlTree.Field.Cardinality;

/**
 * Visitor to the ASDL parse tree that builds and AST representing the user's ASDL. The classes of
 * the parse tree, and the parser itself, were generated from the grammar of ASDL by ANTLR. The AST
 * we generate here (consisting of {@link AsdlTree} nodes) encapsulates the meaning of the user's
 * source, and will be used to generate source code implementing the data structures described by
 * the user's source.
 */
public class ASTBuilderParseVisitor extends ASDLBaseVisitor<AsdlTree.Node> {

    @Override
    public Module visitModule(ModuleContext ctx) {
        List<Definition> defs = new LinkedList<>();
        for (DefinitionContext def : ctx.definition()) {
            defs.add(visitDefinition(def));
        }
        return new Module(ctx.id().getText(), defs);
    }

    /**
     * <pre>
     * definition : TypeId '=' type ;
     * type : product | sum ;
     * product : fields attributes? ;
     * attributes : Attributes fields ;
     * fields : '(' ( field ',' )* field ')' ;
     * sum : constructor ( '|' constructor )* attributes? ;
     * constructor : ConstructorId fields? ;
     * </pre>
     */
    @Override
    public Definition visitDefinition(DefinitionContext ctx) {
        List<Constructor> constructors;
        TypeContext typeContext = ctx.type();
        List<Field> attributes;
        ProductContext prod = typeContext.product();
        if (prod != null) {
            // Product type: represent as a single constructor node
            List<Field> members = getFieldList(prod.fields());
            Constructor c = new Constructor("", members);
            constructors = Collections.singletonList(c);
            // Check for attributes on this product
            attributes = getFieldList(prod.attributes());
            return new Product(ctx.TypeId().getText(), members, attributes);
        } else {
            // Sum type: there will be a list of constructors
            constructors = new LinkedList<>();
            SumContext sum = typeContext.sum();
            for (ConstructorContext cctx : sum.constructor()) {
                Constructor c = visitConstructor(cctx);
                constructors.add(c);
            }
            // If there is a list of attributes, visit them as Field nodes
            attributes = getFieldList(sum.attributes());
            return new Sum(ctx.TypeId().getText(), constructors, attributes);
        }
    }

    /** constructor : ConstructorId fields? ; */
    @Override
    public Constructor visitConstructor(ConstructorContext ctx) {
        String name = ctx.ConstructorId().getText();
        List<Field> members = getFieldList(ctx.fields());
        return new Constructor(name, members);
    }

    /** Visit the fields of the given AttributesContext and list them. */
    private List<Field> getFieldList(AttributesContext ctx) {
        return getFieldList(ctx == null ? null : ctx.fields());
    }

    /** Visit the fields of the given FieldsContext and list them. */
    private List<Field> getFieldList(FieldsContext ctx) {
        List<Field> members;
        if (ctx == null) {
            members = Collections.emptyList();
        } else {
            members = new LinkedList<>();
            for (FieldContext fctx : ctx.field()) {
                members.add(visitField(fctx));
            }
        }
        return members;
    }

    /** field : TypeId cardinality=('?' | '*')? id? ; */
    @Override
    public Field visitField(FieldContext ctx) {
        String typeName = ctx.TypeId().getText();
        Cardinality cardinality = getCardinality(ctx);
        IdContext id = ctx.id();
        String name = (id == null) ? null : id.getText();
        return new Field(typeName, cardinality, name);
    }

    /** Extract the cardinality of the given field as an enum type. */
    private static Cardinality getCardinality(FieldContext ctx) {
        Token c = ctx.cardinality;
        if (c == null) {
            return Cardinality.SINGLE;
        } else if (c.getType() == ASDLLexer.Question) {
            return Cardinality.OPTIONAL;
        } else {
            return Cardinality.SEQUENCE;
        }
    }
}

//
//
// module : Module id '{' definition* '}' ;
// definition : TypeId '=' type ;
// type : product | sum ;
// product : fields attributes? ;
// sum : constructor ( '|' constructor )* attributes? ;
// constructor : ConstructorId fields? ;
// attributes : Attributes fields ;
// fields : '(' ( field ',' )* field ')' ;
// field : TypeId cardinality=('?' | '*')? id? ;
//