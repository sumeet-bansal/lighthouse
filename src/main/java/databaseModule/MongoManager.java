package databaseModule;

import java.util.*;

import org.bson.Document;

import com.mongodb.*;
import com.mongodb.client.*;

/**
 * Generates cache of normalized server config files and data. Must have
 * 'mongod' running simultaneously.
 * 
 * @author ActianceEngInterns
 * @version 1.2
 */
public class MongoManager {

	static final String DB_NAME = "LH_DB";
	static final String COL_NAME = "LH_COL";
	static final int DEFAULT_PORT = 27017;

	protected static MongoClient client;
	protected static MongoDatabase database;
	protected static MongoCollection<Document> collection;

	protected static String[] genericPath = { "environment", "fabric", "node", "filename" };
	protected static String[] reversePath = { "filename", "node", "fabric", "environment" };

	/**
	 * Checks connection and initializes the cache if successful.
	 */
	public static void connectToDatabase() {
		System.out.println("\n[DATABASE MESSAGE] Connecting to database...");

		// check connection
		MongoClient ping = new MongoClient();
		MongoDatabase db = ping.getDatabase("ping");
		db.drop();
		ping.close();

		// connects with server
		client = new MongoClient("localhost", DEFAULT_PORT);
		System.out.println("[DATABASE MESSAGE] Server connection successful @ localhost:" + DEFAULT_PORT);

		// connects with Database
		database = client.getDatabase(DB_NAME);

		// creates Collection
		collection = database.getCollection(COL_NAME);
		System.out.println(
				"[DATABASE MESSAGE] Database connection successful @ " + DB_NAME + "." + COL_NAME);
	}
	
	/**
	 * Disconnects the Mongo connection safely.
	 */
	public static void disconnect() {
		client.close();
	}

	/**
	 * Private helper method. Given path inputs, verifies the validity of the
	 * inputs and generates filters for the inputs.
	 * <dl>
	 * <dt>example path parameters:
	 * <dd>dev1/fabric2
	 * </dl>
	 * <dl>
	 * <dt>example filter:
	 * <dd>{environment: "dev1", fabric: "fabric2"}
	 * </dl>
	 * 
	 * @param path
	 *            the path for which a filter is being generated
	 * @return the generated filter as a BSON Document
	 */
	public static Document generateFilter(String path) {

		// cleans up the path
		while (path.indexOf("//") != -1) {
			path.replace("//", "/");
		}

		// splits the path by delimiter and adds metadata to filter
		String[] split = path.split("/");
		Document filter = new Document();
		for (int i = 0; i < split.length; i++) {
			if (split[i].charAt(0) != ('*')) {
				filter.append(genericPath[i], split[i]);
			} else if (split[i].startsWith("*.")) {
				filter.append("extension", split[i].substring(2));
			}
		}
		return filter;
	}

	/**
	 * Clears all Documents from database.
	 * 
	 * @return the number of properties cleared from the database
	 */
	public static long clearDB() {
		long removed = collection.count();
		collection.deleteMany(new Document());
		return removed;
	}

	/**
	 * Gets a Set of the environments contained within the database.
	 * 
	 * @return a HashSet containing all the environments
	 */
	public static Set<String> getEnvironments() {
		Set<String> envs = new HashSet<>();
		MongoCursor<String> cursor = collection.distinct("environment", String.class).iterator();
		while (cursor.hasNext()) {
			envs.add(cursor.next());
		}
		return envs;
	}

	/**
	 * Getter method for the MongoDB database.
	 * 
	 * @return the MongoDatabase being used
	 */
	public static MongoDatabase getDB() {
		return database;
	}

	/**
	 * Getter method for MongoDB collection.
	 * 
	 * @return the MongoCollection being used
	 */
	public static MongoCollection<Document> getCol() {
		return collection;
	}

}
