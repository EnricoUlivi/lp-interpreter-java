package projectLabo.parser.ast;

import static java.util.Objects.requireNonNull;
import projectLabo.visitors.Visitor;

public class Flatten implements Exp {
	private final Exp exp; 

	public Flatten(Exp exp) {
		this.exp = requireNonNull(exp);
	}

	@Override
	public String toString() {
		return String.format("%s(%s)", getClass().getSimpleName(), exp);
	}

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visitFlatten(exp);
	}
}