package driver;

import java.io.*;
import java.util.*;

import databaseModule.*;

/**
 * Runs the QueryFunctions from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.1
 */
public class AccessQRY {

	private static String version = Access.version;
	private static String sep = File.separator;

	private static String help = "\nQUERY MODULE -- POSSIBLE COMMANDS\n"
			+ "\n'help'\n\tgoes to the help page for 'query'" + "\n\tUsage: lighthouse-" + version + " # Query $ help"
			+ "\n'compare'\n\tcompares the selected root directories and generates appropriate CSVs"
			+ "\n\tUsage: lighthouse-v" + version + " # Query $ compare <path1> <path2>"
			+ "\n'exclude'\n\texcludes selected files or directories from the query"
			+ "\n\tmust be used in conjunction with the 'compare' command" + "\n\tUsage: lighthouse-v" + version
			+ " # Query $ compare <path1> <path2> exclude <path> <path> ... <path>"
			+ "\n'grep'\n\tfinds every property key and value in the database that contains a given pattern"
			+ "\n\tUsage: lighthouse-v" + version + " # Query $ grep -k / -v <pattern>"
			+ "\n'find'\n\tprints the locations and values of a user-given key or value at a specified location, if given"
			+ "\n\tUsage: lighthouse-v" + version + " # Query $ find -k / -v <key or value name> [-l (location path)]\n"
			+ "\n      - Type the command for another module ('db', 'home') to go to that module"
			+ "\n      - Type 'exit' at any time to exit the program\n";

	/**
	 * Prints an error message that help users with 'find' and 'grep' commands
	 * 
	 * @param term
	 *            find or grep
	 */
	private static String searchHelp(String term) {
		String str = "\n - Use the flag -k to search for keys or the flag -v for values";
		if (term.equals("grep")) {
			str += ("\n\n - Usage: lighthouse-v" + version + " # Query $ grep (-k / -v) <pattern>\n");
		} else {
			str += ("\n - Use flag -l for location parameter to find results only in a certain location (optional)"
					+ "\n\n - Usage: lighthouse-v" + version
					+ " # Query $ find (-k / -v) <key or value name> [-l (location path)]\n");
		}
		return str;
	}

	/**
	 * Handles user input and delegates functionality based on first command
	 * 
	 * @param args
	 *            command-line arguments
	 */
	public static void run(String[] args) {

		// let user know if database is empty
		boolean searchCommand = args[0].equals("grep") || args[0].equals("find");
		if (searchCommand || args[0].equals("compare")) {
			if (MongoManager.getCol().count() == 0) {
				System.out.println("Database is empty. Switch to the database module using 'db' and use"
						+ "\nthe 'populate' command to feed files to the database.\n");
				return;
			}
		}

		// consolidate 'find' and 'grep' input into search query
		ArrayList<String> searchQuery = new ArrayList<String>();
		if (searchCommand) {
			searchQuery = generateSearchQuery(args);
			if (searchQuery == null) {
				System.err.println(searchHelp(args[0]));
				return;
			}
		}

		// handle command line input
		switch (args[0]) {
		case "compare":
			runComparison(args);
			break;
		case "find":
			printSearchResults(searchQuery, "find");
			break;
		case "grep":
			printSearchResults(searchQuery, "grep");
			break;
		case "help":
			System.out.println(help);
			break;
		default:
			System.err.println(
					"Command '" + args[0] + "' not recognized. Use the 'help' command for details on usage.\n");
		}

	}

	/**
	 * Checks if input for 'find' and 'grep' commands is valid and consolidates the
	 * input into an ArrayList that can be used by printSearchResults()
	 * 
	 * @param args
	 *            command-line arguments
	 * @return command-line arguments parsed into an ArrayList by type
	 */
	private static ArrayList<String> generateSearchQuery(String[] args) {
		ArrayList<String> searchQuery = new ArrayList<String>();

		if (args.length < 3) {
			return null;
		}

		// make sure user flags for keys or values
		if (!(args[1].equals("-k") || args[1].equals("-v"))) {
			return null;
		} else {
			searchQuery.add(args[1]);
		}

		// iterate through user input to determine search term and location, if given
		String searchTerm = "";
		String location = "";
		boolean hasLocation = false;

		for (int i = 2; i < args.length; i++) { // TODO javadoc
			if (args[0].equals("find")) {
				if (!hasLocation) {
					if (args[i].equals("-l") && args.length > i + 1) {
						hasLocation = true;
						continue;
					} else {
						searchTerm += args[i] + " ";
					}
				} else {
					location += args[i];
				}
			} else {
				searchTerm += args[i] + " ";
			}
		}
		searchTerm = searchTerm.substring(0, searchTerm.length() - 1);
		searchQuery.add(searchTerm);
		if (hasLocation) {
			searchQuery.add(location);
		}
		return searchQuery;
	}

