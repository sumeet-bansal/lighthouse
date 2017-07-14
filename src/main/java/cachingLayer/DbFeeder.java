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
	
	//public static fields
	public static MongoDatabase DATABASE;
	public static MongoCollection<Document> COLLECTION;
	

	/**
	 * Creates the cache and pulls server configuration data from it as per the
	 * query.
	 */
	public static void setup() {

		try {

			// connecting with server
			@SuppressWarnings("resource")
			MongoClient mongoClient = new MongoClient("localhost", 27017);
			System.out.println("server connection successfully done");

			// connecting with Database
			DATABASE = mongoClient.getDatabase("ADS_DB");
			System.out.println("connected to database " + DATABASE.getName());

			// create Collection
			String colName = "ADS_COL";
			COLLECTION = DATABASE.getCollection(colName);
			COLLECTION.drop();
			DATABASE.createCollection(colName);
			System.out.println("created collection " + colName);

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

		for (AbstractParser s : parsedFiles) {

			Document doc = new Document(); // represents a single parsed file

			// gets metadata of parsed file and tags Document accordingly
			Map<String, String> metadata = s.getMetadata();
			for (Map.Entry<String, String> entry : metadata.entrySet()) {
				doc.append(entry.getKey(), entry.getValue());
			}

			// gets ArrayLists of keys and values from parsed file
			ArrayList<String> keys = s.getKeys();
			ArrayList<Object> vals = s.getVals();

			// feeds ArrayLists into Documents
			if (keys.size() != vals.size()) {
				System.err.println("invalid file: var-val mismatch");
			}
			for (int i = 0; i < keys.size(); i++) {
				String key = keys.get(i).replace(".", "```");
				doc.append(key, vals.get(i).toString());
			}

			// inserts Document generated from parsed file data into MongoDB Collection
			System.out.println(doc.toJson());
			COLLECTION.insertOne(doc);
		}
	}
	
	/**
	 * Clears all documetns from database
	 */
	public static void clearDB() {
		try {
		COLLECTION.deleteMany(new Document());
		System.out.println("Cleared data from collection " + COLLECTION.getNamespace());
		fileSet.clear();
		} catch (Exception e) {
			System.err.println("Could not clear data from collection " + COLLECTION.getNamespace());
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
