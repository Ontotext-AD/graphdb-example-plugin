package com.ontotext.trree.plugin.example;

import com.ontotext.trree.sdk.Entities;
import com.ontotext.trree.sdk.InitReason;
import com.ontotext.trree.sdk.ListPatternInterpreter;
import com.ontotext.trree.sdk.PluginBase;
import com.ontotext.trree.sdk.PluginConnection;
import com.ontotext.trree.sdk.RequestContext;
import com.ontotext.trree.sdk.StatementIterator;
import org.eclipse.collections.api.map.primitive.ImmutableLongObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * This plugin defines the predicate http://example.com/getLabel as a multiple-argument functional interface,
 * where the function's arguments are provided as an RDF list in the object and the function output will be bound
 * in the subject. It takes at least three arguments:
 * <pre>
 *     ?label <http://example.com/getLabel> (?resource ?labelPredicate ?lang1 ...)
 * </pre>
 * <p>
 * Where ?resource is the RDF resource to lookup, ?labelPredicate is the predicate whose object will be used,
 * and ?lang1 and the remaining arguments are language tags provided as literals.
 * <p>
 * The plugin will return all labels that match the first language tag that has at least one label, or if none
 * of the language tags match, all labels that are plain literals (i.e. xsd:string literals).
 * <p>
 * The language matching logic is compatible with the SPARQL langMatches() function.
 * <p>
 * It is trivial to add more functional patterns by implementing the {@link FunctionalPattern} interface and passing
 * the instance to {@link #registerFunctionalPatterns(PluginConnection, FunctionalPattern...)}
 */
public class ExampleFunctionalPlugin extends PluginBase implements ListPatternInterpreter {
    private ImmutableLongObjectMap<FunctionalPattern> functionalPatternMap;

    @Override
    public String getName() {
        return "exampleFunctional";
    }

    @Override
    public void initialize(InitReason reason, PluginConnection pluginConnection) {
        // Register the getLabel functional pattern
        functionalPatternMap = registerFunctionalPatterns(pluginConnection, new GetLabelFunctionalPattern());

        getLogger().info("ExampleFunctional plugin initialized!");
    }

    private ImmutableLongObjectMap<FunctionalPattern> registerFunctionalPatterns(PluginConnection pluginConnection,
            FunctionalPattern... functionalPatterns) {
        LongObjectHashMap<FunctionalPattern> map = new LongObjectHashMap<>();
        for (FunctionalPattern functionalPattern : functionalPatterns) {
            long predicateId = pluginConnection.getEntities()
                    .put(SimpleValueFactory.getInstance().createIRI(functionalPattern.getIRI()),
                            Entities.Scope.SYSTEM);
            map.put(predicateId, functionalPattern);
        }
        return map.toImmutable();
    }

    @Override
    public double estimate(long subject, long predicate, long[] objects, long context,
            PluginConnection pluginConnection, RequestContext requestContext) {
        // No need to check the predicate if all of the predicates in this plugin follow the same logic
        for (long object : objects) {
            if (object == Entities.UNBOUND) { // (the constant Entities.UNBOUND is actually zero)
                // Since this plugin receives objects converted from an RDF list it can't bind an object,
                // so it can function only if all members of the RDF list are bound.
                // By returning a very large number we ensure the optimizer won't choose a plan with unbound objects.
                return Double.POSITIVE_INFINITY;
            }
        }

        return 1;
    }

    @Override
    public StatementIterator interpret(long subject, long predicate, long[] objects, long context,
            PluginConnection pluginConnection, RequestContext requestContext) {
        FunctionalPattern functionalPattern = functionalPatternMap.get(predicate);
        if (functionalPattern != null) {
            functionalPattern.verifyNumberOfArguments(objects.length);
            for (long object : objects) {
                // See note in estimate() method. If we do get evaluated with unbound objects simply return
                // an empty iterator (and besides we must return an iterator to signal we want to handle this pattern)
                if (object == 0) {
                    return StatementIterator.EMPTY;
                }
            }

            return functionalPattern.evaluate(objects, pluginConnection);
        }

        // Not interested in handling this triple pattern
        return null;
    }
}
