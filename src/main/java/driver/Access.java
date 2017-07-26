package driver;

import java.util.*;
import org.apache.log4j.*;

/**
 * Runs the complete diagnostic tool from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class Access {
	
	private static String help = "Usage: java -jar <jar> <commands>\nPOSSIBLE COMMANDS \n"
			+ "'help'\n\tgoes to the help page for the general diagnostic tool\n"
			+ "\tUsage: java jar -jar <jar> help\n"
			+ "'zk'\n\tused for functions related to ZooKeeper\n"
			+ "\tUsage: java -jar <jar> zk <commands>\n"
			+ "'db'\n\tused for functions related to directly accessing the database\n"
			+ "\tUsage: java -jar <jar> db <commands>\n"
			+ "'query'\n\tused for functions related to querying the database for diffs\n"
			+ "\tUsage: java -jar <jar> query <commands>";
	
	/**
	 * Takes command-line arguments and delegates functionality as appropriate.
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		
		// disable mongo logging
		@SuppressWarnings("unchecked")
		List<Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
		loggers.add(LogManager.getRootLogger());
		for ( Logger logger : loggers ) {
		    logger.setLevel(Level.ERROR);
		}
//		PropertyConfigurator.configure("resources/log4j.properties");
		

		// if no args passed, automatically sets arg[0] to "help"
		if (args.length == 0) {
			args = new String[1];
			args[0] = "help";
		}
		
		// command-specific args
		String[] pass = { "help" };
		if (args.length > 1) {
			pass = Arrays.copyOfRange(args, 1, args.length);
		}
		
		// delegates functionality as appropriate
		switch (args[0]) {
			case "zk":
				AccessZK.run(pass);
				break;
			case "db":
				AccessDB.run(pass);
				break;
			case "query":
				AccessUI.run(pass);
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