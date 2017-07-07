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
	
	/** TODO
	 * - configure DbFeeder to work with UI
	 * - method to update cache (via Listeners?)
	 */
	
	private static HashSet<File> fileSet = new HashSet<File>();
	private static MongoCollection<Document> col;
	
	/**
	 * Quick tester.
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		DbFeeder feeder = new DbFeeder();
		feeder.feedDocs("C:/Users/sbansal/Documents/parserResources");
	}
	
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
	private static String backtickToDot(String key) {
		key.replace("```", ".");
		return key;
	}

	/**
	 * Due to MongoDB constraints, all equal characters in the value field were
	 * converted to an infrequently used substring--three at signs (@@@)--and
	 * this converts those key Strings back to their original form with equal
	 * signs.
	 * @param value the value whose three-at-sign sets are being converted
	 * @return the converted value, with equal signs instead of three-at-signs
	 */
	private String equalSignToAtSign(String value) {
		value.replace("@@@", "=");
		return value;
	}

	/**
	 * Helper method to iterate through key set and print out list of keys and
	 * values from respective Documents.
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
			key = backtickToDot(key);
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
		// generates key set
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
		// generates key sets
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
