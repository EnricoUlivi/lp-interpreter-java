package projectLabo.parser.ast;

import projectLabo.visitors.Visitor;

public class EmptyStmtSeq extends EmptySeq implements StmtSeq {

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visitEmptyStmtSeq();
	}
}
