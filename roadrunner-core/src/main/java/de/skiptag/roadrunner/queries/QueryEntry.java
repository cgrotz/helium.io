package de.skiptag.roadrunner.queries;

import com.google.common.base.Objects;

import de.skiptag.roadrunner.persistence.Path;

public class QueryEntry {

	private String path;
	private String query;

	public QueryEntry(Path path, String query) {
		this.path = path.toString();
		this.query = query;
	}

	public Path getPath() {
		return new Path(path);
	}

	public String getQuery() {
		return query;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(path, query);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(QueryEntry.class)
				.add("path", path)
				.add("query", query)
				.toString();
	}
}
