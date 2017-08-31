package parser;

import java.io.*;
import java.util.*;

/**
 * Abstract class that declares common methods and instance variables for each file type parser.
 * 
 * @author ActianceEngInterns
 * @version 1.1
 */
public abstract class AbstractParser {

	protected String root;
	protected String path;
	protected Map<String, Object> data = new LinkedHashMap<>();
	protected boolean error = false;

	/**
	 * Getter method for the parsed data.
	 * 
	 * @return A Map representation of the parsed data: each property in a file can be represented
	 *         as a key-value pair (i.e. a Map entry) and a single file can be represented as a
	 *         collection of properties, so that file can then be represented as a collection of
	 *         key-value pairs (i.e. a Map).
	 */
	public final Map<String, Object> getData() {
		return data;
	}

	/**
	 * Getter method for the property metadata (e.g. environment, fabric, node).
	 * 
	 * @return a Map representation of the property metadata
	 */
	public final Map<String, String> getMetadata() {

		// formats path as array with structure [environment, fabric, node, file],
		// with null values for non-standard paths (e.g. root/file becomes [null, null, null, file])
		String[] predictedPath = { "environment", "fabric", "node", "filename" };
		String relativePath = path.substring(root.length());
		String[] split = relativePath.split("/");
		String[] delimitedPath = new String[predictedPath.length];
		for (int i = 0; i < split.length - 1; i++) {
			delimitedPath[i] = split[i];
		}
		delimitedPath[delimitedPath.length - 1] = split[split.length - 1];

		// goes through arrays and adds metadata if not null
		Map<String, String> metadata = new LinkedHashMap<>();
		for (int i = predictedPath.length - 1; i >= 0; i--) {
			if (delimitedPath[i] != null) {
				metadata.put(predictedPath[i], delimitedPath[i]);
			}
		}
		metadata.put("path", relativePath);

		// adds extensions to metadata with exception for hosts files
		if (metadata.get("filename").equals("hosts")) {
			metadata.put("extension", "hosts");
		} else {
			metadata.put("extension", relativePath.substring(relativePath.lastIndexOf('.') + 1));
		}
		return metadata;
	}

	/**
	 * Setter method for file path instance variable.
	 * 
	 * @param path
	 *            the path of the File being standardized
	 */
	public final void setPath(String root, String path) {
		this.root = cleanPath(root + "/");
		this.path = cleanPath(path);
	}

	/**
	 * Helper method to clean path inputs. Reformats paths with slashes for file delimiters.
	 * @param path
	 *            the path to clean
	 * @return the cleaned path
	 */
	private final String cleanPath(String path) {

		// necessary because Windows file delimiters throw off String methods
		String p = "";
		for (int i = 0; i < path.length(); i++) {
			if (path.charAt(i) != '\\') {
				p += path.charAt(i);
			} else {
				p += "/";
			}
		}

		while (p.indexOf("//") != -1) {
			p.replace("//", "/");
		}

		return p;

	}

	/**
	 * Standardizes input File into a Map of keys and values. Each property in a file can be
	 * represented as a key-value pair (i.e. a Map entry) and a single file can be represented as a
	 * collection of properties, so that file can then be represented as a collection of key-value
	 * pairs (i.e. a Map).
	 * 
	 * @param input
	 *            the File to be standardized
	 */
	public abstract void standardize(File input);

	/**
	 * Clears the internal data structures.
	 */
	public final void clear() {
		data.clear();
	}

	/**
	 * Returns String representation of Parser data.
	 * 
	 * @return String representation of Parser data
	 */
	public final String toString() {
		String str = new String();
		if (data.size() != 0) {
			for (Map.Entry<String, Object> entry : data.entrySet()) {
				str += entry.getKey() + "=" + entry.getValue() + "\n";
			}
		} else if (!error) {
			str = "Parser data is empty--check file validity.";
		}
		return str;
	}

}