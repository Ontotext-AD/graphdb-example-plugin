package com.ontotext.trree.plugin.example;

import com.ontotext.test.functional.base.SingleRepositoryFunctionalTest;
import com.ontotext.test.utils.StandardUtils;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests the exampleFunctional plugin.
 */
public class TestExampleFunctionalPlugin extends SingleRepositoryFunctionalTest {
    @Override
    protected RepositoryConfig createRepositoryConfiguration() {
        // Creates a repository configuration with the rdfsplus-optimized ruleset
        return StandardUtils.createOwlimSe("rdfsplus-optimized");
    }

    @Before
    public void setup() throws IOException {
        try (RepositoryConnection connection = getRepository().getConnection()) {
            connection.add(getClass().getResource("/example-functional-data.ttl"), RDFFormat.TURTLE);
        }
    }

    @Test
    public void testInsufficientNumberOfArguments() {
        try (RepositoryConnection connection = getRepository().getConnection()) {
            try (TupleQueryResult result = connection.prepareTupleQuery(""
                            + "prefix data: <http://example.com/data/>\n"
                            + "select ?label {\n"
                            + "  ?label <http://example.com/getLabel> (data:StarTrekTNG rdfs:label)\n"
                            + "}")
                    .evaluate()) {
                while (result.hasNext()) {
                    result.next();
                }
                fail("Must fail with exception");
            } catch (RuntimeException e) {
                MatcherAssert.assertThat(e.getMessage(),
                        CoreMatchers.containsString("Too few arguments"));
            }
        }
    }

    @Test
    public void testLabelSpanishIndirectFallbackToEnglish() {
        try (RepositoryConnection connection = getRepository().getConnection()) {
            try (TupleQueryResult result = connection.prepareTupleQuery(""
                    + "prefix model: <http://example.com/model/>\n"
                    + "select ?movie ?label {\n"
                    + "  ?movie a model:Movie .\n"
                    + "  model:Setting1 model:labelPredicate ?labelPred ;\n"
                    + "     model:language ?language .\n"
                    + "  ?label <http://example.com/getLabel> (?movie ?labelPred ?language \"en\")\n"
                    + "}").evaluate()) {
                assertEquals("Any country Spanish, otherwise any country English or xsd:string",
                        Arrays.asList(
                                "[movie=http://example.com/data/StarTrekTOS;label=\"La conquista del espacio\"@es-ES]",
                                "[movie=http://example.com/data/StarTrekTOS;label=\"Viaje a las estrellas: la serie original\"@es-LA]",
                                "[movie=http://example.com/data/StarTrekTNG;label=\"Star Trek: The Next Generation\"@en]",
                                "[movie=http://example.com/data/StarTrekSNW;label=\"Star Trek: Strange New Worlds\"^^<http://www.w3.org/2001/XMLSchema#string>]"),
                        extractBindingAsString(result));
            }
        }
    }

    @Test
    public void testLabelEuropeanPortugueseFallbackToGerman() {
        try (RepositoryConnection connection = getRepository().getConnection()) {
            try (TupleQueryResult result = connection.prepareTupleQuery(""
                    + "prefix model: <http://example.com/model/>\n"
                    + "select ?movie ?label {\n"
                    + "  ?movie a model:Movie .\n"
                    + "  model:Setting1 model:labelPredicate ?labelPred .\n"
                    + "  ?label <http://example.com/getLabel> (?movie ?labelPred \"pt-PT\" \"de\")\n"
                    + "}").evaluate()) {
                assertEquals("European Portuguese, otherwise any country German or xsd:string",
                        Arrays.asList(
                                "[movie=http://example.com/data/StarTrekTNG;label=\"Star Trek: A Geração Seguinte\"@pt-PT]",
                                "[movie=http://example.com/data/StarTrekSNW;label=\"Star Trek: Fremde neue Welten\"@de]"),
                        extractBindingAsString(result));
            }
        }
    }

    @Test
    public void testLabelGerman() {
        try (RepositoryConnection connection = getRepository().getConnection()) {
            try (TupleQueryResult result = connection.prepareTupleQuery(""
                    + "prefix model: <http://example.com/model/>\n"
                    + "select ?movie ?label {\n"
                    + "  ?movie a model:Movie .\n"
                    + "  model:Setting1 model:labelPredicate ?labelPred .\n"
                    + "  ?label <http://example.com/getLabel> (?movie ?labelPred \"de\")\n"
                    + "}").evaluate()) {
                assertEquals("Any country German or xsd:string",
                        Arrays.asList(
                                "[movie=http://example.com/data/StarTrekTNG;label=\"Star Trek: The Next Generation\"^^<http://www.w3.org/2001/XMLSchema#string>]",
                                "[movie=http://example.com/data/StarTrekSNW;label=\"Star Trek: Fremde neue Welten\"@de]"),
                        extractBindingAsString(result));
            }
        }
    }

    private List<String> extractBindingAsString(TupleQueryResult result) {
        return result.stream()
                .map(Object::toString)
                .peek(System.out::println)
                .collect(Collectors.toList());
    }
}
