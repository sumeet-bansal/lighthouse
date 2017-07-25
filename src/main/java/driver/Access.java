package driver;

import java.util.*;

/**
 * Runs the complete diagnostic tool from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class Access {
	
	private static String help = "POSSIBLE COMMANDS \n"
			+ "'zk'\tused for functions related to ZooKeeper\n"
			+ "'db'\tused for functions related to directly accessing the database\n"
			+ "'query'\tused for functions related to querying the database for diffs\n";
	
	/**
	 * Takes command-line arguments and delegates functionality as appropriate.
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		
		// if no args passed, automatically sets arg[0] to "help"
		if (args.length == 0) {
			args = new String[1];
			args[0] = "help";
		}
		
		// command-specific args
		String[] pass = null;
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
				System.out.println(help);
				break;
			default:
				// should be unreachable but in case of future modifications to code
				System.err.println("Invalid input. Use the 'help' command for details on usage.");
				return;
		}
	}
}