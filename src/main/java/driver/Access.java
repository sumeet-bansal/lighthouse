package driver;

import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.*;

import com.mongodb.MongoSocketWriteException;
import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoTimeoutException;

import databaseModule.MongoManager;

/**
 * Runs the complete diagnostic tool from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.2
 */
public class Access {

	static String version = "1.2"; // displayed on CLI in every module
	static Scanner s = new Scanner(System.in);

	private static String help = "\nHOME PAGE -- POSSIBLE COMMANDS \n\n"
			+ "'help'\n\tgoes to the help page for the general diagnostic tool\n" + "\tUsage: lighthouse-v" + version
			+ " # Home $ help\n"
			+ "'db'\n\tswitches to the database module to access functions that directly edit the database\n"
			+ "\tUsage: lighthouse-v" + version + " # Home $ db\n"
			+ "'query'\n\tswitches to the query module to access functions that analyze the contents of the databse\n"
			+ "\tUsage: lighthouse-v" + version + " # Home $ query\n"
			+ "\n      - Type 'exit' at any time to exit the program\n";

	private static String note = "\nNote: lighthouse must be used in conjuntion with a working MongoDB connection."
			+ "\nTo ensure a working connection to the database, ensure the executable 'mongod' is running.\n";

	/**
	 * Takes command-line arguments and delegates functionality as appropriate
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
			printWelcome();
			System.out.println(help);
			System.out.println(note);
			MongoManager.connectToDatabase();
			System.out.println();

			// main loop
			while (true) {
				System.out.print("lighthouse-v" + version + " # Home $ ");
				String result = s.nextLine();
				if (result.equals("")) {
					continue;
				}

				// delegates functionality as appropriate
				switch (result) {
				case "help":
					System.out.println(help);
					break;
				case "home":
					break;
				case "exit":
					s.close();
					System.exit(0);
				default:
					runModule(result);
				}

			}

			// check mongo connection
		} catch (MongoTimeoutException t) {
			System.out.println(
					"[DATABASE MESSAGE] Server connection timed out. Make sure a 'mongod' instance is running.");
			System.out.println("\nExiting with error code 1...");
			System.exit(1);
		} catch (MongoSocketWriteException | MongoSocketOpenException s) {
			System.out.println("[DATABASE MESSAGE] Mongo connection interrupted. Check if 'mongod' is still running.");
			System.out.println("\nExiting with error code 2...");
			System.exit(2);
		}
	}

	/**
	 * Runs the user-specified module and takes user input via scanner
	 * 
	 * @param branch
	 *            command corresponding to module
	 */
	public static void runModule(String branch) {

		String branchName = new String();
		while (true) {

			// checks input and sets branch name for prompt
			switch (branch) {
			case "db":
				branchName = "Database";
				break;
			case "query":
				branchName = "Query";
				break;
			default:
				System.err.println(
						"Command '" + branch + "' not recognized. Use the 'help' command for details on usage.\n");
				return;

			}

			// prints prompt to CLI and takes in user input
			System.out.print("lighthouse-v" + version + " # " + branchName + " $ ");
			String result = s.nextLine();
			String[] args = result.split(" ");
			switch (args[0]) {
			case "":
				continue;
			case "home":
				return;
			case "exit":
				s.close();
				System.exit(0);
			}

			// check to see if user wants to switch module
			if (!args[0].equals(branch) && (args[0].equals("db") || args[0].equals("query"))) {
				branch = args[0];
				continue;
			}

			// runs user-specified module
			switch (branch) {
			case "db":
				AccessDB.run(args);
				break;
			case "query":
				AccessQRY.run(args);
				break;
			}

		}
	}

	/**
	 * Prints a welcome page that runs when the user first starts up the jar
	 */
	public static void printWelcome() {
		StringBuffer welcome = new StringBuffer(" \n \n");

		welcome.append("                                                         " + "         _-^-_\n");
		welcome.append(
				"                                            -            " + "-    --   |@|   --    -            -\n");
		welcome.append(" _  _         _      _    _                              " + "         =====\n");
		welcome.append("| |(_)  __ _ | |__  | |_ | |__    ___   _   _  ___   ___ " + "          |\\|\n");
		welcome.append("| || | / _` || '_ \\ | __|| '_ \\  / _ \\ | | | |/ __| / _ \\" + "          |\\|\n");
		welcome.append("| || || (_| || | | || |_ | | | || (_) || |_| |\\__ \\|  __/" + "          |\\|\n");
		welcome.append("|_||_| \\__, ||_| |_| \\__||_| |_| \\___/  \\__,_||___/ \\___|"
				+ "        ,/::|.._               ___\n");
		welcome.append(
				"       |___/                                             " + "     __,./::: ...^ ~ ~~ ~ ~~~ /  ~`~\n");

		welcome.append(" \n \n \n \n");
		welcome.append("version " + version + "\n \n");
		welcome.append("developed by Pierce Kelaita, Sumeet Bansal, and Gagan Gupta");
		welcome.append(" \n \n \n \n");

		for (String line : welcome.toString().split("\\n")) {
			System.out.println(line);
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
}