package driver;

import java.io.*;
import java.util.*;

import com.mongodb.MongoNamespace;

import databaseModule.*;

/**
 * Runs DbFunctions from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.3
 */
public class AccessDB {

	private static final String help = "\nDATABASE MODULE -- POSSIBLE COMMANDS"
			+ "\n'help'\n\tgoes to the help page for 'db'"
			+ "\n\tUsage: ~$ help"
			+ "\n'populate'\n\tpopulates the database with the given files"
			+ "\n\tUsage: ~$ populate <root directory>"
			+ "\n'info'\n\tprovides info about the contents of the database"
			+ "\n\tUsage: ~$ info"
			+ "\n'list'\n\tprints the structure of the database at optional branches and levels"
			+ "\n\tUsage: ~$ list [path] [level (1+)]"
			+ "\n\tNote: the higher the level, the deeper the list."
			+ "\n'ignore'\n\tprovides info about ignored properties, can additionally ignore further properties"
			+ "\n\tUsage: ~$ ignore [toggle] [-l path] [property] ... [property]"
			+ "\n\ttoggles:"
			+ "\n\t\t-t, --true\tto ignore the following properties"
			+ "\n\t\t\t\talt.: -i, --ignore"
			+ "\n\t\t-f, --false\tto acknowledge the following properties"
			+ "\n\t\t\t\talt.: -a, --acknowledge"
			+ "\n'clear'\n\tclears the database"
			+ "\n\tUsage: ~$ clear"
			+ "\nType the name of another module to switch modules. Available modules: home, db, query.\n";

	/**
	 * Handles user input and delegates functionality based on first command
	 * 
	 * @param args
	 *            command-line arguments
	 */
	public static void run(String[] args) {

		// warns that database is empty
		if (MongoManager.getCol().count() == 0 && !args[0].equals("populate") && !args[0].equals("help")) {
			System.err.println("Database is empty. Use the 'populate' command to feed files to the database.\n");
			return;
		}

		String cmd = args[0];
		if (args.length > 1) {
			args = Arrays.copyOfRange(args, 1, args.length);
		} else {
			args = null;
		}

		// handles command line input
		switch (cmd) {
		case "populate":

			// checks if directory specified
			if (args == null) {
				System.err.println(help);
				return;
			}

			// adds all specified directories to database
			for (int i = 0; i < args.length; i++) {
				int popcount = DbFunctions.populate(args[i]);
				System.out.println("\nAdded " + popcount + " properties to database.\n");
			}

			break;
		case "clear":
			boolean permission = false;
			if (args != null && (args[0].equals("-y") || args[0].equals("--yes"))) {
				permission = true;
			}
			permission = !permission ? promptClear() : permission;
			if (permission) {
				long n = MongoManager.clearDB();
				MongoNamespace col = MongoManager.getCol().getNamespace();
				System.out.println("\nCleared " + n + " properties from collection " + col + "\n");
			}
			break;
		case "info":
			DbFunctions.printInfo();
			break;
		case "list":
			int level = -1;
			String path = "";
			if (args != null && args.length == 1) {
				if (validateLevel(args[0])) {
					level = Integer.parseInt(args[0]);
				} else {
					path = args[0];
				}
			} else if (args != null && args.length > 1) {
				path = args[0];
				level = validateLevel(args[1]) ? Integer.parseInt(args[1]) : -1;
			}
			System.out.println();
			DbFunctions.printStructure(path, level);
			System.out.println();
			break;
		case "ignore":

			if (args != null) {
				parseIgnore(args);
			}

			Set<String> ignored = DbFunctions.getIgnored();
			if (ignored.size() == 0) {
				System.out.println("\nNo properties set to be ignored.\n");
				break;
			}
			System.out.println("\n" + ignored.size() + " properties set to be ignored:");

			Iterator<String> iter = ignored.iterator();
			while (iter.hasNext()) {
				System.out.println(" - " + iter.next());
			}
			System.out.println();

			break;
		case "man":
		case "help":
			System.out.println(help);
			break;
		default:
			System.err.println("Invalid input. Use the 'help' command for details on usage.\n");
		}

	}

	/**
	 * Parses args into a format that can be fed as parameters to the `ignore` function.
	 * 
	 * @param args
	 *            command-line arguments
	 */
	private static void parseIgnore(String[] args) {

		String location = null;
		boolean toggle = true;
		Set<String> toIgnore = new HashSet<>();

		// option parsing
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-t":
			case "-i":
			case "--true":
			case "--ig":
			case "--ignore":
				toggle = true; 		// toggle set to true to ignore
				break;
			case "-f":
			case "-a":
			case "--false":
			case "--ack":
			case "--acknowledge":
				toggle = false; 	// toggle set to false to acknowledge
				break;
			case "-l":
			case "--loc":
			case "--location":
				if (i > args.length - 2) {
					System.err.println("\n[ERROR] location flag `-l` requires a location argument.");
					return;
				} else {

					// if location unassigned, assigns the arg after -l flag
					location = location == null ? args[++i] : location;

				}
				break;
			default:

				// if non-opt arg, adds to Set of properties to ignore
				toIgnore.add(args[i]);
				break;

			}
		}

		if (toIgnore.size() == 0) {
			System.err.println("\n[ERROR] No properties specified.");
			return;
		}

		String result = DbFunctions.ignore(location, toIgnore, toggle);
		if (result != null) {
			System.err.println("\n" + result);
		}

	}

	/**
	 * Prompts user to verify that the database should be cleared.
	 * 
	 * @return true if permission has been given to clear the database, else false
	 */
	private static boolean promptClear() {
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		System.out.println();

		// repeatedly queries in case of invalid input
		while (true) {
			String choice = "";
			System.out.print("Clear entire database? (y/n): ");
			try {
				choice = input.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (choice.equalsIgnoreCase("y")) {
				return true;
			} else if (choice.equalsIgnoreCase("n")) {
				System.out.println();
				return false;
			} else {
				continue;
			}
		}
	}

	/**
	 * Checks if 'list' level input is valid.
	 * 
	 * @param arg
	 *            command-line arguments
	 * @return true if the level is valid, else false
	 */
	private static boolean validateLevel(String arg) {

		// verify that level is parseable int
		try {
			Integer.parseInt(arg);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
}