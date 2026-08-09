package projectLabo.visitors.environments;

import projectLabo.parser.ast.NamedElement;

public interface EnvironmentInterface<T> {

	/* adds a new nested scope */

	void enterLevel();

	/* removes the most nested scope */

	void exitLevel();

	/*
	 * looks up the value associated with 'var' starting from the innermost scope;
	 * throws an 'EnvironmentException' if 'var' could not be found in any scope
	 */

	T lookup(NamedElement namedEl);

	/*
	 * updates the innermost scope by associating 'var' with 'info'; 'var' is not allowed
	 * to be already defined, 'var' and 'info' must be non-null
	 */

	T dec(NamedElement namedEl, T info);

	/*
	 * updates the innermost scope which defines 'var' by associating 'var' with
	 * 'info'; throws an 'EnvironmentException' if 'var' could not be found in any
	 * scope; 'var' and 'info' must be non-null
	 */

	void update(NamedElement namedEl, T info);

}
