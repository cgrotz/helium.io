/*
 * Copyright 2012 The Helium Project
 *
 * The Helium Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.helium.common;

import com.google.common.base.Strings;

import java.util.Objects;

/**
 * 
 * Representation of a Java based Helium Path Path scheme: /<element1>/<element2>
 * 
 * @author Christoph Grotz
 * 
 */
public final class Path {

	private String[]	elements;

    public Path(String[]	elements) {
        this.elements = elements;

    }
	/**
	 * @param path
	 *          {@link String} Path as String
	 */
	public Path(String path) {
        String workPath = path.replaceAll("//","/");
		if (!Strings.isNullOrEmpty(workPath)) {
			this.elements = getPathElements(workPath);
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
	public String firstElement() {
		if (elements == null || elements.length == 0) {
			return null;
		}
		return elements[0];
	}

	/**
	 * @return the last element of the path
	 */
	public String lastElement() {
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
	public Path subpath(int offset) {
		String output = "";
		for (int i = offset; i < elements.length; i++) {
			output += "/" + elements[i];
		}
		return new Path(output);
	}

    /**
     * Path: /element1/element2/element3/element4
     *
     * Subpath from offset 2: /element1/element2
     *
     * @param offset
     * @return returns the subpath at the offset
     */
    public Path prefix(int offset) {
        String output = "";
        for (int i = 0; i < offset; i++) {
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
	public Path parent() {
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

    public Path append(Path path) {
        return new Path(toString() + path.toString());
    }

	/**
	 * @return Path has no elements
	 */
	public boolean isEmtpy() {

        return elements.length <= 0 || (elements.length == 1 && Strings.isNullOrEmpty(elements[0]));
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

	public String[] toArray() {
		return this.elements;
	}

    public static Path of(String key) {
        return new Path(key);
    }

    public static Path copy(Path path) {
        return new Path(path.toString());
    }
}
