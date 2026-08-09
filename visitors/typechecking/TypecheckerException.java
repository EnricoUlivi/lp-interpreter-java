package projectLabo.visitors.typechecking;

public class TypecheckerException extends RuntimeException {

	public TypecheckerException() {
	}

	public TypecheckerException(String found, String expected) {
		this(String.format("Found  %s, expected %s", found, expected));
	}

	// Aggiungo un messaggio per due tipi attesi
	public TypecheckerException(String found, String expected1, String expected2) {
		this(String.format("Found  %s, expected %s or %s", found, expected1, expected2));
	}

	public TypecheckerException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public TypecheckerException(String message, Throwable cause) {
		super(message, cause);
	}

	public TypecheckerException(String message) {
		super(message);
	}

	public TypecheckerException(Throwable cause) {
		super(cause);
	}

}
