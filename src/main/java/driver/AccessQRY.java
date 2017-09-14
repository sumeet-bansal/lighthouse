package driver;

import java.io.*;
import java.util.*;

import queryModule.QueryEngine;
import queryModule.QueryFunctions;

/**
 * Runs the QueryFunctions from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.3
 */
public class AccessQRY {

	private static final String sep = File.separator;
	private static final int MAX_SPACING = 100;
	private static final int DEFAULT_SPACING = 4;

	private static String help = "\nQUERY MODULE -- POSSIBLE COMMANDS" + "\n'help'\n\tgoes to the help page for 'query'"
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
		if (SQLiteManager.getSize() == 0) {
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
			parseCompare(args);
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
			System.err.println("[ERROR] invalid input: " + cmd + "\nUse the 'help' command for details on usage.\n");
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
				toggle = 0; // toggle set to 0 for key
				break;
			case "-v":
			case "--val":
			case "--value":
				toggle = 1; // toggle set to 1 for value
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
			for (String match : matches) {
				System.out.println(" - " + match);
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
				toggle = 0; // toggle set to 0 for key
				break;
			case "-v":
			case "--val":
			case "--value":
				toggle = 1; // toggle set to 1 for value
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

		String type = toggle == 0 ? "key" : "value"; // used for printing to CLI
		List<Map<String, String>> matches = QueryFunctions.findProp(pattern, location, toggle);

		// prints CLI output
		if (matches.isEmpty()) {
			type = toggle == 0 ? "Key" : "Value";
			System.out.print("\n" + type + " \"" + pattern + "\" not found in database");
			if (location != null) {
				System.out.print(" at location " + location);
			}
			System.out.println(".\nUse the `grep` command to find relevant properties.\n");
		} else {
			System.out.println("\nFound " + matches.size() + " instance(s)" + " of " + type + " \"" + pattern + "\":");
			Iterator<Map<String, String>> iter = matches.iterator();
			while (iter.hasNext()) {
				Map<String, String> match = iter.next();
				String path = "PATH: " + match.get("path");
				String pair = type.equalsIgnoreCase("key") ? "value" : "key";
				pair = pair.toUpperCase() + ": " + match.get(pair);
				int spaces = path.length() + pair.length();
				spaces = spaces < MAX_SPACING ? MAX_SPACING - spaces : DEFAULT_SPACING;
				String spacing = new String(new char[spaces]).replace('\0', ' ');
				System.out.println(" " + path + spacing + pair);
			}
			System.out.println();
		}
	}

	/**
	 * Handles user input for 'compare' command, prints the results of the comparison, and writes
	 * them to a CSV file as appropriate.
	 * 
	 * Runs through the entire process.
	 * 
	 * @param args
	 *            command-line arguments
	 */
	private static void parseCompare(String[] args) {
		ArrayList<String> queries = new ArrayList<String>();
		ArrayList<String> exclusions = new ArrayList<String>();

		// uses reference 'arr' to populate appropriate List
		int arg = 0;
		ArrayList<String> arr = queries; // adds all args to 'queried'
		while (arg < args.length) {

			/*
			 * if 'exclude' keyword detected, switches references and adds rest of args to
			 * 'excluded' List, else continues adding to 'queried'
			 */
			if (args[arg].equals("exclude")) {
				arr = exclusions;
			} else {
				arr.add(args[arg]);
			}
			arg++;
		}

		// invalid query parameters (queries must be made in pairs unless internal)
		if (queries.size() == 0 || queries.size() > 1 && queries.size() % 2 != 0) {
			System.err.println("\n[ERROR] Invalid number of queries.\n");
			return;
		}

		// verifies query and exclusion paths
		for (int i = 0; i < queries.size(); i++) {
			if (queries.get(i).split("/").length > 4) {
				System.err.println("\n[ERROR] Invalid path input: " + queries.get(i) + "\n");
				return;
			}
		}
		for (int i = 0; i < exclusions.size(); i++) {
			if (exclusions.get(i).split("/").length > 4) {
				System.err.println("\n[ERROR] Invalid path input: " + exclusions.get(i) + "\n");
				return;
			}
		}

		QueryEngine comparator = new QueryEngine();

		// tracks all queries added to comparator
		ArrayList<ArrayList<Map<String, String>>> added = new ArrayList<>();

		// adds queries to comparator
		if (queries.size() == 1) {
			String path = cleanPath(queries.get(0));

			// checks that the internal query is not an a file level (an invalid depth)
			if (SQLiteManager.generatePathFilter(path).get("filename") != null) {
				System.err.println("[ERROR] Internal queries cannot be at the lowest level (i.e. full file paths).");
				return;
			}

			// generates all possible subpaths for given path
			ArrayList<String> subpaths = comparator.generateInternalQueries(path);

			// in case of insufficient subpaths for valid paths (must be at least two for a
			// comparison)
			if (subpaths.size() < 2) {
				System.err.println("[ERROR] Directory must contain at least 2 files or subdirectories.");
				return;
			}

			// query each unique pair of paths
			for (int i = 0; i < subpaths.size() - 1; i++) {
				for (int j = i + 1; j < subpaths.size(); j++) {
					added.add(comparator.addQuery(subpaths.get(i), subpaths.get(j)));
				}
			}

		} else {

			// cycles through every pair of queries
			for (int q = 0; q < queries.size(); q += 2) {
				String pathL = cleanPath(queries.get(q));
				String pathR = cleanPath(queries.get(q + 1));

				// different split lengths means paths at different levels
				if (pathL.split("/").length != pathR.split("/").length) {
					System.err.println("\n[ERROR] Paths must be at the same specified level.\n");
					return;
				}

				added.add(comparator.addQuery(pathL, pathR));
			}

		}

		// stdout for added queries
		String failures = "";
		System.out.println("\nQueueing queries for properties with attributes:\n");
		for (ArrayList<Map<String, String>> query : added) {

			String queryL = "\t" + formatAsJSON(query.get(0));
			String queryR = "\t" + formatAsJSON(query.get(1)) + "\n";

			// comparator adds a key "error" to any failed additions
			if (query.get(0).containsKey("error")) {
				failures += "\n[ERROR] Unable to add properties with attributes:\n";
				failures += queryL + "\n" + queryR + "\n";
			} else {
				System.out.println(queryL);
				System.out.println(queryR);
			}

		}
		if (!failures.equals("")) {
			System.err.println(failures);
		}

		// adds exclusions to comparator
		if (!exclusions.isEmpty()) {
			System.out.println("Excluding properties with attributes:\n");
		}
		for (int e = 0; e < exclusions.size(); e++) {
			Map<String, String> excluded = comparator.exclude(exclusions.get(e));
			System.out.println("\t" + formatAsJSON(excluded) + "\n");
		}

		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

		// runs comparator and stdout for query discrepancies summary
		Map<String, Integer> compstats = comparator.run();
		if (compstats.get("queried") == 0) {
			System.err.println("[ERROR] No matching properties found.\n");
			return;
		}
		System.out.print("Found " + compstats.get("queried") + " properties ");

		if (!exclusions.isEmpty()) {
			System.out.print("and excluded " + compstats.get("excluded") + " properties ");

		}
		System.out.println("matching query.");
		int diffkey = comparator.getDiscrepancies().get("key");
		int diffval = comparator.getDiscrepancies().get("value");
		int difftotal = diffkey + diffval;
		int ignored = comparator.getDiscrepancies().get("ignored");
		if (difftotal != 0) {
			System.out.println("\nKey discrepancies\t" + diffkey);
			System.out.println("Value discrepancies\t" + diffval);
			System.out.println("Total discrepancies\t" + difftotal);
			System.out.println("Ignored properties\t" + ignored);
			System.out.println();
		} else {

			// in case of identical configurations
			System.out.println("\nNo discrepancies found in the directories given by the query.");
			while (true) {
				System.out.print("Still write a CSV report? (y/n): ");
				String write = "";
				try {
					write = input.readLine();
				} catch (IOException e) {
					System.err.println("[ERROR] Illegal input.");
					continue;
				}

				if (write.equalsIgnoreCase("n")) {
					System.out.println();
					return;
				} else if (write.equalsIgnoreCase("y")) {
					break;
				}
			}

		}

		// prompts user to either enter a custom CSV name or use default name
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

		// creates necessary directories for CSV write
		String writePath = System.getProperty("user.dir") + sep + "lighthouse-reports";
		while (writePath.contains("\\")) {
			writePath = writePath.replace("\\", "/");
		}
		new File(writePath).mkdirs();

		String writeresult = comparator.writeToCSV(filename, writePath);
		writeresult = writeresult == null ? "Succesfully wrote " + writePath + "/" + filename : writeresult;
		System.out.println("\n" + writeresult + "\n");
	}

	private static String cleanPath(String path) {
		// cleans up path input for processing
		if (path.charAt(0) == '/') {
			path = path.substring(1);
		}
		if (path.charAt(path.length() - 1) == '/') {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	private static String formatAsJSON(Map<String, String> map) {
		String json = "{";
		for (Map.Entry<String, String> entry : map.entrySet()) {
			json += " \"" + entry.getKey() + "\" : \"" + entry.getValue() + "\" ,";
		}
		json = json.substring(0, json.length() - 1) + "}";
		return json;
	}

}