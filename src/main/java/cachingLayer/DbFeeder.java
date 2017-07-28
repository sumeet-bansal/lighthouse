package cachingLayer;

import java.io.*;
import java.util.*;

import org.bson.Document;

import com.mongodb.*;
import com.mongodb.client.*;

import parser.*;

/**
 * Generates cache of normalized server config files and data. Must have
 * 'mongod' running simultaneously.
 * 
 * @author ActianceEngInterns
 * @version 2.0
 */
public class DbFeeder {

	private static MongoDatabase database;
	private static MongoCollection<Document> collection;

	/**
	 * Initializes the cache.
	 */
	public static void connectToDatabase() {

		// connects with server
		@SuppressWarnings("resource")
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		System.out.println("[DATABASE MESSAGE] Server connection successful @ localhost:27017");

		// connects with Database
		database = mongoClient.getDatabase("ADS_DB");

		// creates Collection
		collection = database.getCollection("ADS_COL");
		System.out.println("[DATABASE MESSAGE] Database connection successful @ collection ADS_DB.ADS_COL");

	}

	/**
	 * Feeds parsed Documents into the database.
	 * 
	 * @param path
	 *            the path containing the files to be cached
	 */
	public static void populate(String path) {
		File folder = new File(path);
		DirectoryParser directory = new DirectoryParser(folder);
		directory.parseAll();
		ArrayList<AbstractParser> parsedFiles = directory.getParsedData();
		int count = 0;

		for (AbstractParser s : parsedFiles) {

			// feeds data of parsed file to Document
			Map<String, Object> data = s.getData();
			for (Map.Entry<String, Object> property : data.entrySet()) {

				Document doc = new Document(); // represents a single property
				String key = property.getKey();
				String value = property.getValue().toString();
				doc.append("key", key);
				doc.append("value", value);

				// gets metadata of parsed file and tags Document accordingly
				Map<String, String> metadata = s.getMetadata();
				for (Map.Entry<String, String> entry : metadata.entrySet()) {
					doc.append(entry.getKey(), entry.getValue());
				}
				collection.insertOne(doc);
				count++;
			}
		}
		System.out.println("\nAdded " + count + " properties to database");
	}

	/**
	 * Clears all Documents from database.
	 */
	public static void clearDB() {

		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		String result = "";
		
		// repeatedly queries in case of invalid input
		while (true) {
			System.out.print("\nClear entire database? (y/n): ");
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
