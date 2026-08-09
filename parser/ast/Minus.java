package projectLabo.parser.ast;

import projectLabo.visitors.Visitor;

public class Minus extends UnaryOp {

	public Minus(Exp exp) {
		super(exp);
	}
	
	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visitMinus(exp);
	}
}
