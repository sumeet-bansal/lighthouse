package parser;

import java.io.*;
import java.util.*;

/**
 * Abstract class that declares common methods and instance variables for each
 * file type parser.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public abstract class AbstractParser {

	protected String path;
	protected ArrayList<String> keys = new ArrayList<>();
	protected ArrayList<Object> vals = new ArrayList<>();
	protected Map<String, Object> data = new LinkedHashMap<>();
	protected boolean error = false;
	
	/**
	 * Getter method for the Map representation of the parsed data.
	 * @return the Map representation of the parsed data
	 */
	public Map<String, Object> getData() {
		for (int i = 0; i < keys.size(); i++) {
			data.put(keys.get(i), vals.get(i));
		}
		return data;
	}

	/**
	 * Getter method for file metadata.
	 * 
	 * @return file metadata (i.e. environment, fabric, node info)
	 */
	public Map<String, String> getMetadata() {
		String[] delimitedPath = path.split("/");
		String[] predictedPath = { "filename", "node", "fabric", "environment" };
		Map<String, String> metadata = new LinkedHashMap<>();
		for (int i = 0; i < predictedPath.length; i++) {
			metadata.put(predictedPath[i], delimitedPath[delimitedPath.length - 1 - i]);
		}
		metadata.put("path", path);
		return metadata;
	}

	/**
	 * Setter method for file path instance variable.
	 * 
	 * @param input
	 *            path of File being standardized
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
	 * Standardizes input File into separate ArrayLists for keys and values.
	 * 
	 * @param input
	 *            File to be standardized
	 */
	public abstract void standardize(File input);

	/**
	 * Clears the internal data structures.
	 */
	public void clear() {
		int size = keys.size();
		for (int i = 0; i < size; i++) {
			keys.remove(0);
			vals.remove(0);
		}
		data.clear();
		System.gc();
	}

	/**
	 * Returns representation of Parser data.
	 * 
	 * @return representation of Parser data
	 */
	public String toString() {
		String str = new String();
		if (keys.size() != 0) {
			for (int i = 0; i < keys.size(); i++) {
				str += (keys.get(i) + "=" + vals.get(i) + "\n");
			}
		} else if (!error) {
			str = "Key list is empty.";
		}
		return str;
	}

}