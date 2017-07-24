package driver;

import cachingLayer.DbFeeder;
/**
 * Runs DbFeeder from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.1
 */
public class AccessDB {

	/**
	 * Passes command line arguments to either clear the database or populate it
	 * with a specified root folder. Syntax: clear clears the database populate
	 * <root dir> populates the database
	 * 
	 * @param args
	 *            command-line arguments
	 */
	public static void run(String[] args) {

		DbFeeder.connectToDatabase();

		try {
			switch (args[0]) {
			case "populate":
				DbFeeder.populate(args[1]);
				break;
			case ("clear"):
				DbFeeder.clearDB();
				break;
			case ("info"):
				long count = DbFeeder.getCol().count();
				System.out.println("\nCount:");
				System.out.println(count + " properties currently in databse");
				break;
			default:
				printError();
				break;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			printError();
		}
	}

	public static void printError() {
		String message = "\nInvalid database access input."
				+ "\n Use \"populate <root directory>\" to feed files to the database"
				+ "\n Use \"clear\" to clear the database" + "\n Use \"info\" to see the contents of the database";
		System.err.println(message);
	}
}