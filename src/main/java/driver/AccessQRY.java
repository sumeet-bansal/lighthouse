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
	
	private static String help = "Usage: java -jar <jar file> query <commands>"
			+ "\nPOSSIBLE COMMANDS \n"
			+ "'help'\n\tgoes to the help page for 'query'\n"
			+ "\tUsage: java -jar <jar> query help\n"
	//		+ "'find'\n\tfinds all instances of a specific property or properties\n"
	//		+ "\tUsage: java -jar <jar> query find <property> <property> ... <property>\n"
			+ "'compare'\n\tcompares the selected root directories and generates appropriate CSVs\n"
			+ "\tUsage: java -jar <jar> query compare <path1> <path2>\n";
	//		+ "'exclude'\n\texcludes selected files from the query\n"
	//		+ "\tmust be used in conjunction with the 'compare' command"
	//		+ "\tUsage: java -jar <jar> query compare <path1> <path2> exclude <file> <file> ... <file>\n";
	
	/**
	 * Queries the database and generates CSV files containing comparison
	 * data.
	 * @param args command-line arguments
	 */
	public static void run(String[] args) {

		// if no args passed, automatically sets arg[0] to "help"
		if (args.length == 0) {
			args = new String[1];
			args[0] = "help";
		}

		ArrayList<String> queried = new ArrayList<String>();
		ArrayList<String> excluded = new ArrayList<String>();

		// includes future implementation of 'exclude'
		switch (args[0]) {
			case "compare":
				int i = 1;
				ArrayList<String> arr = queried;
				while (i < args.length) {
					if (args[i].equals("exclude")) {
						arr = excluded;
					} else {
						arr.add(args[i]);
					}
					i++;
				}
				if (queried.size() == 0 || queried.size()%2 != 0) {
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
			
		Comparator c = new Comparator();
		for (int i = 0; i < queried.size(); i += 2) {
			c.addQuery(queried.get(i), queried.get(i+1));
		}
		for (int i = 0; i < excluded.size(); i++) {
			c.blockQuery(excluded.get(i));
		}

		// compare files, build CSV file and clear current query
		c.compare();
		String writePath = System.getProperty("user.home") + sep + "Desktop" + sep
				+ "diagnostic reports";
		new File(writePath).mkdirs();
		c.writeToCSV(writePath);
		c.clearQuery();
	}
}