package projectLabo.visitors.execution;

import static java.util.Objects.requireNonNull;

public record PairValue(Value fstVal, Value sndVal) implements Value {

	public PairValue {
		requireNonNull(fstVal);
		requireNonNull(sndVal);
	}

	@Override
	public PairValue toPair() {
		return this;
	}

	@Override
	public String toString() {
		return String.format("(%s,%s)", fstVal, sndVal);
	}

}
