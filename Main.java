package projectLabo;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import projectLabo.parser.Parser;
import projectLabo.parser.ParserException;
import projectLabo.parser.Tokenizer;
import projectLabo.visitors.typechecking.Typecheck;
import projectLabo.visitors.typechecking.TypecheckerException;
import projectLabo.visitors.execution.Execute;
import projectLabo.visitors.execution.InterpreterException;

import static java.lang.System.err;

public class Main {

	// option flags
	private static final String IN = "-i";
	private static final String OUT = "-o";
	private static final String NO_TYPE_CHECK = "-ntc";

	private record Option(boolean hasArg, String value) {
	};

	private static final Map<String, Option> options = new HashMap<>();

	static {
		options.put(IN, new Option(true, null)); // input option, has argument, default value is null
		options.put(OUT, new Option(true, null)); // output option, has argument, default value is null
		options.put(NO_TYPE_CHECK, new Option(false, null)); // no-type-check flag, no argument
	}

	// handles option errors
	private static void error(String msg) {
		System.err.println(msg);
		System.exit(1);
	}

	private static void processArgs(String[] args) {
		for (var i = 0; i < args.length; i++) {
			var optionString = args[i];
			var currentOption = options.get(optionString);
			if (currentOption == null)
				error("Option error.\nValid options:\n\t" + IN + " <input>\n\t" + OUT + " <output>\n\t" + NO_TYPE_CHECK);
			if (currentOption.hasArg()) // sets option with argument
			{
				if (i + 1 == args.length)
					error("Missing argument for option " + optionString);
				options.put(optionString, new Option(true, args[++i]));
			} else
				options.put(optionString, new Option(false, "")); // sets the option with no argument, any non null
																	// string works
		}
	}

	// opens the input file `inputPath`, or the standard input
	private static BufferedReader openInput(String inputPath) throws FileNotFoundException {
		return new BufferedReader(inputPath == null ? new InputStreamReader(System.in) : new FileReader(inputPath));
	}

	// opens the output file `outputPath`, or the standard output
	private static PrintWriter openOutput(String outputPath) throws FileNotFoundException {
		return outputPath == null ? new PrintWriter(System.out) : new PrintWriter(outputPath);
	}

	public static void main(String[] args) {
		processArgs(args);
		try (final var reader = openInput(options.get(IN).value);
				final var writer = openOutput(options.get(OUT).value);
				final var tokenizer = new Tokenizer(reader);
				final var parser = new Parser(tokenizer)) {
			var prog = parser.parseProg();
			if (options.get(NO_TYPE_CHECK).value == null)
				prog.accept(new Typecheck());
			prog.accept(new Execute(writer));
		} catch (IOException exc) {
			err.println("I/O error: " + exc.getMessage());
		} catch (ParserException exc) {
			err.println("Syntax error " + exc.getMessage());
		} catch (TypecheckerException e) {
			error("Static error: " + e.getMessage());
		} catch (InterpreterException e) {
			error("Dynamic error: " + e.getMessage());
		} catch (Throwable exc) {
			err.println("Unexpected error.");
			exc.printStackTrace();
		}
	}

}
