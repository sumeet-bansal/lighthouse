package driver;

import cachingLayer.DbFeeder;

/**
 * Runs DbFeeder
 * 
 * @author PKelaita
 *
 */
public class AccessDB {

	/**
	 * Passes command line arguments to either clear the database or populate with a
	 * specified root folder. Syntax is as follows: clear - clears the database OR
	 * populate <root folder> - populates the database
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		DbFeeder.connectToDatabase();

		try {
			switch (args[0]) {
			case "populate":
				DbFeeder.feedDocs(args[1]);
				break;
			case ("clear"):
				DbFeeder.clearDB();
				break;
			default:
				System.err.println(
						"\nInvalid database access input!\nUse \"populate <root directory>\" to feed files to the database\nUse \"clear\" to clear the database");
				break;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println(
					"\nInvalid database access input!\nUse \"populate <root directory>\" to feed files to the database\nUse \"clear\" to clear the database");
		}

	}
}
