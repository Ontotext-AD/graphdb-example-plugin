package com.ontotext.trree.plugin.example;

import com.ontotext.trree.sdk.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.Date;

public class ExampleBasicPlugin extends PluginBase implements PatternInterpreter {

	// The predicate we will be listening for
	private static final String TIME_PREDICATE = "http://example.com/now";

	private IRI predicate; // The predicate IRI
	private long predicateId; // ID of the predicate in the entity pool

	// Service interface methods
	@Override
	public String getName() {
		return "exampleBasic";
	}

	// Plugin interface methods
	@Override
	public void initialize(InitReason reason, PluginConnection pluginConnection) {
		// Create an IRI to represent the predicate
		predicate = SimpleValueFactory.getInstance().createIRI(TIME_PREDICATE);
		// Put the predicate in the entity pool using the SYSTEM scope
		predicateId = pluginConnection.getEntities().put(predicate, Entities.Scope.SYSTEM);

		getLogger().info("ExampleBasic plugin initialized!");
	}

	// PatternInterpreter interface methods
	@Override
	public StatementIterator interpret(long subject, long predicate, long object, long context,
									   PluginConnection pluginConnection, RequestContext requestContext) {
		// Ignore patterns with predicate different than the one we are interested in. We want to return the
		// SystemDate only when we detect the <http://example.com/time> predicate.
		if (predicate != predicateId)
			// This will tell the PluginManager that we can't interpret the statement so the statement can be passed
			// to another plugin.
			return null;

		// Create the date/time literal. Here it is important to create the literal in the entities instance of the
		// request and NOT in getEntities(). If you create it in the entities instance returned by getEntities() it
		// will not be visible in the current request.
		long literalId = createDateTimeLiteral(pluginConnection.getEntities());

		// return a StatementIterator with a single statement to be iterated. The object of this statement will be the
		// current timestamp.
		return StatementIterator.create(subject, predicate, literalId, 0);
	}

	@Override
	public double estimate(long subject, long predicate, long object, long context,
						   PluginConnection pluginConnection, RequestContext requestContext) {
		// We always return a single statement so we return a constant 1. This value will be used by the QueryOptimizer
		// when crating the execution plan.
		return 1;
	}

	private long createDateTimeLiteral(Entities entities) {
		// Create a literal for the current timestamp.
		Value literal = SimpleValueFactory.getInstance().createLiteral(new Date());

		// Add the literal in the entity pool with REQUEST scope. This will make the literal accessible only for the
		// current Request and will be disposed once the request is completed. Return it's ID.
		return entities.put(literal, Entities.Scope.REQUEST);
	}

}
