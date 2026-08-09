package projectLabo.parser.ast;

import projectLabo.visitors.Visitor;

public interface AST {
	<T> T accept(Visitor<T> visitor);
}
