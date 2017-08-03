package driver;

import java.util.*;

import cachingLayer.DbFeeder;

/**
 * Runs DbFeeder from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.1
 */
public class AccessDB {

	private static String help = "\nUsage: java -jar <jar file> db <commands>" + "\nPOSSIBLE COMMANDS"
			+ "\n'help'\n\tgoes to the help page for 'db'"
			+ "\n\tUsage: java -jar <jar> db help"
			+ "\n'populate'\n\tpopulates the database with the given files"
			+ "\n\tUsage: java -jar <jar> zk populate <root directory>"
			+ "\n'clear'\n\tclears the database"
			+ "\n\tUsage: java -jar <jar> db clear"
			+ "\n'list'\n\tprints the structure of the database at user-specified level"
			+ "\n\tUsage: java -jar <jar> db list <level (1-4)>"
			+ "\n'info'\n\tprovides info about the contents of the database"
			+ "\n\tUsage: java -jar <jar> db info";
	
	private static String structureHelp = "Usage: java -jar <jar> db list <level>"
			+ "\nWhere <level> denotes the lowest level to which you would like to see the database structure."
			+ "\nAccepted level values:"
			+ "\n\t4 - environment"
			+ "\n\t3 - fabric"
			+ "\n\t2 - node"
			+ "\n\t1 - file";

	/**
	 * Accesses a MongoDB database to clear, populate, or provide info.
	 * @param args command-line arguments
	 */
	public static void run(String[] args) {

		// if no args passed, automatically sets arg[0] to "help"
		if (args.length == 0) {
			args = new String[1];
			args[0] = "help";
		}

		switch (args[0]) {
			case "populate":
				if (args.length > 1) {
					DbFeeder.connectToDatabase();
					DbFeeder.populate(args[1]);
				} else {
					System.err.println(help);
				}
				break;
			case "clear":
				DbFeeder.connectToDatabase();
				DbFeeder.clearDB();
				break;
			case "info":
				DbFeeder.connectToDatabase();
				long count = DbFeeder.getCol().count();
				Set<String> envs = DbFeeder.getEnvironments();
				System.out.println("\nCount:");
				System.out.println(count + " properties currently in database");
				System.out.println("\nEnvironments:");
				for (String env : envs) {
					System.out.println("- " + env);
				}
				break;
			case "list":
				DbFeeder.connectToDatabase();
				if (DbFeeder.getCol().count() == 0) {
					System.out.println("\nDatabase is empty.");
					return;
				}
				if (args.length > 1) {
					int level;
					try {
						level = Integer.parseInt(args[1]);
					} catch (Exception e) {
						System.err.println(structureHelp);
						return;
					}
					if (level < 1 || level > 4) {
						System.err.println(structureHelp);
						return;
					}
					DbFeeder.printStructure(level);
				} else {
					System.err.println(structureHelp);
				}
				break;
			case "help":
				System.err.println(help);
				break;
			default:
				System.err.println(help);
				return;
			}

	}
}