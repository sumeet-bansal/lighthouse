package databaseModule;

import java.util.*;

import org.bson.Document;

import com.mongodb.client.*;

/**
 * Pulls queried data from MongoDB for non-comparison searches.
 * 
 * @author ActianceEngInterns
 * @version 1.2
 */
public class QueryFunctions extends MongoManager {
	
	private final static int MAX_PATH_SPACING = 64;

	/**
	 * Finds every key or value in the database that contains a user-given
	 * pattern.
	 * 
	 * @param pattern
	 *            substring being searched for
	 * @param toggle
	 *            0 for key, 1 for value
	 * @return a Set of property keys or values that contain the pattern
	 */
	public static Set<String> grep(String pattern, int toggle) {

		// determine search type (key or value)
		String type = toggle == 0 ? "key" : "value";

		// iterates through all properties in database to find matches
		Set<String> matches = new HashSet<>();
		MongoCursor<Document> cursor = collection.find(new Document()).iterator();
		while (cursor.hasNext()) {
			String data = cursor.next().getString(type);
			if (data.contains(pattern)) {
				matches.add(data);
			}
		}
		return matches;
	}

	/**
	 * Queries the database for a user-given key and returns location(s) and
	 * values(s) of the key.
	 * 
	 * @param pattern
	 *            the key or value being found
	 * @param location
	 *            a specific path within which to find the key
	 * @param toggle
	 *            0 for key, 1 for value
	 * @return a List of Strings representing each key location and value
	 */
	public static ArrayList<String> findProp(String pattern, String location, int toggle) {

		// determine search type (key or value)
		String type = toggle == 0 ? "key" : "value";

		// sets up filter for given property and retrieves all matching properties
		Document filter = new Document();
		if (location != null) {
			filter = generateFilter(location);
		}
		filter.append(type, pattern);
		MongoCursor<Document> cursor = collection.find(filter).iterator();

		// iterates through matching properties and organizes them for stdout
		ArrayList<String> props = new ArrayList<>();
		while (cursor.hasNext()) {
			Document prop = cursor.next();
			String path = "PATH: ";
			for (int i = 0; i < genericPath.length; i++) {
				path += prop.getString(genericPath[i]) + "/";
			}

			// set up spacing for CLI output and lines up path with key
			int spaces = 5;
			if (path.length() < MAX_PATH_SPACING) {
				spaces = MAX_PATH_SPACING - path.length();
			}

			// output line with path, key, and value
			String line = path;
			for (int i = 0; i < spaces; i++) {
				path += " ";
			}
			if (toggle == 1) {
				line += "Key: " + prop.getString("key");
			} else {
				line += "Value: " + prop.getString("value");
			}
			props.add(line);
		}
		return props;
	}

}
