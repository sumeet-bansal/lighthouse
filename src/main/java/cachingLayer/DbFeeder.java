package cachingLayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Iterator;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import Parser.FileInputReader;

/**
 * Generates cache of normalized server config files. Must have 'mongod'
 * running simultaneously.
 * 
 * @author sbansal
 * @version 1.0
 * @since 2017-07-03
 */
public class DbFeeder {
	
	/** TODO
	 * - configure DbFeeder to work with UI
	 *   - replace main() with methods
	 *     - set up MongoDB instance in ctor
	 *   - method to create initial cache
	 *   - method to update cache
	 *     - work with recursive file finder via Listeners
	 */
	
	/** FIXME
	 * - weird bug with tab characters in printIter()
	 */
	
	private static HashSet<File> fileSet = new HashSet<File>();
	
	/**
	 * Creates the cache and pulls server configuration data from it as per
	 * the query.
	 * @param args command-line arguments
	 */
	public static void main(String args[]) {
		
		try {
			
			/** 1. setting up the caching layer */
			// connecting with server
			@SuppressWarnings("resource")
			MongoClient mongoClient = new MongoClient("localhost", 27017);
			System.out.println("server connection successfully done");
			
			// connecting with Database
			MongoDatabase dbs = mongoClient.getDatabase("test");
			System.out.println("connected to database " + dbs.getName());
			
			// create Collection
			String colName = "ahos";
			MongoCollection<Document> col = dbs.getCollection(colName);
			col.drop(); dbs.createCollection(colName);
			System.out.println("created collection " + colName);

			// drop Collection
			col.drop();
			System.out.println("dropped collection");

			// temporary for testing purposes
			Document doc1 = new Document().append("file", "doc1");
			Document doc2 = new Document().append("file", "doc2");
			
			/** 2. feeding parsed Documents into the database */
			File folder = new File("src/ParserResources"); //any directory containing parsable files
			InputPathReader directory = new InputPathReader(folder);
			directory.parseAll();
			for (String path : directory.getFilePaths()) {
				addFile(path);
			}
			Iterator<File> files = fileSet.iterator();
			while (files.hasNext()) {
				
				// gets ArrayLists of keys and values from parser
				File f = files.next();
				FileInputReader reader = new FileInputReader(f);
				reader.parseFile();
				System.out.println(reader.getData().getKeys());
				ArrayList<String> keys = reader.getData().getKeys();
				System.out.println(reader.getData().getVals());
				ArrayList<Object> vals = reader.getData().getVals();
				
				// feeds ArrayLists into Documents
				if (keys.size() != vals.size()) {
					System.err.println("invalid file: var-val mismatch");
				}
				Document doc = new Document();
				for (int i = 0; i < keys.size(); i++) {
					doc.append(keys.get(i), vals.get(i));
					doc1.append(keys.get(i), vals.get(i));
					doc2.append(keys.get(i), vals.get(i));
				}
				System.out.println(doc.toJson());
				
				// inserts Document generated from File into MongoDB Collection
				col.insertOne(doc);
			}
			
			/** 3. compares two given Documents */
			compareAll(doc1, doc2);
			compareDiffs(doc1, doc2);
			
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
	}
	
	/**
	 * Adds a File to the internal HashSet for caching.
	 * @param filepath the file path of the File to be added and cached
	 */
	public static void addFile(String filepath) {
		fileSet.add(new File(filepath));
	}
	
	/**
	 * Due to MongoDB constraints, all dot characters in the key field were
	 * converted to an infrequently used substring--three backticks (```)--and
	 * this converts those key Strings back to their original form with dots.
	 * @param key the key whose three-backtick sets are being converted
	 * @return the converted key, with dots instead of three-backtick sets
	 */
	private static String dotConversion(String key) {
		while (key.indexOf("```") != -1) {
			int dotIndex = key.indexOf("```");
			String pre = key.substring(0, dotIndex);
			String post = key.substring(dotIndex+3);
			key = pre + "." + post;
		}
		return key;
	}

	/**
	 * Helper method to iterate through key set and print out list of keys and
	 * values in respective Documents.
	 * @param set Set of keys to be printed out
	 * @param doc1 Document with values to be printed out
	 * @param doc2 Document with values to be printed out
	 */
	private static void printIter(Set<String> set, Document doc1, Document doc2) {
		Iterator<String> iter = set.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			String val1 = doc1.get(key).toString();// != null ? doc1.get(key) : "";
			String val2 = doc2.get(key).toString();// != null ? doc2.get(key) : "";
			key = dotConversion(key);
			if (val1 != null && val2 != null && !val1.equals(val2)) {
				// tabs may cause output to not print
				System.out.println(key + "\t" + val1 + "\t" + key + "\t" + val2 + "\t\t\tdiff");
			} else if (val1 == null) {
				System.out.println("\t\t" + key + "\t" + val2 + "\tmissing in doc1\t");
			} else if (val2 == null) {
				System.out.println(key + "\t" + val1 + "\t\t" + "\tmissing in doc2\t");
			} else {
				System.out.println(key + "\t" + val1 + "\t" + key + "\t" + val2);
			}
		}
	}
	
