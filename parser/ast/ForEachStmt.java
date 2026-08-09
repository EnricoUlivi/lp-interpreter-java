package projectLabo.parser.ast;

import static java.util.Objects.requireNonNull;
import projectLabo.visitors.Visitor;

public class ForEachStmt implements Stmt {
	private final Variable var; 
	private final Exp exp;
	private final Block body; 

	public ForEachStmt(Variable var, Exp exp, Block body) {
		this.var = requireNonNull(var);
		this.exp = requireNonNull(exp);
		this.body = requireNonNull(body);
	}

	@Override
	public String toString() {
		return String.format("%s(%s,%s,%s)", getClass().getSimpleName(), var, exp, body);
	}

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visitForEachStmt(var, exp, body);
	}
}