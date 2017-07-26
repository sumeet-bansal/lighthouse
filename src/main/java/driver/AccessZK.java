package driver;

import zookeeperModule.*;

/**
 * Runs ZKGenerator from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class AccessZK {
	
	private static String help = "Usage: java -jar <jar file> zk <commands>"
			+ "\nPOSSIBLE COMMANDS \n"
			+ "'help'\n\tgoes to the help page for 'zk'\n"
			+ "\tUsage: java -jar <jar> zk help\n"
			+ "'generate'\n\tgenerates .properties files from a given environment\n"
			+ "\tUsage: java -jar <jar> zk generate "
			+ "<host IP> <root folder> <environment name>\n"
			+ "\t       [exception] [exception] ... [exception]";
	
	/**
	 * Accesses a ZooKeeper instance to generate .properties files from a given
	 * environment.
	 * @param args command-line arguments
	 */
	public static void run(String[] args) {
		
		// describes proper usage of 'zk' command
		if (args.length < 3 || args[0] == "help") {
				System.err.println(help);
				return;
		}

		// initializes and utilizes ZKGenerator, handles specified branch exceptions
		ZKGenerator generator = new ZKGenerator(args[1], args[2], args[3]);
		for (int i = 3; i < args.length; i++) {
			generator.addException(args[i]);
		}
		generator.generate();
			
	}
}