package com.ontotext.trree.plugin.example;

import com.ontotext.trree.sdk.PluginConnection;
import com.ontotext.trree.sdk.StatementIterator;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.util.Optional;

/**
 * Implements the http://example.com/getLabel functional pattern.
 */
public class GetLabelFunctionalPattern implements FunctionalPattern {
    @Override
    public String getIRI() {
        return "http://example.com/getLabel";
    }

    @Override
    public int getMinArguments() {
        return 3;
    }

    @Override
    public int getMaxArguments() {
        return 0;
    }

    @Override
    public StatementIterator evaluate(long[] languageIds, PluginConnection pluginConnection) {
        return new StatementIterator() {
            final long subjectId = languageIds[0];
            final long labelPredicateId = languageIds[1];

            int languageIndex = 0;
            String language;
            boolean lockLanguage;
            StatementIterator iter;

            private boolean languageMatches(Literal label) {
                if (language.isEmpty() && label.getDatatype().equals(XSD.STRING)) {
                    return true;
                }
                Optional<String> labelLanguage = label.getLanguage();
                return labelLanguage.isPresent() && Literals.langMatches(labelLanguage.get(), language);
            }

            @Override
            public boolean next() {
                if (language == null) {
                    if (languageIndex + 2 < languageIds.length) {
                        // one of the languages in the list
                        language = pluginConnection.getEntities().get(languageIds[languageIndex + 2]).stringValue();
                    } else if (languageIndex + 2 <= languageIds.length) {
                        // last resort, an xsd:string literal - represented simply as an empty language tag
                        language = "";
                    } else {
                        // no more languages to try
                        return false;
                    }
                    if (iter != null) {
                        // Don't forget to close an iterator when you ditch it!
                        iter.close();
                    }
                    iter = pluginConnection.getStatements().get(subjectId, labelPredicateId, 0);
                    languageIndex++;
                }

                while (iter.next()) {
                    Value label = pluginConnection.getEntities().get(iter.object);
                    if (label instanceof Literal) {
                        if (languageMatches((Literal) label)) {
                            // Found a matching language, bind the matching label as the subject of this iterator
                            subject = iter.object;
                            // Raise flag to lock language so the iterator will loop through all remaining labels
                            // that have the same language.
                            lockLanguage = true;
                            return true;
                        }
                    }
                }

                if (!lockLanguage) {
                    // Didn't find any label for the currently requested language, reset and retry with next language
                    language = null;
                    return next();
                }

                return false;
            }

            @Override
            public void close() {
                if (iter != null) {
                    iter.close();
                }
            }
        };
    }
}
