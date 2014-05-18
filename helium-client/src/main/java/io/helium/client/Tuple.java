package de.helium.client;

import com.google.common.base.Objects;

public class Tuple<X, Y> {
	public final X x;
	public final Y y;

	public Tuple(X x, Y y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(x, y);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Tuple) {
			return Objects.equal(x, ((Tuple) obj).x) && Objects.equal(y, ((Tuple) obj).y);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(Tuple.class).add("x", x).add("y", y).toString();
	}

}