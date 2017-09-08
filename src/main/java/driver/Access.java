package driver;

import java.util.*;
import org.apache.log4j.*;
import com.mongodb.*;

import databaseModule.*;

/**
 * Runs the complete diagnostic tool from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.3
 */
public class Access {

	public static Scanner s = new Scanner(System.in);

	private static String branch = "home";
	public static final String VERSION = "1.3.0";
	public static final String APPNAME = "lighthouse-v" + VERSION;

	private static String help = "\nHOME PAGE -- POSSIBLE COMMANDS"
			+ "\n'help'\n\tgoes to the help page for the general diagnostic tool"
			+ "\n\tUsage: ~$ help"
			+ "\n'db'\n\tswitches to the database module to access functions that directly edit the database"
			+ "\n\tUsage: ~$ db"
			+ "\n'query'\n\tswitches to the query module to access functions that analyze the contents of the database"
			+ "\n\tUsage: ~$ query\n";

	private static String note = "Note: lighthouse must be used in conjuntion with a working MongoDB connection."
			+ "\nTo ensure a working connection to the database, ensure the executable 'mongod' is running.";

	/**
	 * Takes command-line arguments and delegates functionality as appropriate.
	 * 
	 * @param args
	 *            command-line arguments
	 */
	public static void main(String[] args) {

		// disables logging, works in parallel with log4j.properties
		@SuppressWarnings("unchecked")
		List<Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
		loggers.add(LogManager.getRootLogger());
		for (Logger logger : loggers) {
			logger.setLevel(Level.OFF);
		}

		try {

			// startup
			printSplash();
			System.out.println("\n" + note);
			MongoManager.connectToDatabase();
			System.out.println(help);

			// runs until `exit` command given
			while (true) {

				// prompts and takes input, split into array in case of multiple commands
				System.out.print("lighthouse-v" + VERSION + ": " + branch + " $ ");
				String[] input = parseStatements(s.nextLine());

				for (int cmdi = 0; cmdi < input.length; cmdi++) {

					// cleans commands for processing
					args = parseArgs(input[cmdi]);

					// switches modules or exits application
					switch (args[0]) {
					case "home":
					case "db":
					case "query":
						branch = args[0];
						if (args.length > 1) {
							args = Arrays.copyOfRange(args, 1, args.length);
						} else {
							args[0] = "help";
						}
						break;
					case "quit":
					case "exit":
						s.close();
						MongoManager.disconnect();
						System.exit(0);
						break;
					}

					// executes commands for 'home' branch
					if (branch.equals("home")) {
						switch (args[0]) {
						case "man":
						case "help":
							System.out.println(help);
							break;
						default:
							System.err.println("Invalid input. Use the 'help' command for details on usage.\n");
							continue;
						}
					}

					// delegates functionality as appropriate
					switch (branch) {
					case "home":
						break;
					case "db":
						AccessDB.run(args);
						break;
					case "query":
						AccessQRY.run(args);
						break;
					default:
						System.err.println("[ERROR] This should never run.");
						break;
					}

				} // end of multi-command loop
			} // end of indefinite while loop

		} catch (MongoTimeoutException t) {
			String error = "[DATABASE MESSAGE] Server connection timed out. Make sure a 'mongod' instance is running.";
			System.out.println(error + "\n\nExiting with error code 1.");
			System.exit(1);
		} catch (MongoSocketWriteException | MongoSocketOpenException s) {
			String error = "[DATABASE MESSAGE] Mongo connection interrupted. Check if 'mongod' is still running.";
			System.out.println(error + "\n\nExiting with error code 2.");
			System.exit(2);
		}
	}

	/**
	 * Cleans single command-line statements for processing.
	 * 
	 * @param command
	 *            a full command-line statement
	 * @return an array of command-line arguments meant to mimic the standard 'args' parameter
	 */
	public static String[] parseArgs(String command) {

		// replaces all trailing, leading, or duplicate spaces
		command = command.trim().replaceAll("\\s{2,}", " ");

		String[] args = command.split(" ");

		// processes args within quotation marks
		if (command.indexOf('\"') != -1) {
			ArrayList<String> parsed = new ArrayList<>(); // to simplify chaining logic
			String full = null; // chains args within quotation marks, null otherwise
			boolean added = false;
			for (int i = 0; i < args.length; i++) {
				if (args[i].indexOf("\"") != args[i].lastIndexOf("\"")) {
					String cleaned = args[i].replaceAll("\"", "");
					full = full == null ? cleaned : full + " " + cleaned;
					added = false;
				} else if (full == null && !args[i].contains("\"")) {
					parsed.add(args[i]);
					added = true;
				} else if (full == null && args[i].contains("\"")) {
					full = args[i];
					added = false;
				} else if (full != null && !args[i].contains("\"")) {
					full += " " + args[i];
					added = false;
				} else if ((full != null && args[i].contains("\""))) {
					full += " " + args[i];
					parsed.add(full.replaceAll("\"", ""));
					full = null;
					added = true;
				}

				if (i == args.length - 1 && !added) {
					parsed.add(full.replaceAll("\"", ""));
				}
			}

			// re instantiates new 'args' to fit parsed ArrayList
			args = new String[parsed.size()];
			int i = 0;
			for (String arg : parsed) {
				args[i++] = arg;
			}
		}

		return args;
	}

	/**
	 * Cleans multiple delimited command-line statements for processing.
	 * 
	 * @param commands
	 *            multiple delimited command-line statements (e.g.: "db clear -y; populate root;
	 *            ignore")
	 * @return an array of single commands that can individually be executed
	 */
	public static String[] parseStatements(String commands) {
		String[] input = commands.split(";");
		ArrayList<String> parsed = new ArrayList<>();
		for (int i = 0; i < input.length; i++) {
			input[i] = input[i].trim().replaceAll("\\s{2,}", " ");

			// skips empty commands
			if (input[i].length() > 0) {
				parsed.add(input[i]);
			}
		}
		String[] mult = new String[parsed.size()];
		for (int i = 0; i < parsed.size(); i++) {
			mult[i] = parsed.get(i);
		}
		return mult;
	}

	/**
	 * Prints a welcome page that runs when the user first starts up the jar
	 */
	public static void printSplash() {

		try {
			LinkedList<String> page = new LinkedList<>();
			page.add("\n\n");
			page.add("                                                                  _-^-_");
			page.add("                                            -            -    --   |@|   --    -         -");
			page.add(" _  _         _      _    _                                       =====");
			page.add("| |(_)  __ _ | |__  | |_ | |__    ___   _   _  ___   ___           |\\|");
			page.add("| || | / _` || '_ \\ | __|| '_ \\  / _ \\ | | | |/ __| / _ \\          |\\|");
			page.add("| || || (_| || | | || |_ | | | || (_) || |_| |\\__ \\|  __/          |\\|");
			page.add("|_||_| \\__, ||_| |_| \\__||_| |_| \\___/  \\__,_||___/ \\___|        ,/::|.._              ___");
			page.add("       |___/                                                  __,./::: ...^ ~ ~~~~~~~ / ~~");
			page.add("\nversion " + VERSION + " -- developed by Sumeet Bansal, Pierce Kelaita, and Gagan Gupta");

			Iterator<String> scroll = page.iterator();
			while (scroll.hasNext()) {
				System.out.println(scroll.next());
				Thread.sleep(50);
			}
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}