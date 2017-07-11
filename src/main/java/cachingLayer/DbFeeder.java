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
	
	private HashSet<File> fileSet = new HashSet<File>();
	private MongoCollection<Document> col;
	
	/**
	 * Creates the cache and pulls server configuration data from it as per
	 * the query.
	 */
	public DbFeeder() {
		
		try {
			
			// connecting with server
			@SuppressWarnings("resource")
			MongoClient mongoClient = new MongoClient("localhost", 27017);
			System.out.println("server connection successfully done");
			
			// connecting with Database
			MongoDatabase dbs = mongoClient.getDatabase("test");
			System.out.println("connected to database " + dbs.getName());

			// drop Collection
			//col.drop();
			//System.out.println("dropped collection");
			
			// create Collection
			String colName = "dbtest";
			col = dbs.getCollection(colName);
			col.drop(); dbs.createCollection(colName);
			System.out.println("created collection " + colName);
			
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
	}

	/**
	 * Feeds parsed Documents into the database.
	 * @param path the path containing the files to be cached
	 */
	public void feedDocs(String path) {
		
		File folder = new File(path);
		DirectoryParser directory = new DirectoryParser(folder);
		directory.parseAll();
		ArrayList<AbstractParser> parsedFiles = directory.getParsedData();
		
		for (AbstractParser s : parsedFiles) {

			Document doc = new Document();	// represents a single parsed file

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
			col.insertOne(doc);
		}
	}
	
	/**
	 * Getter method for the MongoDB Collection.
	 * @return the MongoDB Collection
	 */
	public MongoCollection<Document> getCol() {
		return col;
	}
	
	/**
	 * Adds a File to the internal HashSet for caching.
	 * @param filepath the file path of the File to be added and cached
	 */
	public void addFile(String filepath) {
		fileSet.add(new File(filepath));
	}

}
