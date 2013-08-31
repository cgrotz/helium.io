package de.skiptag.roadrunner.queries;

import java.util.Collection;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import de.skiptag.roadrunner.json.Node;
import de.skiptag.roadrunner.persistence.Path;
import de.skiptag.roadrunner.scripting.SandboxedScriptingEnvironment;

/**
 * 
 * Sandbox Query executing thanks to
 * http://worldwizards.blogspot.de/2009/08/java-scripting-api-sandbox.html
 * 
 * @author balu
 * 
 */
public class QueryEvaluator {

	private static final Logger LOGGER = LoggerFactory.getLogger(QueryEvaluator.class);
	private Multimap<String, String> attached_queries = HashMultimap.create();
	private Multimap<String, String> nodesForQuery = HashMultimap.create();

	private SandboxedScriptingEnvironment scriptingEnvironment = new SandboxedScriptingEnvironment();

	public boolean appliesToQuery(Path path, Object value) {
		for (String queryStr : attached_queries.get(path.toString())) {
			return evaluateQueryOnValue(value, queryStr);
		}
		return false;
	}

	public boolean evaluateQueryOnValue(Object value, String queryStr) {
		try {

			Object parsedValue;
			if (value instanceof Node) {
				parsedValue = scriptingEnvironment.eval("JSON.parse('"
						+ value.toString() + "');");
			} else {
				parsedValue = value;
			}
			scriptingEnvironment.eval("var query = " + queryStr + ";");
			Boolean result = (Boolean) scriptingEnvironment.invokeFunction("query", parsedValue);
			return result.booleanValue();
		} catch (Exception e) {
			LOGGER.error("Error (" + e.getMessage() + ") on Query (" + queryStr + ")", e);
		}
		return false;
	}

	public void addQuery(Path path, String query) {
		attached_queries.put(path.toString(), query);
	}

	public void removeQuery(Path path, String query) {
		attached_queries.remove(path.toString(), query);
	}

	public boolean hasQuery(Path path) {
		return attached_queries.containsKey(path.toString());
	}

	public boolean queryContainsNode(Path queryPath, String query, Path nodePath) {
		return nodesForQuery.containsEntry(new QueryEntry(queryPath, query).toString(), nodePath.toString());
	}

	public boolean addNodeToQuery(Path path, String query, Path pathToNode) {
		return nodesForQuery.put(new QueryEntry(path, query).toString(), pathToNode.toString());
	}

	public boolean removeNodeFromQuery(Path path, String query, Path pathToNode) {
		return nodesForQuery.remove(new QueryEntry(path, query).toString(), pathToNode.toString());
	}

	public Collection<Entry<String, String>> getQueries() {
		return attached_queries.entries();
	}
}
