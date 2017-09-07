package driver;

import java.io.*;
import java.util.*;

import databaseModule.*;

/**
 * Runs the QueryFunctions from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.3
 */
public class AccessQRY {

	private static String sep = File.separator;

	private static String help = "\nQUERY MODULE -- POSSIBLE COMMANDS"
			+ "\n'help'\n\tgoes to the help page for 'query'"
			+ "\n\tUsage: ~$ help"
			+ "\n'compare'\n\tcompares the selected root directories and generates appropriate CSVs"
			+ "\n\tUsage: ~$ compare <path1> <path2>"
			+ "\n'exclude'\n\texcludes selected files or directories from the query"
			+ "\n\tmust be used in conjunction with the 'compare' command"
			+ "\n\tUsage: ~$ compare <path1> <path2> exclude <path> <path> ... <path>"
			+ "\n'grep'\n\tfinds every property key or value in the database matching a given pattern"
			+ "\n\tUsage: ~$ grep [toggle] <pattern>"
			+ "\n\ttoggles:\n\t\t-k\tto find matching keys\n\t\t-v\tto find matching values"
			+ "\n'find'\n\tprints the locations and values of a key/value (can be toggled) within an optional location"
			+ "\n\tUsage: ~$ find [toggle] [-l path] <pattern>"
			+ "\n\ttoggles:\n\t\t-k, --key\tto find matching keys\n\t\t-v, --value\tto find matching values"
			+ "\nType the name of another module to switch modules. Available modules: home, db, query.\n";

	/**
	 * Handles user input and delegates functionality based on first command.
	 * 
	 * @param args
	 *            command-line arguments
	 */
	public static void run(String[] args) {

		// warns that database is empty
		if (MongoManager.getCol().count() == 0) {
			System.err.println("[ERROR] Database is empty. Switch to the db module to feed files to the database.\n");
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
		case "compare":
			if (args == null) {
				System.err.println("\n[ERROR] No queries specified.\n");
				break;
			}
			runComparison(args);
			break;
		case "find":
			parseFind(args);
			break;
		case "grep":
			parseGrep(args);
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
	 * Parses args into a format that can be fed as parameters to the `grep` function.
	 * 
	 * @param args
	 *            command-line arguments
	 */
	static void parseGrep(String[] args) {
		
		String pattern = null;
		int toggle = 0;
		
		// in case of no args
		if (args == null) {
			System.err.println("\n[ERROR] No pattern specified.\n");
			return;
		}
		
		// option parsing
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-k":
			case "--key":
				toggle = 0;		// toggle set to 0 for key
				break;
			case "-v":
			case "--val":
			case "--value":
				toggle = 1;		// toggle set to 1 for value
				break;
			case "-l":
			case "--loc":
			case "--location":
				
				// location opts not supported for `grep`
				System.err.println("\n[ERROR] Invalid option: " + args[i] + "\n");
				return;
				
			default:
				
				// if pattern unassigned, assigns the first non-opt arg
				pattern = pattern == null ? args[i] : pattern;
				break;

			}
		}
		
		// checks if no pattern found
		if (pattern == null) {
			System.err.println("\n[ERROR] No pattern specified.\n");
			return;
		}
		
		// prints CLI output
		Set<String> matches = QueryFunctions.grep(pattern, toggle);
		String type = toggle == 0 ? "key" : "value";
		if (matches.size() == 0) {
			System.out.println("\nNo " + type + " matching \"" + pattern + "\" found.\n");
		} else {
			System.out.println("\nFound " + matches.size() + " matching " + type + "(s):");
			for (String prop : matches) {
				System.out.println(" - " + prop);
			}
			System.out.println();
		}
	}
	
