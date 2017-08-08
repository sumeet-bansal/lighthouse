package driver;

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

		MongoManager.connectToDatabase();
		switch (args[0]) {
			case "populate":
				if (args.length > 1) {
					DbFunctions.populate(args[1]);
				} else {
					System.err.println(help);
				}
				break;
			case "clear":
				MongoManager.clearDB();
				break;
			case "info":
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
				System.out.println("\nUse the 'list' command to see a detailed database structure.");
				break;
			case "list":
				if (MongoManager.getCol().count() == 0) {
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
					DbFunctions.printStructure(level);
				} else {
					System.err.println(structureHelp);
				}
				break;
			case "help":
				System.err.println(help);
				break;
			default:
				System.err.println("Invalid input. Use the 'help' command for details on usage.");
				return;
			}

	}
}