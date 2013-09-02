package de.skiptag.roadrunner.persistence;

import java.util.Objects;

import com.google.common.base.Strings;

/**
 * 
 * Representation of a Java based Roadrunner Path Path scheme: /<element1>/<element2>
 * 
 * @author Christoph Grotz
 * 
 */
public final class Path {

	private String[]	elements;

	/**
	 * @param path
	 *          {@link String} Path as String
	 */
	public Path(String path) {
		if (!Strings.isNullOrEmpty(path)) {
			this.elements = getPathElements(path);
		}
		if (elements == null) {
			this.elements = new String[] {};
		}
	}

	private String[] getPathElements(String path) {
		return path.startsWith("/") ? path.substring(1).split("/") : path.split("/");
	}

	/**
	 * @return the first element of the path
	 */
	public String getFirstElement() {
		if (elements == null || elements.length == 0) {
			return null;
		}
		return elements[0];
	}

	/**
	 * @return the last element of the path
	 */
	public String getLastElement() {
		if (elements.length == 0) {
			return null;
		}
		return elements[elements.length - 1];
	}

	/**
	 * Path: /element1/element2/element3/element4
	 * 
	 * Subpath from offset 2: /element3/element4
	 * 
	 * @param offset
	 * @return returns the subpath at the offset
	 */
	public Path getSubpath(int offset) {
		String output = "";
		for (int i = offset; i < elements.length; i++) {
			output += "/" + elements[i];
		}
		return new Path(output);
	}

	/**
	 * @return true if path consists of only one element
	 */
	public boolean isSimple() {
		return elements.length == 1;
	}

	/**
	 * Path: /element1/element2/element3/element4
	 * 
	 * Parent path: Path: /element1/element2/element3
	 * 
	 * @return returns the parent path
	 */
	public Path getParent() {
		String output = "";
		for (int i = 0; i < elements.length - 1; i++) {
			output += "/" + elements[i];
		}
		return new Path(output);
	}

	/**
	 * append element to path
	 * 
	 * @param element
	 *          element to append
	 * @return new Path with appended element
	 */
	public Path append(String element) {
		return new Path(toString() + "/" + element);
	}

	/**
	 * @return Path has no elements
	 */
	public boolean isEmtpy() {
		return elements.length <= 0;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(toString());
	}

	@Override
	public String toString() {
		String output = "";
		for (String element : elements) {
			output += "/" + element;
		}
		return output;
	}
}