	/**
	 * Handles user input for 'find' / 'grep' commands and prints the results of
	 * their search
	 * 
	 * @param searchQuery
	 *            parsed command-line arguemtns
	 * @param function
	 *            "find" or "grep"
	 */
	private static void printSearchResults(ArrayList<String> searchQuery, String function) {

		// Parse search query specifications
		String flag = searchQuery.get(0);
		String searchTerm = searchQuery.get(1);
		String location = null;
		if (function.equals("find") && searchQuery.size() > 2) {
			location = searchQuery.get(2);
		}

		// Check user given flag to see if user wants to search for keys or values
		int searchType = -1;
		String propType = "";
		if (flag.equals("-k")) {
			searchType = 0; // search for keys
			propType = "key";
		} else {
			searchType = 1; // search for values
			propType = "value";
		}

		// Print matching properties based on function specification
		if (function.equals("find")) {
			ArrayList<String> pathList = QueryFunctions.findProp(searchTerm, location, searchType);
			if (pathList.size() == 0) {
				System.out.print("\n" + propType + " \"" + searchTerm + "\" not found in database");
				if (location != null) {
					System.out.print(" at location " + location);
				}
				System.out.print(".\n");
			} else {
				String s = "s";
				if (pathList.size() == 1) {
					s = "";
				}
				System.out.println("\nFound " + pathList.size() + " instance" + s + " of " + propType + " \""
						+ searchTerm + "\":");
				for (String path : pathList) {
					System.out.println(" - " + path);
				}
			}
		} else if (function.equals("grep")) {
			Set<String> propSet = QueryFunctions.grep(searchTerm, searchType);
			if (propSet != null && propSet.size() == 0) {
				System.out.print("\nNo " + propType + " containing \"" + searchTerm + "\" found in database");
				System.out.print(".\n");
			} else {
				System.out.println("\nFound " + propSet.size() + " matching " + propType + ":");
				for (String prop : propSet) {
					System.out.println(" - " + prop);
				}
			}
		}
		System.out.println();
	}

	/**
	 * Handles user input for 'compare' command, prints the results of the
	 * comparison, and writes them to a CSV file as appropriate
	 * 
	 * @param args
	 *            command-line arguments
	 */
	private static void runComparison(String[] args) {
		ArrayList<String> queried = new ArrayList<String>();
		ArrayList<String> excluded = new ArrayList<String>();
		// uses generic 'arr' to populate appropriate List
		int i = 1;
		ArrayList<String> arr = queried; // adds all args to 'queried'
		while (i < args.length) {

			/*
			 * if 'exclude' keyword detected, switches refs and adds rest of args to
			 * 'excluded' List, else continues adding to 'queried'
			 */
			if (args[i].equals("exclude")) {
				arr = excluded;
			} else {
				arr.add(args[i]);
			}
			i++;
		}

		// invalid query parameters
		if (queried.size() == 0 || queried.size() > 1 && queried.size() % 2 != 0) {
			System.err.println(help);
			return;
		}

		// adds queries to QueryFunctions instance and compares
		QueryFunctions c = new QueryFunctions();
		if (queried.size() == 1) {
			c.generateInternalFilters(queried.get(0));
		} else {
			for (int q = 0; q < queried.size(); q += 2) {
				c.addQuery(queried.get(q), queried.get(q + 1));
			}
		}
		for (int e = 0; e < excluded.size(); e++) {
			c.exclude(excluded.get(e));
		}
		if (!c.compare()) {
			return;
		}

		// prompts user to either enter a custom CSV name or use default name
		String writePath = System.getProperty("user.home") + sep + "Documents" + sep + "lighthouse-reports";
		new File(writePath).mkdirs(); // TODO user - workingdir/reports
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

		// gives a summary of the discrepancies in the query
		String writeZero = ""; // holds user input
		int diffkey = c.getDiscrepancies()[0];
		int diffval = c.getDiscrepancies()[1];
		int difftotal = c.getDiscrepancies()[2];
		if (difftotal == 0) {
			System.out.println("\nNo discrepancies found in the directories given by the query.");
			while (true) {
				System.out.print("Would you still like to write a CSV report? (y/n): ");
				try {
					writeZero = input.readLine();
				} catch (IOException e) {
					System.out.println("Illegal input!");
					continue;
				}
				if (writeZero.equalsIgnoreCase("n")) {
					System.out.println();
					return;
				} else if (writeZero.equalsIgnoreCase("y")) {
					break;
				}
			}
		} else {
			System.out.println("\nKey discrepancies\t" + diffkey);
			System.out.println("Value discrepancies\t" + diffval);
			System.out.println("Total discrepancies\t" + difftotal);
		}

		String result = ""; // holds user input
		System.out.println();
		while (true) {
			System.out.print("Use default CSV file name " + c.getDefaultName() + "? (y/n): ");
			try {
				result = input.readLine();
				if (result.equalsIgnoreCase("y")) {
					c.writeToCSV(writePath);
					c.clearQuery();
					return;
				} else if (result.equalsIgnoreCase("n")) {
					// checks if custom filename legal across OSes
					String customName;
					while (true) {
						System.out.print("Enter custom CSV file name: ");
						String test = input.readLine();
						String legal = test.replaceAll("[^a-zA-Z0-9_ .-]", "~");
						if (!test.equals(legal)) {
							System.out.println("\nERROR: illegal CSV file name.");
							System.out.println("To prevent writing corrupted files, only letters,"
									+ "\nnumbers, spaces, and the characters . _ - ~ are allowed.\n");
							continue;
						} else if (test.equals("")) {
							continue;
						} else {
							customName = test;
							c.writeToCSV(customName, writePath);
							return;
						}
					}
				} else {
					continue;
				}
			} catch (IOException e) {
				System.out.println("Illegal input!");
				continue;
			}
		}
	}
}