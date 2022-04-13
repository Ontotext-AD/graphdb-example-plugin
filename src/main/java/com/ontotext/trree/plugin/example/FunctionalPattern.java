package com.ontotext.trree.plugin.example;

import com.ontotext.trree.sdk.PluginConnection;
import com.ontotext.trree.sdk.PluginException;
import com.ontotext.trree.sdk.StatementIterator;

/**
 * Interface that allows easy implementation of functional patterns that can be registered in {@link ExampleFunctionalPlugin}.
 */
public interface FunctionalPattern {
    /**
     * Returns the IRI to use for calling the functional pattern.
     *
     * @return an IRI as as {@link String}
     */
    String getIRI();

    /**
     * Returns the minimum number of arguments the functional pattern expects or zero if no minimum.
     *
     * @return a number
     */
    int getMinArguments();

    /**
     * Returns the maximum number of arguments the functional pattern expects or zero if no maximum.
     *
     * @return a number
     */
    int getMaxArguments();

    /**
     * Verifies the number of arguments for calling the functional pattern. The default implementation should suffice
     * in most cases.
     *
     * @param number the number of arguments that will be passed to the functional pattern
     */
    default void verifyNumberOfArguments(int number) {
        if (getMinArguments() > 0 && number < getMinArguments()) {
            throw new PluginException("Too few arguments for " + getIRI() + ": got " + number
                    + ", expected at least " + getMinArguments());
        } else if (getMaxArguments() > 0 && number > getMaxArguments()) {
            throw new PluginException("Too many arguments for " + getIRI() + ": got " + number
                    + ", expected at most " + getMaxArguments());
        }
    }

    /**
     * Evaluates the functional pattern with the provided arguments.
     *
     * @param arguments        the arguments as entity IDs
     * @param pluginConnection the plugin connection used to call the functional pattern
     * @return a {@link StatementIterator} that must bind the output of the functional pattern as the subject
     */
    StatementIterator evaluate(long[] arguments, PluginConnection pluginConnection);
}
