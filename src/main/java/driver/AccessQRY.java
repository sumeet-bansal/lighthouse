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
			+ "\n'help'\n\tgoes to the help page for 'query'" + "\n\tUsage: ADS-" + version + " # Query $ help"
			+ "\n'compare'\n\tcompares the selected root directories and generates appropriate CSVs"
			+ "\n\tUsage: ADS-v" + version + " # Query $ compare <path1> <path2>"
			+ "\n'exclude'\n\texcludes selected files or directories from the query"
			+ "\n\tmust be used in conjunction with the 'compare' command" + "\n\tUsage: ADS-v" + version
			+ " # Query $ compare <path1> <path2> exclude <path> <path> ... <path>"
			+ "\n'grep'\n\tfinds every property key and value in the database that contains a given pattern"
			+ "\n\tUsage: ADS-v" + version + " # Query $ grep -k / -v <pattern>"
			+ "\n'find'\n\tprints the locations and values of a user-given key or value at a specified location, if given"
			+ "\n\tUsage: ADS-v" + version + " # Query $ find -k / -v <key or value name> [-l (location path)]\n"
			+ "\n      - Type the command for another module ('db', 'home') to go to that module"
			+ "\n      - Type 'exit' at any time to exit the program\n";

	/**
	 * Queries the database and generates CSV files containing comparison data.
	 * 
	 * @param args
	 *            command-line arguments
	 */
	public static void run(String[] args) {

		ArrayList<String> queried = new ArrayList<String>();
		ArrayList<String> excluded = new ArrayList<String>();

		/*
		 * consolidates arguments past the 3rd argument into a single argument so 'find'
		 * and 'grep' can search for a string with spaces. If location given, handles
		 * location args
		 */
		if ((args[0].equals("grep") || args[0].equals("find"))) {
			if (args.length > 3) {
				String findWithSpaces = "";
				String location = "";
				boolean hasLocation = false;
				for (int i = 2; i < args.length; i++) {
					if (args[0].equals("find")) {
						if (args[i - 1].equals("-l")) {
							location = args[i];
							hasLocation = true;
							break;
						} else if (!args[i].equals("-l")) {
							findWithSpaces += args[i] + " ";
						}
					} else {
						findWithSpaces += args[i] + " ";
					}
				}
				findWithSpaces = findWithSpaces.substring(0, findWithSpaces.length() - 1);
				args[2] = findWithSpaces;
				for (int i = 3; i < args.length; i++) {
					args[i] = null;
				}
				if (hasLocation) {
					args[3] = "-l";
					args[4] = location;
				}
			}
		}

		// switch statement to handle command line input
		switch (args[0]) {
		case "compare":
			if (MongoManager.getCol().count() == 0) {
				System.out.println("\nDatabase is empty.\n");
				return;
			}

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
			break;
		case "find":
			if (MongoManager.getCol().count() == 0) {
				System.out.println("\nDatabase is empty.\n");
				return;
			} else {
				if (args.length < 2) {
					System.err.println(help);
					return;
				}

				// Check user given flag to see if user wants to search for keys or values
				int searchFor = -1;
				String propType = "";
				if (!(args[1].equals("-k") || args[1].equals("-v"))) {
					System.err.println(
							"\nPlease specify whether to search for keys or values using the flag -k for keys or -v for values");
					System.err.println("Usage: ADS-v" + version
							+ " # Query $ find -k / -v <key or value name> [-l (location path)]\n");
					return;
				} else if (args[1].equals("-k")) {
					searchFor = 0; // search for keys
					propType = "key";
				} else {
					searchFor = 1; // search for values
					propType = "value";
				}

				// Determine user-specified location, if given
				String location = null;
				if (args.length > 4) {
					if (args[3] != null && args[4] != null) {
						if (!args[3].equals("-l")) {
							System.err.println("\nPlease use the -l flag to specify location" + "\nUsage: ADS-v"
									+ version + "# Query $ grep -k / -v <pattern> -l <location path>");
							return;
						} else {
							location = args[4];
							System.out.println("\nLocation: " + location);
						}
					}
				} else if (args.length < 3) {
					System.err.println(help);
					return;
				}

				// Print matching properties
				ArrayList<String> pathList = QueryFunctions.findProp(args[2], location, searchFor);
				if (pathList.size() == 0) {
					System.out.print("\n" + propType + " \"" + args[2] + "\" not found in database");
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
							+ args[2] + "\":");
					for (String path : pathList) {
						System.out.println(" - " + path);
					}
				}
				System.out.println();
			}
			return;
		case "grep":
			if (MongoManager.getCol().count() == 0) {
				System.out.println("\nDatabase is empty.\n");
				return;
			} else {
				if (args.length < 2) {
					System.err.println(help);
					return;
				}

				// Check user given flag to see if user wants to search for keys or values
				int searchFor = -1;
				String propType = "";
				if (!(args[1].equals("-k") || args[1].equals("-v"))) {
					System.err.println(
							"\nPlease specify whether to search for keys or values using the flag -k for keys or -v for values");
					System.err.println("Usage: ADS-v" + version + " # Query $ grep -k / -v <pattern>\n");
					return;
				} else if (args[1].equals("-k")) {
					searchFor = 0; // search for keys
					propType = "keys";
				} else {
					searchFor = 1; // search for values
					propType = "values";
				}

				// Print matching properties
				Set<String> propSet = null;
				if (args.length > 2) {
					propSet = QueryFunctions.grep(args[2], searchFor);
				} else {
					System.err.println(help);
					return;
				}

				if (propSet != null && propSet.size() == 0) {
					System.out.print("\nNo " + propType + " containing \"" + args[2] + "\" found in database");
					System.out.print(".\n");
				} else {
					System.out.println("\nFound " + propSet.size() + " matching " + propType + ":");
					for (String prop : propSet) {
						System.out.println(" - " + prop);
					}
				}
				System.out.println();

			}
			return;
		case "help":
			System.out.println(help);
			return;
		default:
			System.err.println(
					"Command '" + args[0] + "' not recognized. Use the 'help' command for details on usage.\n");
			return;
		}

		// adds queries to QueryFunctions instance and compares
		QueryFunctions c = new QueryFunctions();
		if (queried.size() == 1) {
			c.generateInternalFilters(queried.get(0));
		} else {
			for (int i = 0; i < queried.size(); i += 2) {
				c.addQuery(queried.get(i), queried.get(i + 1));
			}
		}
		for (int i = 0; i < excluded.size(); i++) {
			c.exclude(excluded.get(i));
		}
		if (!c.compare()) {
			return;
		}

		// prompts user to either enter a custom CSV name or use default name
		String writePath = System.getProperty("user.home") + sep + "Documents" + sep + "ADS Reports";
		new File(writePath).mkdirs();
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