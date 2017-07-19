package driver;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import org.bson.Document;

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
			case ("update"):
				try {
					// find name of root folder in database values
					MongoCollection<Document> col = DbFeeder.COLLECTION;
					FindIterable<Document> iter = col.find();
					MongoCursor<Document> cursor = iter.iterator();
					Document doc = cursor.next();
					
					doc.remove("environment");
					doc.remove("fabric");
					doc.remove("node");
					doc.remove("filename");
					
					String str = doc.toJson().replaceFirst("/", "");
					String root = "C:/" + str.substring(str.indexOf("@@@") + 5, str.indexOf("/"));
					
					// update database with root folder
					DbFeeder.clearDB();
					DbFeeder.feedDocs(root);
					System.out.println("\nSuccessfully updated database with respect to root directory " + root);
				} catch (Exception e) {
					System.out.println("\nError: Could not update, database is empty");
					e.printStackTrace();
				}
				break;
			case ("info"):
				long count = DbFeeder.COLLECTION.count();
				System.out.println("\nCount:");
				System.out.println(count + " files currently in databse");
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
		String message = "\nInvalid database access input!"
				+ "\n - Use \"populate <root directory>\" to feed files to the database"
				+ "\n - Use \"clear\" to clear the database\n - Use \"info\" to see the contents of the database";
		System.err.println(message);
	}
}