package projectLabo.visitors.environments;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import projectLabo.parser.ast.NamedElement;

public class Environment<T> implements EnvironmentInterface<T> {

	private final LinkedList<HashMap<NamedElement, T>> levelList = new LinkedList<>(); // list of single level declarations
 
	/*
	 * enter a new nested level; private method shared by 'enterLevel()' and the
	 * constructor 'Environment()'
	 */
	private void addEmptyLevel() {
		levelList.addFirst(new HashMap<>());
	}

	/* create an environment with just one empty level */
	public Environment() {
		addEmptyLevel();
	}

	@Override
	public void enterLevel() {
		addEmptyLevel();
	}

	@Override
	public void exitLevel() {
		levelList.removeFirst();
	}

	/*
	 * looks up the innermost level where 'namedEl' is found;
	 * throws an 'EnvironmentException' if 'namedEl' could not be found at any level
	 */

	protected Map<NamedElement, T> resolve(NamedElement namedEl) {
		for (var level : levelList)
			if (level.containsKey(namedEl))
				return level;
		throw new EnvironmentException("Undeclared " + namedEl);
	}

	@Override
	public T lookup(NamedElement namedEl) {
		return resolve(namedEl).get(namedEl);
	}

	/*
	 * updates map to associate 'namedEl' with 'info'; 'namedEl' and 'info' must be non-null
	 */

	private static <T> T updateLevel(Map<NamedElement, T> map, NamedElement namedEl, T info) {
		return map.put(requireNonNull(namedEl), requireNonNull(info));
	}

	/*
	 * updates the innermost level by associating 'namedEl' with 'info'; 'namedEl' is not allowed
	 * to be already defined, 'namedEl' and 'info' must be non-null
	 */

	@Override
	public T dec(NamedElement namedEl, T info) {
		var level = levelList.getFirst();
		if (level.containsKey(namedEl))
			throw new EnvironmentException(namedEl + " already declared");
		return updateLevel(level, namedEl, info);
	}

	/*
	 * updates the 'info' of 'namedEl' found at the innermost level, throws an 'EnvironmentException' if no 'namedEl' can be
	 * found in the list of levels. Only used for the dynamic semantics
	 */

	@Override
	public void update(NamedElement namedEl, T info) {
		var level = resolve(namedEl);
		updateLevel(level, namedEl, info);
	}

}
