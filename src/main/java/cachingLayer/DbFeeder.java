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
		try {

			// connects with server
			@SuppressWarnings("resource")
			MongoClient mongoClient = new MongoClient("localhost", 27017);
			System.out.println("successfully connected to server");

			// connects with Database
			database = mongoClient.getDatabase("ADS_DB");
			System.out.println("connected to database " + database.getName());

			// creates Collection
			String colName = "ADS_COL";
			collection = database.getCollection(colName);
			System.out.println("accessed collection " + colName);

		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
	}

	/**
	 * Feeds parsed Documents into the database.
	 * @param path the path containing the files to be cached
	 */
	public static void feedDocs(String path) {
		File folder = new File(path);
		DirectoryParser directory = new DirectoryParser(folder);
		directory.parseAll();
		ArrayList<AbstractParser> parsedFiles = directory.getParsedData();

		for (AbstractParser s : parsedFiles) {

			// feeds data of parsed file to Document
			Map<String, Object> data = s.getData();
			for (Map.Entry<String, Object> property : data.entrySet()) {
				
				Document doc = new Document(); // represents a single property

				// due to MongoDB constraints, all dot chars in key fields
				// converted to an infrequent substring--three backticks (```)
				String key = property.getKey().replace(".", "```");
				String value = property.getValue().toString();
				doc.append(key, value);

				// gets metadata of parsed file and tags Document accordingly
				Map<String, String> metadata = s.getMetadata();
				for (Map.Entry<String, String> entry : metadata.entrySet()) {
					doc.append(entry.getKey(), entry.getValue());
				}

				collection.insertOne(doc);
			}
		}
	}

	/**
	 * Clears all Documents from database.
	 */
	public static void clearDB() {
		try {
			collection.deleteMany(new Document());
			System.out.println("cleared collection " + collection.getNamespace());
		} catch (Exception e) {
			System.err.println("failed to clear data from collection");
			e.printStackTrace();
		}
	}
	
	/**
	 * Getter method for the MongoDB database.
	 * @return the MongoDatabase being used
	 */
	public static MongoDatabase getDB() {
		return database;
	}
	
	/**
	 * Getter method for MongoDB collection.
	 * @return the MongoCollection being used
	 */
	public static MongoCollection<Document> getCol() {
		return collection;
	}

}
