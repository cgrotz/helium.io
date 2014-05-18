package io.helium.rpc;

public class ObjectConverter {

	public Object convert(Object value, Class<?> clazz) {
		if (value == null) {
			return null;
		}
		if (clazz.isAssignableFrom(value.getClass())) {
			return clazz.cast(value);
		}
		if (clazz.isAssignableFrom(Integer.class) && value.getClass().isAssignableFrom(String.class)) {
			return Integer.valueOf((String) value);
		}
		throw new RuntimeException("Not convertable " + value.getClass().getSimpleName() + " to "
				+ clazz.getSimpleName());
	}
}
