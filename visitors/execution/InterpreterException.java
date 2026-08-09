package projectLabo.visitors.execution;

public class InterpreterException extends RuntimeException {

	public InterpreterException() {
	}

	public InterpreterException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public InterpreterException(String found, String expected1, String expected2) {
		super(String.format("Found %s, expected %s or %s", found, expected1, expected2));
	}

	public InterpreterException(String message, Throwable cause) {
		super(message, cause);
	}

	public InterpreterException(String message) {
		super(message);
	}

	public InterpreterException(Throwable cause) {
		super(cause);
	}

	public InterpreterException(String found, String expected) {
		super(String.format("Found %s, expected %s", found, expected));
	}

}
