package com.ontotext.trree.plugin.example;

import com.ontotext.trree.sdk.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.Date;

public class ExampleBasicPlugin extends PluginBase implements PatternInterpreter {

	// The predicate we will be listening for
	private static final String NOW_PREDICATE = "http://example.com/now";
	private static final String LIST_PREDICATE = "http://example.com/list";

	private long nowPredicateId; // ID of the predicate in the entity pool
	private long listPredicateId; // ID of the predicate in the entity pool

	// Service interface methods
	@Override
	public String getName() {
		return "exampleBasic";
	}

	// Plugin interface methods
	@Override
	public void initialize(InitReason reason, PluginConnection pluginConnection) {
		// Create an IRI to represent the now predicate
		IRI nowPredicate = SimpleValueFactory.getInstance().createIRI(NOW_PREDICATE);
		// Put the predicate in the entity pool using the SYSTEM scope
		nowPredicateId = pluginConnection.getEntities().put(nowPredicate, Entities.Scope.SYSTEM);

		// Now the same for the list predicate
		IRI listPredicate = SimpleValueFactory.getInstance().createIRI(LIST_PREDICATE);
		// Put the predicate in the entity pool using the SYSTEM scope
		listPredicateId = pluginConnection.getEntities().put(listPredicate, Entities.Scope.SYSTEM);

		getLogger().info("ExampleBasic plugin initialized!");
	}

	// This method will be called to determine if the plugin is interested in handling a given triple pattern.
	// If the plugin wants to handle it, it must return a non-null value.
	//
	// The method may be called multiple times based on what needs to be evaluated.
	//
	// When the supplied subject predicate, object or context is non-zero it provides the actual value at that position
	// and the plugin is expected to take it into account.
	//
	// When the value is zero, the plugin is expected to provide all possible bindings for the value.
	@Override
	public StatementIterator interpret(long subject, long predicate, long object, long context,
									   PluginConnection pluginConnection, RequestContext requestContext) {
		if (predicate == nowPredicateId) {
			// Create the date/time literal. Here it is important to create the literal in the entities instance of the
			// request and NOT in getEntities(). If you create it in the entities instance returned by getEntities() it
			// will not be visible in the current request.
			long literalId = createDateTimeLiteral(pluginConnection.getEntities());

			// return a StatementIterator with a single statement to be iterated. The object of this statement will be the
			// current timestamp.
			return StatementIterator.create(subject, predicate, literalId, 0);
		} else if (predicate == listPredicateId) {
			// Creates an iterator that enumerates all possible list values, taking into account the bound subject
			// and object (i.e. bound if they are non-zero).
			return new ExampleListIterator(subject, object, pluginConnection);
		} else {
			// Ignore patterns with predicate different from the ones we are interested in.
			//
			// This will tell the PluginManager that we can't interpret the statement so the statement can be passed
			// to another plugin or sourced from regular data.
			return null;
		}
	}

	// When interpret() returns a non-null value, this method will be called to determine the expected complexity
	// of the iterator returned by interpret(). Lower values mean lesser complexity.
	//
	// The query optimizer prefers query plans with lesser complexity and generally pushes less complex triple
	// patterns to the top, so they get evaluated first.
	//
	// Unlike interpret(), this method may be called with the special value Entities.BOUND for subject, predicate,
	// object or context, which signals "what is the estimated complexity when the value is bound but not yet known".
	@Override
	public double estimate(long subject, long predicate, long object, long context,
						   PluginConnection pluginConnection, RequestContext requestContext) {
		// We always return a constant 1. This is a simple way to ensure our plugin gets evaluated early.
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
