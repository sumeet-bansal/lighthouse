package driver;

import com.mongodb.client.*;

import org.bson.Document;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
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

		// disable mongo logging
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
		rootLogger.setLevel(Level.OFF);

		DbFeeder.connectToDatabase();

		try {
			switch (args[0]) {
			case "populate":
				DbFeeder.populate(args[1]);
				break;
			case ("clear"):
				DbFeeder.clearDB();
				break;
			case ("update"):
				try {
					// find name of root folder in database values
					MongoCollection<Document> col = DbFeeder.getCol();
					MongoCursor<Document> cursor = col.find().iterator();
					Document doc = cursor.next();

					doc.remove("environment");
					doc.remove("fabric");
					doc.remove("node");
					doc.remove("filename");

					String str = doc.toJson().replaceFirst("/", "");
					String root = "C:/" + str.substring(str.indexOf("@@@") + 5, str.indexOf("/"));

					// update database with root folder
					DbFeeder.clearDB();
					DbFeeder.populate(root);
					System.out.println("\nSuccessfully updated database with respect to root directory " + root);
				} catch (Exception e) {
					System.out.println("\nError: Could not update, database is empty");
					e.printStackTrace();
				}
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