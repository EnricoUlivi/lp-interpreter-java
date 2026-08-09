package projectLabo.visitors.execution;

public interface AtomicValue<T> extends Value { // Value objects which cannot be decomposed into simplest Value objects 
	T value();
}
