package projectLabo.visitors.execution;

import java.util.List;
import static java.util.Objects.requireNonNull;

public record VectorValue(List<Value> elements) implements Value {

    public VectorValue {
        elements = List.copyOf(requireNonNull(elements));
    }

    @Override
    public VectorValue toVector() {
        return this;
    }

    @Override
    public String toString() {
        return "VectorValue[" + elements.size() + "]";
    }
}