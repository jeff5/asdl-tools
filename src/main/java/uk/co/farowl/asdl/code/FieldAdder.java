package uk.co.farowl.asdl.code;

import uk.co.farowl.asdl.ast.AsdlTree;
import uk.co.farowl.asdl.code.CodeTree.Definition;
import uk.co.farowl.asdl.code.CodeTree.Field;
import uk.co.farowl.asdl.code.CodeTree.Module;
import uk.co.farowl.asdl.code.CodeTree.Node;

/**
 * Base class for visitors on the ASDL AST that add {@link CodeTree.Field}s to the partial
 * {@link CodeTree.Module}, produced by a {@link DefinitionBuilder}. When creating
 * {@link CodeTree.Field}s this visitor resolves the names in the visited {@link AsdlTree.Field}
 * entries against the definitions, treating it as a symbol table for type names.
 */
abstract class FieldAdder implements AsdlTree.Visitor<Node> {

    /** Module within which to resolve type names. */
    protected final Module module;

    protected FieldAdder(Module module) {
        this.module = module;
    }

    /** Traverse the definitions (provided to the constructor) and add the fields from the AST. */
    abstract void addFields();

    @Override
    public Module visitModule(AsdlTree.Module module) {
        return null;            // Never visited
    }

    @Override
    public Field visitField(AsdlTree.Field field) {
        Definition type = module.scope.definition(field.typeName);
        Field f = new Field(type, field.cardinality, field.name);
        return f;
    }
}
