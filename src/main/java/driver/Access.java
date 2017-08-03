package driver;

import java.util.*;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Runs the complete diagnostic tool from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class Access {

	private static String help = "Usage: java -jar <jar> <commands>\n\nPOSSIBLE COMMANDS \n"
			+ "'help'\n\tgoes to the help page for the general diagnostic tool\n"
			+ "\tUsage: java jar -jar <jar> help\n" + "'zk'\n\tused for functions related to ZooKeeper\n"
			+ "\tUsage: java -jar <jar> zk <commands>\n"
			+ "'db'\n\tused for functions related to directly accessing the database\n"
			+ "\tUsage: java -jar <jar> db <commands>\n"
			+ "\tmust be used in conjunction with a working Mongo connection (ensure 'mongod.exe' is running)\n"
			+ "'query'\n\tused for functions related to querying the database for diffs\n"
			+ "\tUsage: java -jar <jar> query <commands>\n"
			+ "\tmust be used in conjunction with a working Mongo connection (ensure 'mongod.exe' is running)";

	/**
	 * Takes command-line arguments and delegates functionality as appropriate.
	 * 
	 * @param args
	 *            command-line arguments
	 */
	public static void main(String[] args) {
		// converts args to lower case
		for (int i = 0; i < args.length; i++) {
			args[i] = args[i].toLowerCase();
		}

		// disables logging, works in parallel with log4j.properties
		@SuppressWarnings("unchecked")
		List<Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
		loggers.add(LogManager.getRootLogger());
		for (Logger logger : loggers) {
			logger.setLevel(Level.ERROR);
		}

		// if no args passed, automatically prints welcome and sets args[0] to "help"
		if (args.length == 0) {
			printWelcome();
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
			AccessQRY.run(pass);
			break;
		case "help":
			System.err.println(help);
			break;
		default:
			System.err.println("Invalid input. Use the 'help' command for details on usage.");
			return;
		}
	}
	
	/**
	 * Prints a welcome page that runs when the user does not give any input
	 */
	public static void printWelcome() {
		ArrayList<String> lines = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			lines.add("");
		}
		lines.add("       db         88888888ba,     ad88888ba ");
		lines.add("      d88b        88      `\"8b   d8\"     \"8b");
		lines.add("     d8'`8b       88        `8b  Y8,        ");
		lines.add("    d8'  `8b      88         88  `Y8aaaaa,  ");
		lines.add("   d8YaaaaY8b     88         88    `\"\"\"\"\"8b,");
		lines.add("  d8\"\"\"\"\"\"\"\"8b    88         8P          `8b");
		lines.add(" d8'        `8b   88      .a8P   Y8a     a8P");
		lines.add("d8'          `8b  88888888Y\"'     \"Y88888P\" ");
		
		for (int i = 0; i < 3; i++) {
			lines.add("");
		}
		lines.add("version 1.1");
		lines.add("");
		lines.add("Developed by Pierce Kelaita, Sumeet Bansal, and Gagan Gupta");
		for (int i = 0; i < 3; i++) {
			lines.add("");
		}
		
		for (String str : lines) {
			System.out.println(str);
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
}