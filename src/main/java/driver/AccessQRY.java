package driver;

import java.io.*;
import java.util.*;

import cachingLayer.Comparator;

/**
 * Runs the Comparator from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.1
 */
public class AccessQRY {

	private static String sep = File.separator;

	private static String help = "Usage: java -jar <jar file> query <commands>" + "\nPOSSIBLE COMMANDS \n"
			+ "'help'\n\tgoes to the help page for 'query'\n" + "\tUsage: java -jar <jar> query help\n"
			// + "'find'\n\tfinds all instances of a specific property or properties\n"
			// + "\tUsage: java -jar <jar> query find <property> <property> ...
			// <property>\n"
			+ "'compare'\n\tcompares the selected root directories and generates appropriate CSVs\n"
			+ "\tUsage: java -jar <jar> query compare <path1> <path2>\n"
			+ "'exclude'\n\texcludes selected files from the query\n"
			+ "\tmust be used in conjunction with the 'compare' command\n"
			+ "\tUsage: java -jar <jar> query compare <path1> <path2> exclude <file> <file> ... <file>";

	/**
	 * Queries the database and generates CSV files containing comparison data.
	 * 
	 * @param args
	 *            command-line arguments
	 */
	public static void run(String[] args) {

		// if no args passed, automatically sets arg[0] to "help"
		if (args.length == 0) {
			args = new String[1];
			args[0] = "help";
		}

		ArrayList<String> queried = new ArrayList<String>();
		ArrayList<String> excluded = new ArrayList<String>();

		switch (args[0]) {
		case "compare":
			
			// uses generic 'arr' to populate appropriate List
			int i = 1;
			ArrayList<String> arr = queried; // adds all args to 'queried'
			while (i < args.length) {
				
				// if 'exclude' keyword detected, switches refs and adds rest
				// of args to 'excluded' List, else continues adding to 'queried'
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
		case "help":
			System.err.println(help);
			return;
		default:
			System.err.println("Invalid input. Use the 'help' command for details on usage.");
			return;
		}

		// adds queries to Comparator instance and compares
		Comparator c = new Comparator();
		if (queried.size() == 1) {
			c.internalQuery(queried.get(0));
		} else {
			for (int i = 0; i < queried.size(); i += 2) {
				c.addQuery(queried.get(i), queried.get(i+1));
			}
		}
		for (int i = 0; i < excluded.size(); i++) {
			c.exclude(excluded.get(i));
		}
		c.compare();

		// prompts user to either enter a custom CSV name or use default name
		String writePath = System.getProperty("user.home") + sep + "Documents" + sep + "ADS Reports";
		new File(writePath).mkdirs();
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

		// gives a summary of the discrepancies in the query
		String writeZero = "";
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

		String result = "";
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