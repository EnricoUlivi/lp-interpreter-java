package projectLabo.parser;

import projectLabo.parser.ast.Prog;

public interface ParserInterface extends AutoCloseable {

	Prog parseProg() throws ParserException;

}