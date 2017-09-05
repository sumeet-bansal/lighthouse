package databaseModule;

import java.io.*;
import java.util.*;

import org.bson.Document;

import com.mongodb.*;
import com.mongodb.client.MongoCursor;

import parser.*;

/**
 * Generates cache of normalized server config files and data. Must have 'mongod' running
 * simultaneously.
 * 
 * @author ActianceEngInterns
 * @version 2.0
 */
public class DbFunctions extends MongoManager {

	/**
	 * Feeds parsed Documents into the database.
	 * 
	 * @param path
	 *            the path of the root directory containing the files to be cached (i.e. a
	 *            compatible directory structure, as outlined in the README and Dev Guide)
	 * @return the number of properties added to the database
	 */
	public static int populate(String path) {

		File root = new File(path);
		DirectoryParser directory = new DirectoryParser(root);
		directory.parseAll();
		ArrayList<AbstractParser> parsedFiles = directory.getParsedData();

		Map<Document, Set<String>> ignore = new HashMap<>();

		// iterates through each parsed file
		int count = 0;
		for (AbstractParser parsedFile : parsedFiles) {

			LinkedList<Document> docs = new LinkedList<>();
			Map<String, String> metadata = parsedFile.getMetadata();
			Map<String, Object> data = parsedFile.getData();

			// sets up generic path filter for specific file
			Document filter = new Document();
			for (Map.Entry<String, String> entry : metadata.entrySet()) {
				filter.append(entry.getKey(), entry.getValue());
			}

			// if file is .ignore file, add to Map of BSON filters and properties to ignore
			if (parsedFile.isInternal()) {
				if (!ignore.containsKey(filter)) {
					ignore.put(filter, new HashSet<String>());
				}
				for (Map.Entry<String, Object> property : data.entrySet()) {
					ignore.get(filter).add(property.getKey());
				}
				continue;
			}

			// feeds data of parsed file to Document
			for (Map.Entry<String, Object> property : data.entrySet()) {

				Document doc = new Document(); // represents a single property
				doc.append("key", property.getKey());
				doc.append("value", property.getValue().toString());
				doc.append("ignore", "false");

				// gets metadata of parsed file and tags Document accordingly
				for (Map.Entry<String, String> entry : metadata.entrySet()) {
					doc.append(entry.getKey(), entry.getValue());
				}
				docs.add(doc);
				count++;
			}

			// overwrites existing properties with matching metadata
			collection.deleteMany(filter);
			if (!docs.isEmpty()) {
				collection.insertMany(docs);
			}

		}

		// sets the "ignore" field to true for each property specified in each .ignore file
		for (Map.Entry<Document, Set<String>> entry : ignore.entrySet()) {
			ignore(entry.getKey(), entry.getValue());
		}

		return count;
	}

	/**
	 * Given a BSON and set of properties, updates the "ignore" field for each property matching the
	 * filter to true.
	 * 
	 * @param filter
	 *            a BSON filter containing metadata specifying which properties to ignore
	 * @param properties
	 *            a Set containing the keys of each property to be ignored
	 */
	private static void ignore(Document filter, Set<String> properties) {

		// creates Mongo query including each property
		QueryBuilder qb = new QueryBuilder();
		Iterator<String> iter = properties.iterator();
		String[] ignore = new String[properties.size()];
		int i = 0;
		while (iter.hasNext()) {
			ignore[i++] = iter.next();
		}
		qb.put("key").in(ignore);

		// adds path filter to query
		BasicDBObject query = new BasicDBObject();
		query.putAll(qb.get());
		query.putAll(filter);
		query.remove("filename");
		query.remove("path");
		query.remove("extension");
		
		// updates "ignore" field to true for all properties matching query
		Document updated = new Document().append("$set", new Document().append("ignore", "true"));
		collection.updateMany(query, updated);

	}

	/**
	 * Prints the directory structure of the files within the database.
	 * 
	 * @param path
	 *            a specific branch of the structure to print
	 * @param level
	 *            the level to which the structure is being printed
	 */
	public static void printStructure(String path, int level) {
		DirTree tree = popTree();
		tree.print(path, level);
	}

	/**
	 * Private method to generate DirTrees of the complete directory structure within the database.
	 * @return the generated DirTree, a representation of the complete directory structure within
	 *         the database
	 */
	private static DirTree popTree() {
		DirTree tree = new DirTree();
		MongoCursor<String> cursor = collection.distinct("path", String.class).iterator();
		while (cursor.hasNext()) {
			tree.insert(cursor.next());
		}
		return tree;
	}

	/**
	 * Details scope of database (i.e. number of environments, fabrics, nodes, files).
	 */
	public static void printInfo() {

		// calculates numbers of tree nodes at respective depths
		DirTree tree = popTree();
		long properties = MongoManager.getCol().count();
		int level = 1;
		int envs = tree.countNodes(tree.getRoot(), level++, true);
		int fabrics = tree.countNodes(tree.getRoot(), level++, true);
		int nodes = tree.countNodes(tree.getRoot(), level++, true);
		int files = tree.countNodes(tree.getRoot(), level++, false);

		System.out.println("\nThere are currently " + properties + " properties in the database.");
		System.out.println("\nenvironments\t\t" + envs + " (see below)");
		System.out.println(" > fabrics\t\t" + fabrics);
		System.out.println("   - nodes\t\t" + nodes);
		System.out.println("     - files\t\t" + files);

		// print environments
		if (envs != 0) {
			System.out.println("\nEnvironments:");
		}
		int i = 0;
		for (String env : MongoManager.getEnvironments()) {
			System.out.println(++i + ". " + env);
		}
		System.out.println("\nUse the 'list' command to see a detailed database structure.\n");
	}

}
