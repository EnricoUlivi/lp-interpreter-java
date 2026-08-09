package projectLabo.parser;

import java.io.IOException;

public interface TokenizerInterface extends AutoCloseable {

	TokenType next() throws TokenizerException;

	TokenType tokenType();

	String tokenString();

	int intValue();

	boolean boolValue();

	void close() throws IOException;

	int getLineNumber();

}