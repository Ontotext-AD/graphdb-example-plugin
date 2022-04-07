# GraphDB Example plugins

This is a sample project which aims to illustrate the use of the GraphDB Plugin API. Is is created entirely for training
purposes.

Additional documentation on the Plugin API and the project itself you can find
[here](http://graphdb.ontotext.com/free/plug-in-api.html) 

## Functionality

The project contains two plugins -- a basic plugin and a more complex one.

The responsibility of the basic plugin is:

- It interprets the pattern `?s <http://example.com/now> ?o` and binds the object to a literal containing
the system date/time of the machine running GraphDB. The subject position is not used and its value does not matter.

- It interprets the pattern `?s <http://example.com/list> ?o` and binds the subject and object to a set of values:

    | ?s                      | ?o  |
    |-----|-----|
    | http://example.com/iri1 | "a" |
    | http://example.com/iri1 | "b" |
    | http://example.com/iri2 | "a" |
    | http://example.com/iri2 | "c" |

    This pattern will also take into account the values of ?s and ?o, if they are bound by other patterns in the same query.

The complex plugin has more responsibilities:

- If a `FROM <http://example.com/time>` clause is detected in the query, the result is a single binding set in which
all projected variables are bound to a literal containing the system date/time of the machine running GraphDB.
- If a triple with the subject `http://example.com/time` and one of the predicates `http://example.com/goInFuture`
or `http://example.com/goInPast` is inserted, its object is set as a positive or negative offset for all future requests
querying the system date/time via the plugin.

## Overview

The main plugin classes in this project are `ExamplePlugin` and `ExampleBasicPlugin`. They both extend
`com.ontotext.trree.sdk.PluginBase` -- the base class for all Plugins.

The interfaces which the plugins implement are:

- `PatternInterpreter` -- allows interpretation of basic triple patterns
- `UpdateInterpreter` -- allows interpretation of update triple patterns
- `Preprocessor` -- used to add context to a request at the beginning if the processing
- `Postprocessor` -- used to modify the query results

As the `PluginBase` implements `com.ontotext.trree.sdk.Service` we need to have a service descriptor in
`META-INT/services/`. 

## Deployment

Below you can find the deployment steps for this plugin:

1. In the `pom.xml` set the version of GraphDB you are using (9.0.0 or newer required).
This GraphDB version will be used for compiling and testing only.
2. In the service descriptor include the class of the plugin you want to build (by default both are included)
3. Build the project using `mvn clean package`
4. Unzip `./target/example-plugin-graphdb-plugin.zip` in  `<GDB_INST_DIR>/lib/plugins/`.

Once you start GraphDB and a repository is initialized you will see the following entries in the log:
```
Registering plugin exampleBasic
Initializing plugin 'exampleBasic'
ExampleBasic plugin initialized!
...
Registering plugin example
Initializing plugin 'example'
Example plugin initialized!
```

## Usage of the ExampleBasic plugin

### Get the time

Run the following query to retrieve the current system date/time:

```
SELECT ?o
WHERE {
    [] <http://example.com/now> ?o .
} 
```

The variable `?o` will be bound to a literal with the `xsd:dateTime` type.

### List and filter values

Insert the following simple data to provide a small set of humans that each have associated items:

```
INSERT DATA {
    <http://example.com/John> a <http://example.com/Human> ;
        <http://example.com/hasItem> <http://example.com/iri1> .
    <http://example.com/Mary> a <http://example.com/Human> ;
        <http://example.com/hasItem> <http://example.com/iri2> .
}
```

Then run this query to fetch the items associated with each human as well as the item parts (provided by the ExampleBasic plugin via the <http://example.com/list> predicate):

```
SELECT ?human ?item ?itemPart {
    ?human a <http://example.com/Human> ;
        <http://example.com/hasItem> ?item .
    ?item <http://example.com/list> ?itemPart
}
```

The results look like this:

| ?human                  | ?item                   | ?itemPart |
|-------------------------|-------------------------|-----------|
| http://example.com/John | http://example.com/iri1 | "a"       |
| http://example.com/John | http://example.com/iri1 | "b"       |
| http://example.com/Mary | http://example.com/iri2 | "a"       |
| http://example.com/Mary | http://example.com/iri2 | "c"       |


## Usage of the Example plugin

- Run the following query to get all unbound variables (in this case ?s, ?p and ?o) set to the system data/time plus optional offset:
```
SELECT *
FROM <http://example.com/time>
WHERE
{
    ?s ?p ?o .
}
```

The variables `?s`, `?p` and `?o` will be bound to literals with the `xsd:dateTime` type. 

- Set a system time offset of X hours in the future using this update:

```
# Adds two hours
INSERT DATA
{
    <http://example.com/time> <http://example.com/goInFuture> 2 .
}
```

- Set a system time offset of X hours in the past using this update:

```
# Removes one hour
INSERT DATA
{
    <http://example.com/time> <http://example.com/goInPast> 1 .
}
```

Once you run one of the inserts you can use the above select query to verify the result.

## Caution

Please be extremely careful when adding a new plugin to GraphDB. Faulty plugins can have a devastating effect on the
system -- they can affect the performance negatively, cause memory leaks or lead to non-deterministic behaviour!
