package projectLabo.parser.ast;

import projectLabo.visitors.Visitor;

public class Zip extends BinaryOp {
	public Zip(Exp left, Exp right) {
		super(left, right);
	}

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visitZip(left, right);
	}
}