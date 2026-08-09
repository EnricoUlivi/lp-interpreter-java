package projectLabo.visitors.execution;

public interface Value {
	/* default conversion methods */
	default int toInt() {
		throw new InterpreterException(this.getClass().getSimpleName(), "IntValue");
	}

	default boolean toBool() {
		throw new InterpreterException(this.getClass().getSimpleName(), "BoolValue");
	}

	default PairValue toPair() {
		throw new InterpreterException(this.getClass().getSimpleName(), "PairValue");
	}

	default VectorValue toVector() {
		throw new InterpreterException(this.getClass().getSimpleName(), "VectorValue");
	}
}
