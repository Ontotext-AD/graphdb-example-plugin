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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Tests the exampleBasic plugin.
 */
public class TestExampleBasicPlugin extends SingleRepositoryFunctionalTest {

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

    @Test
    public void testExampleBasicList() {
        try (RepositoryConnection connection = getRepository().getConnection()) {
            // Select everything provided by the list predicate
            TupleQuery query1 = connection.prepareTupleQuery("select ?s ?o { ?s <http://example.com/list> ?o }");
            try (TupleQueryResult result = query1.evaluate()) {
                assertEquals("All results listed",
                        Arrays.asList("[s=http://example.com/iri1;o=\"a\"]",
                                "[s=http://example.com/iri1;o=\"b\"]",
                                "[s=http://example.com/iri2;o=\"a\"]",
                                "[s=http://example.com/iri2;o=\"c\"]"),
                        extractBindingAsString(result));
            }

            // Select everything provided by the list predicate where the object position is "a"
            TupleQuery query2 = connection.prepareTupleQuery("select ?s { ?s <http://example.com/list> \"a\" }");
            try (TupleQueryResult result = query2.evaluate()) {
                assertEquals("Only subjects that have 'a' in the object position",
                        Arrays.asList("[s=http://example.com/iri1]", "[s=http://example.com/iri2]"),
                        extractBindingAsString(result));
            }

            // Select everything provided by the list predicate where the subject position is "http://example.com/iri2"
            TupleQuery query3 = connection.prepareTupleQuery(
                    "select ?o { <http://example.com/iri2> <http://example.com/list> ?o }");
            try (TupleQueryResult result = query3.evaluate()) {
                assertEquals("Only objects that have 'http://example.com/iri2' in the object position",
                        Arrays.asList("[o=\"a\"]", "[o=\"c\"]"),
                        extractBindingAsString(result));
            }

            // Now add some data that use http://example.com/iri1 and http://example.com/iri2
            connection.prepareUpdate("insert data {\n"
                    + "<http://example.com/John> a <http://example.com/Human> ;\n"
                    + "    <http://example.com/hasItem> <http://example.com/iri1> .\n"
                    + "<http://example.com/Mary> a <http://example.com/Human> ;\n"
                    + "    <http://example.com/hasItem> <http://example.com/iri2> .\n"
                    + "}").execute();

            // Select every human and the items the human has, then ask the plugin to list those items.
            // This essentially performs a join between data stored in the repository and data provided by the plugin.
            TupleQuery query4 = connection.prepareTupleQuery("select ?human ?item ?itemPart {\n"
                    + "?human a <http://example.com/Human> ;\n"
                    + "    <http://example.com/hasItem> ?item .\n"
                    + "?item <http://example.com/list> ?itemPart\n"
                    + "}");
            try (TupleQueryResult result = query4.evaluate()) {
                assertEquals("List humans and their corresponding items/parts",
                        Arrays.asList("[human=http://example.com/John;item=http://example.com/iri1;itemPart=\"a\"]",
                                "[human=http://example.com/John;item=http://example.com/iri1;itemPart=\"b\"]",
                                "[human=http://example.com/Mary;item=http://example.com/iri2;itemPart=\"a\"]",
                                "[human=http://example.com/Mary;item=http://example.com/iri2;itemPart=\"c\"]"),
                        extractBindingAsString(result));
            }

        }
    }

    private List<String> extractBindingAsString(TupleQueryResult result) {
        return result.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }
}
