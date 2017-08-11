package databaseModule;

import java.io.*;
import java.util.*;

import org.bson.Document;

import com.mongodb.*;
import com.mongodb.client.*;

/**
 * Generates cache of normalized server config files and data. Must have
 * 'mongod' running simultaneously.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class MongoManager {

	protected static MongoDatabase database;
	protected static MongoCollection<Document> collection;
	
	protected static String[] genericPath = { "environment", "fabric", "node", "filename" };
	protected static String[] reversePath = { "filename", "node", "fabric", "environment" };

	/**
	 * Initializes the cache.
	 */
	public static void connectToDatabase() {
		System.out.println("\n[DATABASE MESSAGE] Connecting to database...");

		// connects with server
		@SuppressWarnings("resource")
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		System.out.println("[DATABASE MESSAGE] Server connection successful @ localhost:27017");

		// connects with Database
		database = mongoClient.getDatabase("ADS_DB");

		// creates Collection
		collection = database.getCollection("ADS_COL");
		System.out.println("[DATABASE MESSAGE] Database connection successful @ ADS_DB.ADS_COL");

	}

	/**
	 * Clears all Documents from database.
	 */
	public static void clearDB() {

		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		String result = "";
		System.out.println();

		// repeatedly queries in case of invalid input
		while (true) {
			System.out.print("Clear entire database? (y/n): ");
			try {
				result = input.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (result.equalsIgnoreCase("y")) {
				long num = collection.count();
				collection.deleteMany(new Document());
				System.out.println("\nCleared " + num + " properties from collection " + collection.getNamespace());
				return;
			} else if (result.equalsIgnoreCase("n")) {
				return;
			} else {
				continue;
			}
		}
	}

	/**
	 * Gets a Set of the environments contained within the database.
	 * 
	 * @return a HashSet containing all the environments
	 */
	public static Set<String> getEnvironments() {
		Set<String> envs = new HashSet<String>();
		MongoCursor<Document> cursor = collection.find().iterator();
		while (cursor.hasNext()) {
			Document doc = cursor.next();
			String env = doc.getString("environment");
			if (env != null) {
				envs.add(env);
			}
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
