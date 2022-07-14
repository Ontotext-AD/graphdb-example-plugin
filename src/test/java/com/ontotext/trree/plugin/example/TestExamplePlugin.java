package com.ontotext.trree.plugin.example;

import com.ontotext.graphdb.Config;
import com.ontotext.test.TemporaryLocalFolder;
import com.ontotext.test.functional.base.SingleRepositoryFunctionalTest;
import com.ontotext.test.utils.StandardUtils;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the example plugin.
 */
public class TestExamplePlugin extends SingleRepositoryFunctionalTest {

    @ClassRule
    public static TemporaryLocalFolder tmpFolder = new TemporaryLocalFolder();

    @Override
    protected RepositoryConfig createRepositoryConfiguration() {
        // Creates a repository configuration with the rdfsplus-optimized ruleset
        return StandardUtils.createOwlimSe("rdfsplus-optimized");
    }
    @BeforeClass
    public static void setWorkDir() {
        System.setProperty("graphdb.home.work", String.valueOf(tmpFolder.getRoot()));
        Config.reset();
    }

    @AfterClass
    public static void resetWorkDir() {
        System.clearProperty("graphdb.home.work");
        Config.reset();
    }

    @Test
    public void testExample() {
        try (RepositoryConnection connection = getRepository().getConnection()) {
            // The 'from <http://example.com/time>' is how we request this to be processed by the plugin.
            TupleQuery query = connection.prepareTupleQuery("select * from <http://example.com/time> { ?s ?p ?o }");
            assertQueryWithTimeOffset(query, 0);
            setPluginTimeOffset(10);
            assertQueryWithTimeOffset(query, 10);
            setPluginTimeOffset(-10);
            assertQueryWithTimeOffset(query, 0);
        }
    }

    private void assertQueryWithTimeOffset(TupleQuery query, int numHours) {
        // Expected time adjusted for offset (hours converted to milliseconds)
        long timeInMillisFromSystem = System.currentTimeMillis() + numHours * 3_600_000;
        try (TupleQueryResult result = query.evaluate()) {
            assertTrue("Must have at least one row in the result", result.hasNext());
            BindingSet bindings = result.next();
            assertTrue("Must have a binding for 's'", bindings.hasBinding("s"));
            assertTrue("Must have a binding for 'p'", bindings.hasBinding("p"));
            assertTrue("Must have a binding for 'o'", bindings.hasBinding("o"));
            assertEquals("Must have only 3 bindings", 3, bindings.getBindingNames().size());
            for (String bindingName : result.getBindingNames()) {
                Value time = bindings.getValue(bindingName);
                assertTrue("Returned value must be a literal", time instanceof Literal);
                long timeInMillisFromQuery = ((Literal) time).calendarValue().toGregorianCalendar().getTimeInMillis();
                assertEquals("Time must be a close match", timeInMillisFromSystem, timeInMillisFromQuery, 100);
            }
            assertFalse("There must be a single row in the result", result.hasNext());
        }
    }

    private void setPluginTimeOffset(int numHours) {
        try (RepositoryConnection connection = getRepository().getConnection()) {
            connection.begin();
            String updateString = String.format("insert data { <http://example.com/time> <%s> %d }",
                    numHours > 0 ? "http://example.com/goInFuture" : "http://example.com/goInPast", Math.abs(numHours));
            connection.prepareUpdate(updateString).execute();
            connection.commit();
        }
    }
}
