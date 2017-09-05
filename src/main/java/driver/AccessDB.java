package driver;

import java.io.*;

import com.mongodb.MongoNamespace;

import databaseModule.*;

/**
 * Runs DbFunctions from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.1
 */
public class AccessDB {

	private static final String help = "\nDATABASE MODULE -- POSSIBLE COMMANDS"
			+ "\n'help'\n\tgoes to the help page for 'db'" + "\n\tUsage: ~$ help"
			+ "\n'populate'\n\tpopulates the database with the given files" + "\n\tUsage: ~$ populate <root directory>"
			+ "\n'info'\n\tprovides info about the contents of the database" + "\n\tUsage: ~$ info"
			+ "\n'list'\n\tprints the structure of the database at optional branches and levels"
			+ "\n\tUsage: ~$ list [path] [level (1+)]" + "\n\tNote: the higher the level, the deeper the list."
			+ "\n'clear'\n\tclears the database" + "\n\tUsage: ~$ clear"
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

		// handles command line input
		switch (args[0]) {
		case "populate":

			// checks if directory specified
			if (args.length < 2) {
				System.err.println(help);
				return;
			}

			// adds all specified directories to database
			for (int i = 1; i < args.length; i++) {
				int popcount = DbFunctions.populate(args[1]);
				System.out.println("\nAdded " + popcount + " properties to database.\n");
			}

			break;
		case "clear":
			boolean permission = false;
			if (args.length > 1 && (args[1].equals("-y") || args[1].equals("--yes"))) {
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
			if (args.length == 2) {
				if (validateLevel(args[1])) {
					level = Integer.parseInt(args[1]);
				} else {
					path = args[1];
				}
			} else if (args.length > 2) {
				path = args[1];
				level = validateLevel(args[2]) ? Integer.parseInt(args[2]) : -1;
			}
			System.out.println();
			DbFunctions.printStructure(path, level);
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