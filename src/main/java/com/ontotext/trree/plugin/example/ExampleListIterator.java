package com.ontotext.trree.plugin.example;

import com.ontotext.trree.sdk.Entities;
import com.ontotext.trree.sdk.PluginConnection;
import com.ontotext.trree.sdk.StatementIterator;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A {@link StatementIterator} that is capable on filtering internally based on the subject/object values used.
 */
public class ExampleListIterator extends StatementIterator {
    private static final ValueFactory VF = SimpleValueFactory.getInstance();
    private static final Map<Value, List<Value>> DATA;

    static {
        // Initialize the data as a simple mapping from subject to multiple object values associated
        // with the subject.
        Map<Value, List<Value>> map = new HashMap<>();
        map.put(VF.createIRI("http://example.com/iri1"),
                Arrays.asList(VF.createLiteral("a"), VF.createLiteral("b")));
        map.put(VF.createIRI("http://example.com/iri2"),
                Arrays.asList(VF.createLiteral("a"), VF.createLiteral("c")));
        DATA = Collections.unmodifiableMap(map);
    }

    private final Entities entities;
    private final Iterator<Map.Entry<Value, List<Value>>> outerIter;
    private final long filterObject;
    private Iterator<Value> innerIter;

    ExampleListIterator(long filterSubject, long filterObject, PluginConnection pluginConnection) {
        this.filterObject = filterObject;
        this.entities = pluginConnection.getEntities();

        outerIter = filterCollection(DATA.entrySet(), Map.Entry::getKey, filterSubject);
    }

    @Override
    public boolean next() {
        if (innerIter == null || !innerIter.hasNext()) {
            if (outerIter.hasNext()) {
                Map.Entry<Value, List<Value>> entry = outerIter.next();
                innerIter = filterCollection(entry.getValue(), Function.identity(), filterObject);
                subject = entities.put(entry.getKey(), Entities.Scope.REQUEST);
            } else {
                return false;
            }
        }
        if (innerIter.hasNext()) {
            Value objectValue = innerIter.next();
            object = entities.put(objectValue, Entities.Scope.REQUEST);

            return true;
        }

        return false;
    }

    @Override
    public void close() {
        // nothing to close
    }

    private <T> Iterator<T> filterCollection(Collection<T> collection, Function<T, Value> filterMapper,
            long filterValueId) {
        if (filterValueId == 0) {
            // No filter - return entire collection
            return collection.iterator();
        } else {
            // Return only elements that match
            Value filterString = entities.get(filterValueId);
            return collection.stream()
                    .filter(e -> filterMapper.apply(e).equals(filterString))
                    .iterator();
        }
    }
}
