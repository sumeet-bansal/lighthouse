package databaseModule;

import java.io.*;
import java.util.*;

import org.bson.Document;

import com.mongodb.client.*;

import parser.*;

/**
 * Generates cache of normalized server config files and data. Must have
 * 'mongod' running simultaneously.
 * 
 * @author ActianceEngInterns
 * @version 2.0
 */
public class DbFunctions extends MongoManager {

	/**
	 * Feeds parsed Documents into the database.
	 * 
	 * @param path
	 *            the path containing the files to be cached (i.e. a compatible
	 *            directory structure, as outlined in the README and Dev Guide)
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
	 * @param level
	 *            the level to which the structure is being printed
	 */
	public static void printStructure(int level) {

		// grabs properties from database
		ArrayList<Document> props = new ArrayList<Document>();
		MongoCursor<Document> cursor = collection.find().iterator();
		while (cursor.hasNext()) {
			props.add(cursor.next());
		}

		// sets up tokens '-' for lowest level directories and '>' for mid level
		String fabToken = "> ";
		String nodeToken = "> ";
		if (level == 3) {
			fabToken = "- ";
		}
		if (level == 2) {
			nodeToken = "- ";
		}

		// specifies level for CLI output
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

		// prints structure based on level
		System.out.println("\nDATABASE STRUCTURE @ " + dir + " LEVEL\n");
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

}
