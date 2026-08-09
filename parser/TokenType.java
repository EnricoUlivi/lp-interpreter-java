package projectLabo.parser;

public enum TokenType {
	// used internally by the tokenizer, should never be accessed by the parser
	KEYWORD, SKIP, SYMBOL,
	// non singleton categories
	IDENT, NUM,
	// end-of-file
	EOF,
	// symbols
	ASSIGN, CLOSE_BLOCK, CLOSE_PAR, EQ, MINUS, OPEN_BLOCK, OPEN_PAR, PAIR_OP, PLUS, STMT_SEP, TIMES, AND, NOT, 
	// new symbols
	CAT, ZIP, FLATTEN, OPEN_VECT, CLOSE_VECT, 
	// keywords
	BOOL, ELSE, FST, IF, PRINT, SND, VAR, ASSERT,
	// new keywords 
	FOR, IN

}
