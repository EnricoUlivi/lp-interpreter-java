package projectLabo.visitors.execution;

import static java.util.Objects.requireNonNull;

public record BoolValue(Boolean value) implements AtomicValue<Boolean> {

	public BoolValue {
		requireNonNull(value);
	}

	@Override
	public boolean toBool() {
		return value;
	}

	@Override
	public final String toString() {
		return value.toString();
	}
}
