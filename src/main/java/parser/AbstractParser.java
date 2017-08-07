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
	 * @return a Map representation of the parsed data
	 */
	public Map<String, Object> getData() {
		return data;
	}

	/**
	 * Getter method for the file metadata (e.g. environment, fabric, node).
	 * @return a Map representation of the file metadata
	 */
	public Map<String, String> getMetadata() {
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
		return metadata;
	}

	/**
	 * Setter method for file path instance variable.
	 * @param path the path of the File being standardized
	 */
	public void setPath(String path) {

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
	 * Standardizes input File into a Map of keys and values.
	 * @param input the File to be standardized
	 */
	public abstract void standardize(File input);

	/**
	 * Clears the internal data structures.
	 */
	public void clear() {
		data.clear();
		System.gc();
	}

	/**
	 * Returns representation of Parser data.
	 * @return representation of Parser data
	 */
	public String toString() {
		String str = new String();
		if (data.size() != 0) {
			for (Map.Entry<String, Object> entry : data.entrySet()) {
				str += entry.getKey() + "=" + entry.getValue() + "\n";
			}
		} else if (!error) {
			str = "Map is empty.";
		}
		return str;
	}

}