	/**
	 * Parses args into a format that can be fed as parameters to the `find` function.
	 * 
	 * @param args
	 *            command-line arguments
	 */
	static void parseFind(String[] args) {
		
		String pattern = null, location = null;
		int toggle = 0;
		
		// in case of no args
		if (args == null) {
			System.err.println("\n[ERROR] No pattern specified.\n");
			return;
		}
		
		// option parsing
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-k":
			case "--key":
				toggle = 0;		// toggle set to 0 for key
				break;
			case "-v":
			case "--val":
			case "--value":
				toggle = 1;		// toggle set to 1 for value
				break;
			case "-l":
			case "--loc":
			case "--location":
				if (i == args.length - 2) {
					System.err.println("[ERROR] location flag `-l` requires a location argument.");
				} else {
					
					// if location unassigned, assigns the arg after -l flag
					location = location == null ? args[++i] : location;
					
				}
				break;
			default:
				
				// if pattern unassigned, assigns the first non-opt arg
				pattern = pattern == null ? args[i] : pattern;
				break;

			}
		}
		
		// checks if no pattern found
		if (pattern == null) {
			System.err.println("\n[ERROR] No pattern specified.\n");
			return;
		}
		
		String type = toggle == 0 ? "key" : "value";		// used for printing to CLI
		ArrayList<String> matches = QueryFunctions.findProp(pattern, location, toggle);
		
		// prints CLI output
		if (matches.size() == 0) {
			type = toggle == 0 ? "Key" : "Value";
			System.out.print("\n" + type + " \"" + pattern + "\" not found in database");
			if (location != null) {
				System.out.print(" at location " + location);
			}
			System.out.println(".\nUse the `grep` command to find relevant properties.\n");
		} else {
			System.out.println("\nFound " + matches.size() + " instance(s)" + " of " + type + " \"" + pattern + "\":");
			for (String path : matches) {
				System.out.println(" " + path);
			}
			System.out.println();
		}
	}

	/**
	 * Handles user input for 'compare' command, prints the results of the comparison, and writes
	 * them to a CSV file as appropriate.
	 * 
	 * @param args
	 *            command-line arguments
	 */
	private static void runComparison(String[] args) {
		ArrayList<String> queried = new ArrayList<String>();
		ArrayList<String> excluded = new ArrayList<String>();

		// uses generic 'arr' to populate appropriate List
		int arg = 0;
		ArrayList<String> arr = queried; // adds all args to 'queried'
		while (arg < args.length) {

			/*
			 * if 'exclude' keyword detected, switches refs and adds rest of args to 'excluded'
			 * List, else continues adding to 'queried'
			 */
			if (args[arg].equals("exclude")) {
				arr = excluded;
			} else {
				arr.add(args[arg]);
			}
			arg++;
		}

		// invalid query parameters (queries must be made in pairs unless internal)
		if (queried.size() == 0 || queried.size() > 1 && queried.size() % 2 != 0) {
			System.err.println("\n[ERROR] Invalid number of queries.\n");
			return;
		}

		// verifies query and exclusion paths
		for (int i = 0; i < queried.size(); i++) {
			if (queried.get(i).split("/").length > 4) {
				System.err.println("\n[ERROR] Invalid path input: " + queried.get(i) + "\n");
				return;
			}
		}
		for (int i = 0; i < excluded.size(); i++) {
			if (excluded.get(i).split("/").length > 4) {
				System.err.println("\n[ERROR] Invalid path input: " + excluded.get(i) + "\n");
				return;
			}
		}

		QueryEngine comparator = new QueryEngine();

		// adds queries to comparator
		String status = "";
		if (queried.size() == 1) {
			status += comparator.generateInternalQueries(queried.get(0));
		} else {
			for (int q = 0; q < queried.size(); q += 2) {
				status += comparator.addQuery(queried.get(q), queried.get(q + 1));
			}
		}
		if (status != null) {
			if (!status.contains("[ERROR]")) {
				System.out.println("\nLooking for properties with attributes:\n" + status);
			} else {
				System.err.println(status);
			}
		}

		// adds exclusions to comparator
		status = "";
		for (int e = 0; e < excluded.size(); e++) {
			status += comparator.exclude(excluded.get(e)) + "\n";
		}
		if (status != null) {
			if (!status.equals("") && !status.contains("[ERROR]")) {
				System.out.println("\nExcluding properties with attributes:\n" + status);
			} else {
				System.err.println(status);
			}
		}

		// aborts if unable to compare for any reason
		String result = comparator.compare();
		if (result.contains("[ERROR]")) {
			System.err.println(result);
			return;
		} else {
			System.out.println(result);
		}

		// prompts user to either enter a custom CSV name or use default name
		String writePath = System.getProperty("user.dir") + sep + "lighthouse-reports";
		new File(writePath).mkdirs();
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

		// gives a summary of the discrepancies in the query
		int diffkey = comparator.getDiscrepancies().get("key");
		int diffval = comparator.getDiscrepancies().get("value");
		int difftotal = diffkey + diffval;
		int ignored = comparator.getDiscrepancies().get("ignored");

		// in case of identical configurations
		String write = "";
		if (difftotal == 0) {
			System.out.println("\nNo discrepancies found in the directories given by the query.");
			while (true) {
				System.out.print("Would you still like to write a CSV report? (y/n): ");
				try {
					write = input.readLine();
				} catch (IOException e) {
					System.out.println("Illegal input!");
					continue;
				}

				if (write.equalsIgnoreCase("n")) {
					System.out.println();
					return;
				} else if (write.equalsIgnoreCase("y")) {
					break;
				}
			}
		} else {
			System.out.println("\nKey discrepancies\t" + diffkey);
			System.out.println("Value discrepancies\t" + diffval);
			System.out.println("Total discrepancies\t" + difftotal);
			System.out.println("Ignored properties\t" + ignored);
			System.out.println();
		}


		String filename = comparator.getDefaultName();
		while (true) {
			System.out.print("Use default CSV file name " + filename + "? (y/n): ");

			String choice = "";
			try {
				choice = input.readLine();
			} catch (IOException e) {
				System.out.println("[ERROR] Illegal input.");
				continue;
			}

			if (choice.equalsIgnoreCase("y")) {
				break;
			} else if (choice.equalsIgnoreCase("n")) {

				// checks if custom filename legal across OSes
				while (true) {
					System.out.print("Enter custom CSV file name: ");
					String custom;
					try {
						custom = input.readLine();
					} catch (IOException e) {
						System.out.println("[ERROR] Illegal input.");
						continue;
					}
					
					// regex contains all valid characters: alphanumeric and _.-
					String legal = custom.replaceAll("[^a-zA-Z0-9_ .-]", "~");

					// if custom != legal, then custom had illegal characters
					if (!custom.equals(legal)) {
						System.err.println("\n[ERROR] Illegal CSV file name.");
						System.err.println("To prevent writing corrupted files, only the following are allowed:"
								+ "letters, numbers, spaces, and the characters ._-\n");
						continue;
					} else if (custom.equals("")) {
						continue;
					} else {
						filename = custom;
						break;
					}
				} // end of custom file naming loop
				
				break;
				
			} else {
				continue;
			}
			
		} // end of main prompt loop
		
		result = comparator.writeToCSV(filename, writePath);
		System.out.println("\n" + result + "\n");		
	}
}