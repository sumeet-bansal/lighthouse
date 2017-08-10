package driver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.*;

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
			+ "'help'\n\tgoes to the help page for the general diagnostic tool\n" + "\tUsage: ADS-v" + version
			+ " # Home $ help\n"
			+ "'db'\n\tswitches to the database module to access functions that directly edit the database\n"
			+ "\tUsage: ADS-v" + version + " # Home $ db\n"
			+ "'query'\n\tswitches to the query module to access functions that analyze the contents of the databse\n"
			+ "\tUsage: ADS-v" + version + " # Home $ query\n"
			+ "\nType 'exit' at any time to exit the program\n";
	
	private static String note = "\nNote: ADS must be used in conjuntion with a working MongoDB connection"
			+ "\nTo ensure a working connection to the database, ensure 'mongod' executable is running.\n";

	/**
	 * Takes command-line arguments and delegates functionality as appropriate.
	 * 
	 * @param arr
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

		// startup
		printWelcome();
		System.out.println(help);
		System.out.println(note);
		MongoManager.connectToDatabase();
		System.out.println();

		while (true) {
			System.out.print("ADS-v" + version + " # Home $ ");
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
				break;
			}

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
				System.err.println("\nInvalid input. Use the 'help' command for details on usage.");
				return;

			}

			// prints prompt to CLI and takes in user input
			System.out.print("ADS-v" + version + " # " + branchName + " $ ");
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
		ArrayList<String> lines = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			lines.add("");
		}
		lines.add("       db         88888888ba,     ad88888ba ");
		lines.add("      d88b        88      `\"8b   d8\"     \"8b");
		lines.add("     d8'`8b       88        `8b  Y8,        ");
		lines.add("    d8'  `8b      88         88  `Y8aaaaa,  ");
		lines.add("   d8YaaaaY8b     88         88    `\"\"\"\"\"8b,");
		lines.add("  d8\"\"\"\"\"\"\"\"8b    88         8P          `8b");
		lines.add(" d8'        `8b   88      .a8P   Y8a     a8P");
		lines.add("d8'          `8b  88888888Y\"'     \"Y88888P\" ");

		for (int i = 0; i < 3; i++) {
			lines.add("");
		}
		lines.add("version " + version);
		lines.add("");
		lines.add("Developed by Pierce Kelaita, Sumeet Bansal, and Gagan Gupta");
		for (int i = 0; i < 3; i++) {
			lines.add("");
		}

		for (String str : lines) {
			System.out.println(str);
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