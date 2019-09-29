package com.ontotext.trree.plugin.example;

import com.ontotext.test.functional.base.SingleRepositoryFunctionalTest;
import com.ontotext.test.utils.StandardUtils;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the exampleBasic plugin.
 */
public class TestExampleBasicPlugin extends SingleRepositoryFunctionalTest {
    @Override
    protected RepositoryConfig createRepositoryConfiguration() {
        // Creates a repository configuration with the rdfsplus-optimized ruleset
        return StandardUtils.createOwlimSe("rdfsplus-optimized");
    }

    @Test
    public void testExampleBasic() {
        try (RepositoryConnection connection = getRepository().getConnection()) {
            // The predicate is how we request this to be processed by the plugin.
            // We'll get the response as a binding in the object position via the ?time variable.
            // We need a value in the unused subject position so an anonymous blank node is a good fit.
            TupleQuery query = connection.prepareTupleQuery("select ?time { [] <http://example.com/now> ?time }");
            try (TupleQueryResult result = query.evaluate()) {
                assertTrue("Must have at least one row in the result", result.hasNext());
                BindingSet bindings = result.next();
                assertTrue(bindings.hasBinding("time"));
                Value time = bindings.getValue("time");
                assertTrue("Returned value must be a literal", time instanceof Literal);
                long timeInMillisFromQuery = ((Literal) time).calendarValue().toGregorianCalendar().getTimeInMillis();
                long timeInMillisFromSystem = System.currentTimeMillis();
                assertEquals("Time must be a close match", timeInMillisFromSystem, timeInMillisFromQuery, 100);
                assertFalse("There must be a single row in the result", result.hasNext());
            }
        }
    }
}