	/**
	 * Compares all keys between two Documents in database as per UI
	 * specifications.
	 * @param doc1 Document to be compared
	 * @param doc2 Document to be compared
	 */
	public static void compareAll(Document doc1, Document doc2) {
		// generate key set
		Set<String> setAmalgam = new TreeSet<>(doc1.keySet());
		setAmalgam.addAll(new TreeSet<String>(doc2.keySet()));
		setAmalgam.remove("_id");	// auto-generated by MongoDB
/*
		// manual check for key set
		System.out.println("doc1 key set: " + doc1.keySet());
		System.out.println("doc2 key set: " + doc2.keySet());
		System.out.println("complete key set " + setAmalgam);
*/
		// list all keys and values from both Documents
		System.out.println();
		System.out.println("------------------------list------------------------");
		System.out.println();
		printIter(setAmalgam, doc1, doc2);
	}
	
	/**
	 * Compares keys with different values (including null) between two
	 * Documents in database as per UI specifications.
	 * @param doc1 Document to be compared
	 * @param doc2 Document to be compared
	 */
	public static void compareDiffs(Document doc1, Document doc2) {
		// generate key sets
		Set<String> doc1unique = new HashSet<>(doc1.keySet());
		Set<String> doc2unique = new HashSet<>(doc2.keySet());
		Set<String> overlapSet = new HashSet<>(doc1.keySet());
		doc1unique.removeAll(new HashSet<String>(doc2.keySet()));
		doc2unique.removeAll(new HashSet<String>(doc1.keySet()));
		overlapSet.retainAll(new HashSet<String>(doc2.keySet()));
		overlapSet.remove("_id");	// auto-generated by MongoDB
/*		
		// manual check for key sets
		System.out.println("doc1 key set " + doc1.keySet());
		System.out.println("doc2 key set " + doc2.keySet());
		System.out.println("unique in doc1: " + doc1unique);
		System.out.println("unique in doc2: " + doc2unique);
		System.out.println("overlap in d12: " + overlapSet);
*/
		// gets differences between all files
		System.out.println();
		System.out.println("------------------------diff------------------------");
		System.out.println();
		Iterator<String> iter = overlapSet.iterator();
		TreeSet<String> diffSet = new TreeSet<>();
		while (iter.hasNext()) {
			String key = iter.next();
			if (!doc1.get(key).equals(doc2.get(key))) {
				diffSet.add(key);
			}
		}
		printIter(new TreeSet<String>(doc1unique), doc1, doc2);
		printIter(new TreeSet<String>(doc2unique), doc1, doc2);
		printIter(new TreeSet<String>(diffSet), doc1, doc2);
	}

}
