package de.skiptag.roadrunner.core.helper;

public class Path {

    private String path;
    private String[] elements;

    public Path(String path) {
	this.path = path;
	this.elements = getPathElements(path);
    }

    private String[] getPathElements(String path) {
	return path.startsWith("/") ? path.substring(1).split("/")
		: path.split("/");
    }

    public String getFirstElement() {
	return elements[0];
    }

    public Path getSubpath(int offset) {
	String output = "";
	for (int i = offset; i < elements.length; i++) {
	    output += "/" + elements[i];
	}
	return new Path(output);
    }

    @Override
    public String toString() {
	String output = "";
	for (int i = 0; i < elements.length; i++) {
	    output += "/" + elements[i];
	}
	return output;
    }

    public boolean isSimple() {
	return elements.length == 1;
    }

    public String getLastElement() {
	return elements[elements.length - 1];
    }

    public Path getParent() {
	String output = "";
	for (int i = 0; i < elements.length - 1; i++) {
	    output += "/" + elements[i];
	}
	return new Path(output);
    }

}
