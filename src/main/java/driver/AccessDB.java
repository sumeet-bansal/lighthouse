package driver;

import java.io.*;
import java.util.*;

import org.bson.Document;

import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoCursor;

import databaseModule.*;

/**
 * Runs DbFunctions from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.1
 */
public class AccessDB {

	private static final String help = "\nDATABASE MODULE -- POSSIBLE COMMANDS"
			+ "\n'help'\n\tgoes to the help page for 'db'"
			+ "\n\tUsage: ~$ help"
			+ "\n'populate'\n\tpopulates the database with the given files"
			+ "\n\tUsage: ~$ populate <root directory>"
			+ "\n'info'\n\tprovides info about the contents of the database"
			+ "\n\tUsage: ~$ info"
			+ "\n'list'\n\tprints the structure of the database at user-specified level"
			+ "\n\tUsage: ~$ list <level (1-4)>"
			+ "\n'clear'\n\tclears the database"
			+ "\n\tUsage: ~$ clear"
			+ "\nType the name of another module to switch modules.\n";

	private static final String listHelp = "\nUsage: ~$ list <level>"
			+ "\nNote: level denotes the preferred lowest level of the database structure."
			+ "\nAccepted level values:"
			+ "\n\t4 - environment"
			+ "\n\t3 - fabric"
			+ "\n\t2 - node"
			+ "\n\t1 - file\n";

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
				DbFunctions.populate(args[1]);
			}
			
			break;
		case "clear":
			promptClear();
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
	 */
	private static void promptClear() {
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
				MongoNamespace col = MongoManager.getCol().getNamespace();
				System.out.println("\nCleared " + n + " properties from collection " + col + "\n");
				break;
			} else if (result.equalsIgnoreCase("n")) {
				return;
			} else {
				continue;
			}
		}
	}

	/**
	 * Checks if 'list' level input is valid.
	 * 
	 * @param args
	 *            command-line arguments
	 * @return true if the level is valid, else false
	 */
	private static boolean validLevel(String[] args) {
		int level;
		
		// verify that level is parseable int
		try {
			level = Integer.parseInt(args[1]);
		} catch (Exception e) {
			return false;
		}

		// verify that level falls within scope of database
		if (level < 1 || level > 4) {
			return false;
		}
		
		return true;
	}

	/**
	 * Details scope of database (i.e. number of environments, fabrics, nodes,
	 * files).
	 * 
	 * TODO rewrite with tree implementation
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