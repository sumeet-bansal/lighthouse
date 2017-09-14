package databaseModule;

import java.io.*;
import java.util.*;

import driver.SQLiteManager;
import parser.*;

/**
 * Generates cache of normalized server config files and data. Must have 'mongod' running
 * simultaneously.
 * 
 * @author ActianceEngInterns
 * @version 2.0
 */
public class DbFunctions {

	/**
	 * Feeds parsed Documents into the database.
	 * 
	 * @param path
	 *            the path of the root directory containing the files to be cached (i.e. a
	 *            compatible directory structure, as outlined in the README and Dev Guide)
	 * @return the number of properties added to the database
	 */
	public static long populate(String path) {

		File root = new File(path);
		DirectoryParser directory = new DirectoryParser(root);
		directory.parseAll();
		ArrayList<AbstractParser> parsedFiles = directory.getParsedData();

		LinkedList<Map<String, String>> deletions = new LinkedList<>();
		LinkedList<Map<String, String>> documents = new LinkedList<>();

		// Map where each key is a metadata filter, each value is Set of properties within that
		// scope that are preset to be ignored
		Map<Map<String, String>, Set<String>> ignore = new HashMap<>();

		// iterates through each parsed file
		for (AbstractParser parsedFile : parsedFiles) {

			Map<String, String> metadata = parsedFile.getMetadata();
			Map<String, Object> properties = parsedFile.getData();

			// if file is .ignore file, add to Map of filters and properties to ignore
			if (parsedFile.isInternal()) {

				// to avoid null pointers for insertion
				if (!ignore.containsKey(metadata)) {
					ignore.put(metadata, new HashSet<String>());
				}

				for (Map.Entry<String, Object> property : properties.entrySet()) {
					ignore.get(metadata).add(property.getKey());
				}
				continue;
			}

			// sets up each property as an individual Map<String, String>
			for (Map.Entry<String, Object> property : properties.entrySet()) {
				Map<String, String> document = new LinkedHashMap<>();
				document.put("key", property.getKey());
				document.put("value", property.getValue().toString());
				document.putAll(metadata);
				document.put("ignore", "false");
				documents.add(document);
			}

			// queues metadata for deletion to "overwrite" existing properties with matching
			deletions.add(metadata);

		}

		// functionally overwrites all existing properties with matching file metadata
		SQLiteManager.deleteBatch(deletions);
		SQLiteManager.insertBatch(documents);

		// sets the "ignore" field to true for each property specified in each .ignore file
		for (Map.Entry<Map<String, String>, Set<String>> entry : ignore.entrySet()) {
			Map<String, String> filter = entry.getKey();
			filter.remove("filename");
			filter.remove("path");
			filter.remove("extension");
			Set<String> keys = entry.getValue();

			Map<String, String> updated = new HashMap<>();
			updated.put("ignore", "true");
			SQLiteManager.update(updated, filter, keys);
		}

		return documents.size();
	}

	/**
	 * Given a filter and set of properties, updates the "ignore" field for each property matching
	 * the filter to the optionally-specified value.
	 * 
	 * @param location
	 *            a specific path within which to ignore a property
	 * @param properties
	 *            a Set containing the keys of each property to be ignored
	 * @param toggle
	 *            true if the properties are to be ignored, else false
	 */
	public static void ignore(String location, Set<String> properties, boolean toggle) {

		// generates filter from location
		Map<String, String> filter = location != null ? SQLiteManager.generatePathFilter(location) : new HashMap<>();

		// removes unnecessary fields from filter
		filter.remove("filename");
		filter.remove("path");
		filter.remove("extension");

		// updates "ignore" field to toggled value
		Map<String, String> update = new HashMap<>();
		update.put("ignore", toggle ? "true" : "false");
		SQLiteManager.update(update, filter, properties);

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
	public static DirTree popTree() {
		DirTree tree = new DirTree();
		Iterator<String> paths = SQLiteManager.getDistinct("path", null).iterator();
		while (paths.hasNext()) {
			tree.insert(paths.next());
		}
		return tree;
	}

	/**
	 * Details scope of database (i.e. number of environments, fabrics, nodes, files).
	 */
	public static void printInfo() {

		// calculates numbers of tree nodes at respective depths
		DirTree tree = popTree();
		long properties = SQLiteManager.getSize();
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
		Iterator<String> environments = SQLiteManager.getDistinct("environment", null).iterator();
		while (environments.hasNext()) {
			System.out.println(++i + ". " + environments.next());
		}
		System.out.println("\nUse the 'list' command to see a detailed database structure.\n");
	}

	/**
	 * Getter method for the database properties set to be ignored.
	 * 
	 * @return a Set containing all the properties with the "ignore" field marked as "true"
	 */
	public static Set<String> getIgnored() {
		Set<String> ignored = new HashSet<>();
		String sql = "SELECT DISTINCT key FROM " + SQLiteManager.getTable() + " WHERE ignore = 'true';";
		Iterator<Map<String, String>> iter = SQLiteManager.select(sql).iterator();
		while (iter.hasNext()) {
			ignored.add(iter.next().get("key"));
		}
		return ignored;
	}

}
