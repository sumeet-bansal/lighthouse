package parser;

import java.io.*;
import java.util.*;

/**
 * Abstract class that declares common methods and instance variables for each
 * file type parser.
 * 
 * @author ActianceEngInterns
 * @version 1.1
 */
public abstract class AbstractParser {

	protected String path;
	protected Map<String, Object> data = new LinkedHashMap<>();
	protected boolean error = false;

	/**
	 * Getter method for the parsed data.
	 * 
	 * @return A Map representation of the parsed data: each property in a
	 * file can be represented as a key-value pair (i.e. a Map entry) and a
	 * single file can be represented as a collection of properties, so that
	 * file can then be represented as a collection of key-value pairs (i.e. a
	 * Map).
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
		String[] delimitedPath = path.split("/");
		String[] predictedPath = { "filename", "node", "fabric", "environment" };
		String relativePath = "";
		Map<String, String> metadata = new LinkedHashMap<>();
		for (int i = 0; i < predictedPath.length; i++) {
			metadata.put(predictedPath[i], delimitedPath[delimitedPath.length - 1 - i]);
			relativePath = delimitedPath[delimitedPath.length - 1 - i] + "/" + relativePath;
		}
		relativePath = relativePath.substring(0, relativePath.length() - 1);
		metadata.put("path", relativePath);
		if (metadata.get("filename").equals("hosts")) {
			metadata.put("extension", "hosts");
		} else {
			metadata.put("extension", relativePath.substring(relativePath.lastIndexOf('.')+1));
		}
		return metadata;
	}

	/**
	 * Setter method for file path instance variable.
	 * 
	 * @param path
	 *            the path of the File being standardized
	 */
	public final void setPath(String path) {

		// necessary because Windows file delimiters throw off String methods
		String p = "";
		for (int i = 0; i < path.length(); i++) {
			if (path.charAt(i) != '\\') {
				p += path.charAt(i);
			} else {
				p += "/";
			}
		}

		this.path = p;
	}

	/**
	 * Standardizes input File into a Map of keys and values. Each property in a
	 * file can be represented as a key-value pair (i.e. a Map entry) and a
	 * single file can be represented as a collection of properties, so that
	 * file can then be represented as a collection of key-value pairs (i.e. a
	 * Map).
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