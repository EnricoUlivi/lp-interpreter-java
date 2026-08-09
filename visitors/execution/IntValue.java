package projectLabo.visitors.execution;

import static java.util.Objects.requireNonNull;

public record IntValue(Integer value) implements AtomicValue<Integer> {

	public IntValue {
		requireNonNull(value);
	}

	@Override
	public int toInt() {
		return value;
	}
	
	@Override
	public final String toString() {
		return value.toString();
	}

}