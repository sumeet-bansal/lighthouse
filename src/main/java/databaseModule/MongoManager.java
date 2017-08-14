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

	static final String DATABASE_NAME = "LH_DB";
	static final String COLLECTION_NAME = "PROPERTIES";
	static final int DEFAULT_PORT = 27017;

	protected static MongoDatabase database;
	protected static MongoCollection<Document> collection;

	protected static String[] genericPath = { "environment", "fabric", "node", "filename" };
	protected static String[] reversePath = { "filename", "node", "fabric", "environment" };

	/**
	 * Checks connection and initializes the cache if successful.
	 * 
	 * @return Whether or not Mongo connection is successful.
	 */
	@SuppressWarnings("resource")
	public static void connectToDatabase() {
		System.out.println("\n[DATABASE MESSAGE] Connecting to database...");

		// check connection
		MongoClient ping = new MongoClient();
		MongoDatabase db = ping.getDatabase("ping");
		db.drop();

		// connects with server
		MongoClient mongoClient = new MongoClient("localhost", DEFAULT_PORT);
		System.out.println("[DATABASE MESSAGE] Server connection successful @ localhost:" + DEFAULT_PORT);

		// connects with Database
		database = mongoClient.getDatabase(DATABASE_NAME);

		// creates Collection
		collection = database.getCollection(COLLECTION_NAME);
		System.out.println(
				"[DATABASE MESSAGE] Database connection successful @ " + DATABASE_NAME + "." + COLLECTION_NAME);
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
