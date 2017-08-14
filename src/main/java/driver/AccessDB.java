package driver;

import java.io.*;
import java.util.*;

import org.bson.Document;

import com.mongodb.client.MongoCursor;

import databaseModule.*;

/**
 * Runs DbFunctions from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.1
 */
public class AccessDB {

	private static String version = Access.version;

	private static String help = "\nDATABASE MODULE -- POSSIBLE COMMANDS\n"
			+ "\n'help'\n\tgoes to the help page for 'db'" + "\n\tUsage: lighthouse-v" + version + " # Database $ help"
			+ "\n'populate'\n\tpopulates the database with the given files" + "\n\tUsage: lighthouse-v" + version
			+ " # Database $ populate <root directory>" + "\n'clear'\n\tclears the database" + "\n\tUsage: lighthouse-v"
			+ version + " # Database $ clear"
			+ "\n'list'\n\tprints the structure of the database at user-specified level" + "\n\tUsage: lighthouse-v"
			+ version + " # Database $ <level (1-4)>" + "\n'info'\n\tprovides info about the contents of the database"
			+ "\n\tUsage: lighthouse-v" + version + " # Database $ info\n"
			+ "\n      - Type the command for another module ('query', 'home') to go to that module"
			+ "\n      - Type 'exit' at any time to exit the program\n";

	private static String listHelp = "\nUsage: lighthouse-v" + version + " # Database $ list <level>"
			+ "\nWhere <level> denotes the lowest level to which you would like to see the database structure."
			+ "\nAccepted level values:" + "\n\t4 - environment" + "\n\t3 - fabric" + "\n\t2 - node" + "\n\t1 - file\n";

	/**
	 * Handles user input and delegates functionality based on first command
	 * 
	 * @param args
	 *            command-line arguments
	 */
	public static void run(String[] args) {
		
		/*
		 * let the user know if the database needs to be populated in order to use
		 * database functions
		 */
		if (MongoManager.getCol().count() == 0 && !args[0].equals("populate") && !args[0].equals("help")) {
			System.err.println("Database is empty. Use the 'populate' command to feed files to the database.\n");
			return;
		}

		// handle command line input
		switch (args[0]) {
		case "populate":
			if (args.length != 2) {
				System.err.println(help);
				return;
			}
			DbFunctions.populate(args[1]);
			System.out.println();
			break;
		case "clear":
			promptForClear();
			break;
		case "info":
			printInfo();
			break;
		case "list":
			if (validLevel(args)) {
				int level = Integer.parseInt(args[1]);
				DbFunctions.printStructure(level);
				System.out.println();
			} else {
				System.err.println(listHelp);
			}
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
	 * Prompts user to see if they want to clear the database
	 * 
	 */
	private static void promptForClear() {
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		String result = "";
		System.out.println();

		// repeatedly queries in case of invalid input
		while (true) {
			System.out.print("Clear entire database? (y/n): ");
			try {
				result = input.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (result.equalsIgnoreCase("y")) {
				long n = MongoManager.clearDB();
				System.out.println(
						"\nCleared " + n + " properties from collection " + MongoManager.getCol().getNamespace());
				break;
			} else if (result.equalsIgnoreCase("n")) {
				return;
			} else {
				continue;
			}
		}
		System.out.println();
	}

	/**
	 * Checks if 'list' level input is valid
	 * 
	 * @param args
	 *            command-line arguemtns
	 * @return validity of input
	 */
	private static boolean validLevel(String[] args) {
		int level;
		try {
			level = Integer.parseInt(args[1]);
		} catch (Exception e) {
			return false;
		}
		if (level < 1 || level > 4) {
			return false;
		}
		return true;
	}

	/**
	 * Prints an info page for the database
	 */
	private static void printInfo() {
		System.out.println("\nDatabase Info:");

		// count each type of metadata tag in database
		long propCount = MongoManager.getCol().count();
		int fileCount = 0;
		int nodeCount = 0;
		int fabCount = 0;
		Set<String> envs = MongoManager.getEnvironments();
		ArrayList<Document> props = new ArrayList<Document>();
		MongoCursor<Document> cursor = MongoManager.getCol().find().iterator();
		while (cursor.hasNext()) {
			props.add(cursor.next());
		}
		for (int i = 0; i < envs.size(); i++) {
			Set<String> fabs = new HashSet<>();
			for (Document prop : props) {
				String fab = prop.getString("fabric");
				fabs.add(fab);
			}
			fabCount += fabs.size();
			for (int j = 0; j < fabs.size(); j++) {
				Set<String> nodes = new HashSet<>();
				for (Document prop : props) {
					String node = prop.getString("node");
					nodes.add(node);
				}
				nodeCount += nodes.size();
				for (int k = 0; k < nodes.size(); k++) {
					Set<String> files = new HashSet<>();
					for (Document prop : props) {
						String file = prop.getString("filename");
						files.add(file);
					}
					fileCount += files.size();
				}
			}
		}

		// print db count
		System.out.println("\nProperties\t" + propCount);
		System.out.println("Files\t\t" + fileCount);
		System.out.println("Nodes\t\t" + nodeCount);
		System.out.println("Fabrics\t\t" + fabCount);

		// print environments
		if (propCount != 0) {
			System.out.println("\nEnvironments:");
		}
		for (String env : envs) {
			System.out.println("- " + env);
		}
		System.out.println("\nUse the 'list' command to see a detailed database structure.\n");
	}
}