package driver;

import cachingLayer.DbFeeder;

/**
 * Runs DbFeeder from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.1
 */
public class AccessDB {
	
	private static String help = "Usage: java -jar <jar file> db <commands>"
			+ "\nPOSSIBLE COMMANDS"
			+ "\n'help'\n\tgoes to the help page for 'db'"
			+ "\n\tUsage: java -jar <jar> db help"
			+ "\n'populate'\n\tpopulates the database with the given files"
			+ "\n\tUsage: java -jar <jar> zk populate <root directory>"
			+ "\n'clear'\n\tclears the database"
			+ "\n\tUsage: java -jar <jar> db clear"
			+ "\n'info'\n\tprovides info about the contents of the database"
			+ "\n\tUsage: java -jar <jar> db info";
	
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
		
		DbFeeder.connectToDatabase();

		switch (args[0]) {
			case "populate":
				if (args.length == 1) {
					DbFeeder.populate(args[1]);
				} else {
					System.err.println(help);
				}
				break;
			case "clear":
				DbFeeder.clearDB();
				break;
			case "info":
				long count = DbFeeder.getCol().count();
				System.out.println("\nCount:");
				System.out.println(count + " properties currently in database");
				break;
			case "help":
				System.err.println(help);
				break;
			default:
				// should be unreachable but in case of future modifications to code
				System.err.println("Invalid input. Use the 'help' command for details on usage.");
				return;
		}
	}
	
}