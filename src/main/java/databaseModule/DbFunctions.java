package databaseModule;

import java.io.*;
import java.util.*;

import org.bson.Document;

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
	 *            the path containing the files to be cached (i.e. a compatible directory structure,
	 *            as outlined in the README and Dev Guide)
	 */
	public static void populate(String path) {
		File folder = new File(path);
		DirectoryParser directory = new DirectoryParser(folder);
		directory.parseAll();
		ArrayList<AbstractParser> parsedFiles = directory.getParsedData();
		int count = 0;

		for (AbstractParser s : parsedFiles) {

			LinkedList<Document> docs = new LinkedList<>();
			Map<String, String> metadata = s.getMetadata();

			// feeds data of parsed file to Document
			Map<String, Object> data = s.getData();
			for (Map.Entry<String, Object> property : data.entrySet()) {

				Document doc = new Document(); // represents a single property
				doc.append("key", property.getKey());
				doc.append("value", property.getValue().toString());

				// gets metadata of parsed file and tags Document accordingly
				for (Map.Entry<String, String> entry : metadata.entrySet()) {
					doc.append(entry.getKey(), entry.getValue());
				}
				docs.add(doc);
				count++;
			}

			for (Document doc : docs) {
				collection.insertOne(doc);
			}

		}
		System.out.println("\nAdded " + count + " properties to database.\n");
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
		System.out.println();
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
	 * Details scope of database (i.e. number of environments, fabrics, nodes,
	 * files).
	 */
	public static void printInfo() {
		
		// calculates numbers of tree nodes at respective depths
		DirTree tree = popTree();
		long properties = MongoManager.getCol().count();
		int level = 1;
		int envs = tree.countNodes(tree.getRoot(), level++);
		int fabrics = tree.countNodes(tree.getRoot(), level++);
		int nodes = tree.countNodes(tree.getRoot(), level++);
		int files = tree.countNodes(tree.getRoot(), level++);

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
