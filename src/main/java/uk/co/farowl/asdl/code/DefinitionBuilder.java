package uk.co.farowl.asdl.code;

import java.util.HashMap;
import java.util.Map;

import uk.co.farowl.asdl.ast.AsdlTree;
import uk.co.farowl.asdl.code.CodeTree.Definition;
import uk.co.farowl.asdl.code.CodeTree.Module;
import uk.co.farowl.asdl.code.CodeTree.Product;
import uk.co.farowl.asdl.code.CodeTree.Sum;

/**
 * This class is a visitor on the ASDL AST, that generates a partial {@link CodeTree.Module},
 * containing skeletal definitions: the {@link CodeTree.Definition} nodes do not have their
 * {@link CodeTree.Field}s. This set of definitions forms a symbol table against which the named in
 * {@link AsdlTree.Field} entries may be resolved, when creating {@link CodeTree.Field}s. An
 * instance of the class represents the first pass in generating the <code>CodeTree</code> for one
 * ASDL module.
 */
class DefinitionBuilder implements AsdlTree.Visitor<Definition> {

    /** Collect the <code>Sum</code> (AST and code) objects to revisit in the next phase. */
    final Map<AsdlTree.Sum, Sum> sums = new HashMap<>();
    /** Collect the <code>Product</code> (AST and code) objects to revisit in the next phase. */
    final Map<AsdlTree.Product, Product> products = new HashMap<>();
    /** The module to which this visitor is adding definitions. */
    private Module module;
    /** The {@link Scope} within which this <code>DefinitionBuilder</code> creates types. */
    private final Scope<Definition> enclosingScope;

    public DefinitionBuilder(Scope<Definition> enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    /**
     * Return a <code>Module</code> with its full set of definitions, but these definitions are
     * incomplete since they lack the <code>Field</code> entries. This call implements one pass in
     * generating code from the ASDL AST. It visits all the definitions and adds them to the module.
     * It also creates separate lists {@link #sums} and {@link #products} that will drive the next
     * phase of code generation.
     */
    public Module buildModule(AsdlTree.Module module) {
        // Construct an empty module
        this.module = new Module(module.name, new Scope<>(enclosingScope));
        // Now add each of the definitions from the abstract syntax tree
        for (AsdlTree.Definition d : module.defs) {
            // Use accept() because we don't know the concrete type
            this.module.defs.add(d.accept(this));
        }
        return this.module;
    }

    /**
     * Visit an AST Sum and create a {@link CodeTree.Sum} definition in the module symbol table. Add
     * the new Sum to {@link #sums} for later processing.
     */
    @Override
    public Sum visitSum(AsdlTree.Sum sum) {
        String name = sum.name;
        Sum newSum = new Sum(name, module, sum.constructors.size(), sum.attributes.size());
        module.scope.define(name, newSum);
        sums.put(sum, newSum);
        return newSum;
    }

    /**
     * Visit an AST Product and create a {@link CodeTree.Product} definition in the module symbol
     * table. Add the new Sum to {@link #products} for later processing.
     */
    @Override
    public Product visitProduct(AsdlTree.Product prod) {
        String name = prod.name;
        Product newProduct = new Product(name, module, prod.members.size(), prod.attributes.size());
        module.scope.define(name, newProduct);
        products.put(prod, newProduct);
        return newProduct;
    }

    @Override
    public Definition visitConstructor(AsdlTree.Constructor constructor) {
        return null;            // Never visited
    }

    @Override
    public Definition visitField(AsdlTree.Field field) {
        return null;            // Never visited
    }

    @Override
    public Definition visitModule(AsdlTree.Module module) {
        return null;            // Never visited
    }
}
