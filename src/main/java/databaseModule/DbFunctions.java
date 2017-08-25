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
	 * 
	 * TODO rewrite with tree implementation
	 */
	public static void printInfo() {
		System.out.println("\nDatabase Info:");

		// count each type of metadata tag in database
		long propCount = MongoManager.getCol().count();
		int fileCount = 0;
		int nodeCount = 0;
		int fabCount = 0;
		Set<String> envs = MongoManager.getEnvironments();
		ArrayList<Document> props = new ArrayList<Document>();
		MongoCursor<Document> cursor = MongoManager.getCol().find().iterator();
		while (cursor.hasNext()) {
			props.add(cursor.next());
		}
		for (int i = 0; i < envs.size(); i++) {
			Set<String> fabs = new HashSet<>();
			for (Document prop : props) {
				String fab = prop.getString("fabric");
				fabs.add(fab);
			}
			fabCount += fabs.size();
			for (int j = 0; j < fabs.size(); j++) {
				Set<String> nodes = new HashSet<>();
				for (Document prop : props) {
					String node = prop.getString("node");
					nodes.add(node);
				}
				nodeCount += nodes.size();
				for (int k = 0; k < nodes.size(); k++) {
					Set<String> files = new HashSet<>();
					for (Document prop : props) {
						String file = prop.getString("filename");
						files.add(file);
					}
					fileCount += files.size();
				}
			}
		}

		// print db count
		System.out.println("\nProperties\t" + propCount);
		System.out.println("Files\t\t" + fileCount);
		System.out.println("Nodes\t\t" + nodeCount);
		System.out.println("Fabrics\t\t" + fabCount);

		// print environments
		if (propCount != 0) {
			System.out.println("\nEnvironments:");
		}
		for (String env : envs) {
			System.out.println("- " + env);
		}
		System.out.println("\nUse the 'list' command to see a detailed database structure.\n");
	}

}
