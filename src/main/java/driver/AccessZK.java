package driver;

import zookeeperModule.*;

/**
 * Runs ZKGenerator from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.1
 */
public class AccessZK {
	
	private static String help = "Usage: java -jar <jar file> zk <commands>"
			+ "\nPOSSIBLE COMMANDS \n"
			+ "'help'\n\tgoes to the help page for 'zk'\n"
			+ "\tUsage: java -jar <jar> zk help\n"
			+ "'generate'\n\tgenerates .properties files from a given environment\n"
			+ "\tUsage: java -jar <jar> zk generate\n"
			+ "\t       <host IP> <ZooKeeper path> <root folder> <environment name>\n"
			+ "\t       [exception] [exception] ... [exception]";
	
	/**
	 * Accesses a ZooKeeper instance to generate .properties files from a given
	 * environment.
	 * @param args command-line arguments
	 */
	public static void run(String[] args) {

		// if no args passed, automatically sets args[0] to "help"
		if (args.length == 0) {
			args = new String[1];
			args[0] = "help";
		}
		
		switch(args[0]) {
			case "generate":
				
				// necessary parameters not met
				if (args.length < 5) {
					System.err.println(help);
					return;
				}
	
				// initializes and utilizes ZKGenerator, handles specified branch exceptions
				ZKGenerator generator = new ZKGenerator(args[1], args[2], args[3], args[4]);
				for (int i = 4; i < args.length; i++) {
					generator.addException(args[i]);
				}
				generator.generate();
				
				break;
			case "help":
				System.err.println(help);
				return;
			default:
				System.err.println("Invalid input. Use the 'help' command for details on usage.");
				return;
		}
			
	}
}