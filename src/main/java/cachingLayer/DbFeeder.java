package cachingLayer;

import java.io.File;
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
 * @version 1.1
 */
public class DbFeeder {

	private static HashSet<File> fileSet = new HashSet<File>();

	// public static fields
	public static MongoDatabase DATABASE;
	public static MongoCollection<Document> COLLECTION;

	/**
	 * Static initializer to create the cache and pulls server configuration data
	 * from it as per the query.
	 */
	public static void connectToDatabase() {
		try {

			// connecting with server
			@SuppressWarnings("resource")
			MongoClient mongoClient = new MongoClient("localhost", 27017);
			System.out.println("server connection successfully done");

			// connecting with Database
			DATABASE = mongoClient.getDatabase("ADS_DB");
			System.out.println("Connected to database " + DATABASE.getName());

			// create Collection
			String colName = "ADS_COL";
			COLLECTION = DATABASE.getCollection(colName);
			System.out.println("Accessed collection " + COLLECTION.getNamespace());

		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
	}

	/**
	 * Feeds parsed Documents into the database.
	 * 
	 * @param path
	 *            the path containing the files to be cached
	 */
	public static void feedDocs(String path) {
		File folder = new File(path);
		DirectoryParser directory = new DirectoryParser(folder);
		directory.parseAll();
		ArrayList<AbstractParser> parsedFiles = directory.getParsedData();

		int count = 0;
		for (AbstractParser s : parsedFiles) {

			Document doc = new Document(); // represents a single parsed file

			// gets metadata of parsed file and tags Document accordingly
			Map<String, String> metadata = s.getMetadata();
			for (Map.Entry<String, String> entry : metadata.entrySet()) {
				doc.append(entry.getKey(), entry.getValue());
			}

			// feeds data of parsed file to Document
			Map<String, Object> data = s.getData();
			for (Map.Entry<String, Object> entry : data.entrySet()) {
				String key = entry.getKey().replace(".", "```");
				String value = entry.getValue().toString();
				doc.append(key, value);
			}

			// inserts Document generated from parsed file data into MongoDB Collection
			COLLECTION.insertOne(doc);
			count++;
		}
		System.out.println("Added " + count + " files to collection " + COLLECTION.getNamespace());
	}

	/**
	 * Clears all Documents from database.
	 */
	public static void clearDB() {
		try {
			COLLECTION.deleteMany(new Document());
			System.out.println("Cleared data from collection " + COLLECTION.getNamespace());
			fileSet.clear();
		} catch (Exception e) {
			System.err.println("Could not clear data from collection");
			e.printStackTrace();
		}
	}

	/**
	 * Adds a File to the internal HashSet for caching.
	 * 
	 * @param filepath
	 *            the file path of the File to be added and cached
	 */
	public void addFile(String filepath) {
		fileSet.add(new File(filepath));
	}

}
