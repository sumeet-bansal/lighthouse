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
	 * Adds each unique environment specified in the database properties to a
	 * HashSet
	 * 
	 * @return HashSet with each unique environment
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
	 * Prints the file structure based on each property's metadata in the database
	 * 
	 * @param level
	 *            the lowest level the user would like to print to
	 */
	public static void printStructure(int level) {
		// grab properties from database
		ArrayList<Document> props = new ArrayList<Document>();
		MongoCursor<Document> cursor = collection.find().iterator();
		while (cursor.hasNext()) {
			props.add(cursor.next());
		}

		// set up tokens '-' for lowest level directories and '>' for mid level
		String fabToken = "> ";
		String nodeToken = "> ";
		if (level == 3) {
			fabToken = "- ";
		}
		if (level == 2) {
			nodeToken = "- ";
		}

		// specify level for CLI output
		String dir = new String();
		switch (level) {
		case 4:
			dir = "ENVIRONMENT";
			break;
		case 3:
			dir = "FABRIC";
			break;
		case 2:
			dir = "NODE";
			break;
		case 1:
			dir = "FILE";
			break;
		}

		// print structure based on level
		System.out.println("\nDATABASE STRUCTURE @ " + dir + " LEVEL");
		Set<String> envs = getEnvironments();
		for (String env : envs) {
			System.out.println(env);
			if (level == 4) {
				continue;
			}
			Set<String> fabs = new HashSet<>();
			for (Document prop : props) {
				String fab = prop.getString("fabric");
				if (prop.getString("environment").equals(env)) {
					fabs.add(fab);
				}
			}
			for (String fab : fabs) {
				System.out.println("  " + fabToken + fab);
				if (level == 3) {
					continue;
				}
				Set<String> nodes = new HashSet<>();
				for (Document prop : props) {
					String node = prop.getString("node");
					if (prop.getString("fabric").equals(fab) && prop.getString("environment").equals(env)) {
						nodes.add(node);
					}
				}
				for (String node : nodes) {
					System.out.println("    " + nodeToken + node);
					if (level == 2) {
						continue;
					}
					Set<String> files = new HashSet<>();
					for (Document prop : props) {
						String file = prop.getString("filename");
						if (prop.getString("node").equals(node) && prop.getString("fabric").equals(fab)
								&& prop.getString("environment").equals(env)) {
							files.add(file);
						}
					}
					for (String file : files) {
						System.out.println("      - " + file);
					}
				}
			}
		}
	}

	public static ArrayList<String> findProp(String key) {
		Document keyFilter = new Document().append("key", key);
		MongoCursor<Document> cursor = collection.find(keyFilter).iterator();
		ArrayList<Document> props = new ArrayList<>();
		while (cursor.hasNext()) {
			props.add(cursor.next());
		}
		ArrayList<String> pathList = new ArrayList<>();
		for (Document prop : props) {
			String env = prop.getString("environment");
			String fab = prop.getString("fabric");
			String node = prop.getString("node");
			String file = prop.getString("filename");
			String value = prop.getString("value");
			pathList.add("PATH: " + env + "/" + fab + "/" + node + "/" + file + "\tVALUE: " + value);
		}
		return pathList;
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
