package uk.co.farowl.asdl.ast;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;

import uk.co.farowl.asdl.ASDLParser;
import uk.co.farowl.asdl.ASTBuilderParseVisitor;

/**
 * This class defines the nodes in abstract syntax trees that represent the (partially) compiled
 * form of ASDL source. Any such node may be thought of as a sub-tree, since an instance cannot exist
 * without its complement of child nodes. From these node classes the AST representing an instance
 * of a specification in ASDL may be composed. These classes support traversal of the AST by objects
 * that implement AdslTree#{@link Visitor}.
 * <p>
 * ASDL is a language for describing ASTs and trees composed of these nodes are part of its
 * compiler. Do not confuse these AST node classes with the ones the compiler generates from the
 * ASDL source.
 */
public class AsdlTree {

    /** The root node of the AST. */
    public final Node root;

    public AsdlTree(ParserRuleContext ctx){
        // Using a visitor to the parse tree, construct an AST
        ASTBuilderParseVisitor astBuilder = new ASTBuilderParseVisitor();
        root = ctx.accept(astBuilder);
    }

    public AsdlTree(ASDLParser.ModuleContext module){
        // Using a visitor to the parse tree, construct an AST
        ASTBuilderParseVisitor astBuilder = new ASTBuilderParseVisitor();
        root = astBuilder.visitModule(module);
    }

    @Override
    public String toString() {
        return root.toString();
    }

    /** All the nodes of the ASDL AST implement this interface. */
    public interface Node {

        /**
         * Call the "visit" method on the visitor that is specific to this node's type. (See the
         * <code>Visitor</code> interface.) The visitor could call its own type-appropriate visit
         * method directly. In the case that the caller does not know the concrete type of <Node>,
         * <code>accept</code> is a useful way of dispatching to a method that depends on the actual
         * types of both the node and the visitor.
         *
         * @param visitor
         * @return
         */
        abstract <T> T accept(Visitor<T> visitor);

        default Collection<Node> children() {
            return Collections.emptyList();
        }
    }

    public static class Module implements Node {

        public final String name;
        public final List<Definition> defs;

        public Module(String name, List<Definition> defs) {
            this.name = name;
            this.defs = Collections.unmodifiableList(defs);
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visitModule(this);
        }

        @Override
        public String toString() {
            return String.format("module %s %s", name, defs);
        }

    }

    /**
     * Class representing one definition, which may be a sum or product type. The facility to
     * specify attributes on a product type seems to be a Python addition, used as a notational
     * convenience. In EBNF:
     * <pre>
     * definition : TypeId '=' type ;
     * type : product | sum ;
     * product : fields attributes? ;
     * sum : constructor ( '|' constructor )* attributes? ;
     * constructor : ConstructorId fields? ;
     * attributes : Attributes fields ;
     * </pre>
     */
    public static abstract class Definition implements Node {

        public final String name;
        public final List<Field> attributes;

        public Definition(String name, List<Field> attributes) {
            this.name = name;
            this.attributes = Collections.unmodifiableList(attributes);
        }

        public boolean isSum() {
            return this instanceof Sum;
        }
    }

    /**
     * Class representing one sum-type definition. In EBNF:
     * <pre>
     * sum : constructor ( '|' constructor )* attributes? ;
     * constructor : ConstructorId fields? ;
     * attributes : Attributes fields ;
     * </pre>
     */
    public static class Sum extends Definition {

        public final List<Constructor> constructors;

        public Sum(String name, List<Constructor> constructors, List<Field> attributes) {
            super(name, attributes);
            this.constructors = Collections.unmodifiableList(constructors);
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visitSum(this);
        }

        /** A sum is simple if it has no members or attributes. */
        public boolean isSimple() {
            if (attributes == null || !attributes.isEmpty()) {
                return false;
            }
            for (Constructor c : constructors) {
                if (!c.members.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            String fmt = "%s = Sum(%s, attr=%s)";
            return String.format(fmt, name, constructors, attributes);
        }
    }

    /**
     * Class representing one product-type definition. The facility to
     * specify attributes on a product type seems to be a Python addition, used as a notational
     * convenience. In EBNF:
     * <pre>
     * product : fields attributes? ;
     * constructor : ConstructorId fields? ;
     * attributes : Attributes fields ;
     * </pre>
     */
    public static class Product extends Definition {

        public final List<Field> members;

        public Product(String name, List<Field> members, List<Field> attributes) {
            super(name, attributes);
            this.members = Collections.unmodifiableList(members);
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visitProduct(this);
        }

        @Override
        public String toString() {
            String fmt = "%s = Product(%s, attr=%s)";
            return String.format(fmt, name, members, attributes);
        }
    }

    public static class Constructor implements Node {

        public final String name;
        public final List<Field> members;

        public Constructor(String name, List<Field> members) {
            this.name = name;
            this.members = Collections.unmodifiableList(members);
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visitConstructor(this);
        }

        @Override
        public String toString() {
            return name + members;
        }
    }

    public static class Field implements Node {

        public enum Cardinality {
            SINGLE(""), OPTIONAL("?"), SEQUENCE("*");

            public final String marker;

            private Cardinality(String marker) {
                this.marker = marker;
            }
        };

        public final String typeName;
        public final Cardinality cardinality;
        public final String name;

        public Field(String typeName, Cardinality cardinality, String name) {
            this.typeName = typeName;
            this.cardinality = cardinality;
            this.name = name;
        }

        public final boolean isOptional() {
            return cardinality == Cardinality.OPTIONAL;
        }

        public final boolean isSequence() {
            return cardinality == Cardinality.SEQUENCE;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visitField(this);
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer(typeName);
            sb.append(cardinality.marker);
            if (name != null) {
                sb.append(' ').append(name);
            }
            return sb.toString();
        }
    }

    /**
     * There is a method in the <code>Visitor</code> interface for each <em>concrete</em> type of
     * {@link AsdlTree.Node}.
     */
    public interface Visitor<T> {

        T visitModule(Module module);

        T visitSum(Sum sum);

        T visitProduct(Product product);

        T visitConstructor(Constructor constructor);

        T visitField(Field field);
    }
}