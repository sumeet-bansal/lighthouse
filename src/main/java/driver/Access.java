package driver;

import java.util.*;
import org.apache.log4j.*;
import com.mongodb.*;

import databaseModule.*;

/**
 * Runs the complete diagnostic tool from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.2
 */
public class Access {

	public static Scanner s = new Scanner(System.in);

	private static String branch = "home";
	public static final String VERSION = "1.3";
	public static final String APPNAME = "lighthouse-v" + VERSION;
	
	private static String help = "\nHOME PAGE -- POSSIBLE COMMANDS"
			+ "\n'help'\n\tgoes to the help page for the general diagnostic tool"
			+ "\n\tUsage: ~$ help"
			+ "\n'db'\n\tswitches to the database module to access functions that directly edit the database"
			+ "\n\tUsage: ~$ db"
			+ "\n'query'\n\tswitches to the query module to access functions that analyze the contents of the database"
			+ "\n\tUsage: ~$ query\n";

	private static String note = "Note: lighthouse must be used in conjuntion with a working MongoDB connection."
			+ "\nTo ensure a working connection to the database, ensure the executable 'mongod' is running.";

	/**
	 * Takes command-line arguments and delegates functionality as appropriate.
	 * 
	 * @param args
	 *            command-line arguments
	 */
	public static void main(String[] args) {

		// disables logging, works in parallel with log4j.properties
		@SuppressWarnings("unchecked")
		List<Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
		loggers.add(LogManager.getRootLogger());
		for (Logger logger : loggers) {
			logger.setLevel(Level.OFF);
		}

		try {

			// startup
			printSplash();
			System.out.println(help);
			System.out.println(note);
			MongoManager.connectToDatabase();
			System.out.println();

			// runs until `exit` command given
			while (true) {
				System.out.print("lighthouse-v" + VERSION + ": " + branch + " $ ");
				String input = s.nextLine();
				if (input.equals("")) {
					continue;
				} else {
					while (input.indexOf("  ") != -1) {
						input.replace("  ", " ");
					}
				}
				args = input.split(" ");

				// switches modules or exits application
				switch (args[0]) {
				case "home":
				case "db":
				case "query":
					branch = args[0];
					if (args.length > 1) {
						args = Arrays.copyOfRange(args, 1, args.length);
					} else {
						args[0] = "help";
					}
					break;
				case "quit":
				case "exit":
					s.close();
					MongoManager.disconnect();
					System.exit(0);
					break;
				}

				// executes commands for 'home' branch
				if (branch.equals("home")) {
					switch (args[0]) {
					case "man":
					case "help":
						System.out.println(help);
						break;
					default:
						System.err.println("Invalid input. Use the 'help' command for details on usage.\n");
						continue;
					}
				}

				// delegates functionality as appropriate
				switch (branch) {
				case "home":
					break;
				case "db":
					AccessDB.run(args);
					break;
				case "query":
					AccessQRY.run(args);
					break;
				default:
					System.err.println("This should never run.");
					break;
				}

			}

		} catch (MongoTimeoutException t) {
			String error = "[DATABASE MESSAGE] Server connection timed out. Make sure a 'mongod' instance is running.";
			System.out.println(error + "\n\nExiting with error code 1.");
			System.exit(1);
		} catch (MongoSocketWriteException | MongoSocketOpenException s) {
			String error = "[DATABASE MESSAGE] Mongo connection interrupted. Check if 'mongod' is still running.";
			System.out.println(error + "\n\nExiting with error code 2.");
			System.exit(2);
		}
	}

	/**
	 * Prints a welcome page that runs when the user first starts up the jar
	 */
	public static void printSplash() {

		try {
			LinkedList<String> page = new LinkedList<>();
			page.add("\n\n");
			page.add("                                                                  _-^-_");
			page.add("                                            -            -    --   |@|   --    -         -");
			page.add(" _  _         _      _    _                                       =====");
			page.add("| |(_)  __ _ | |__  | |_ | |__    ___   _   _  ___   ___           |\\|");
			page.add("| || | / _` || '_ \\ | __|| '_ \\  / _ \\ | | | |/ __| / _ \\          |\\|");
			page.add("| || || (_| || | | || |_ | | | || (_) || |_| |\\__ \\|  __/          |\\|");
			page.add("|_||_| \\__, ||_| |_| \\__||_| |_| \\___/  \\__,_||___/ \\___|        ,/::|.._              ___");
			page.add("       |___/                                                  __,./::: ...^ ~ ~~~~~~~ / ~~");
			page.add("\n\nversion " + VERSION + "\n");
			page.add("developed by Sumeet Bansal, Pierce Kelaita, and Gagan Gupta");
			page.add("\n\n\n\n");

			Iterator<String> scroll = page.iterator();
			while (scroll.hasNext()) {
				System.out.println(scroll.next());
				Thread.sleep(50);
			}
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}