package parser;

import java.io.*;
import java.util.*;

/**
 * Abstract class that declares common methods and instance variables for each
 * file type parser.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 * @since 2017-07-06
 */
public abstract class AbstractParser {
	
	String path;
	ArrayList<String> keys = new ArrayList<>();
	ArrayList<Object> vals = new ArrayList<>();
	boolean error = false;
	
	/**
	 * Getter method for key ArrayList.
	 * @return the key ArrayList
	 */
	public ArrayList<String> getKeys() {
		
		// due to MongoDB constraints, all dot characters ('.') are being
		// converted to an extremely infrequently used substring--in this
		// case, a series of three backticks ("```").
		for (int i = 0; i < keys.size(); i++) {
			keys.get(i).replace(".", "```");
		}
		
		return keys;
	}
	
	/**
	 * Getter method for value ArrayList.
	 * @return the value ArrayList
	 */
	public ArrayList<Object> getVals() {
		
		// due to MongoDB constraints, all equal sign characters ('=') are
		// being converted to an extremely infrequently used substring--in
		// this case, a series of three at signs ("@@@").
		for (int i = 0; i < vals.size(); i++) {
			vals.get(i).toString().replace("=", "@@@");
		}
		return vals;
	}
	
	/**
	 * Getter method for file metadata.
	 * @return file metadata (i.e. environment, fabric, node info)
	 */
	public Map<String, String> getMetadata() {
		String[] delimitedPath = path.split("/");
		String[] predictedPath = {"filename", "node", "fabric", "environment"};
		Map<String, String> metadata = new LinkedHashMap<>();
		for (int i = 0; i < predictedPath.length; i++) {
			metadata.put(predictedPath[i], delimitedPath[delimitedPath.length-1-i]);
		}
		return metadata;
	}

	/**
	 * Setter method for file path instance variable.
	 * @param input path of File being standardized
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
	 * @param input File to be standardized
	 */
	public abstract void standardize(File input);

	/**
	 * Clears the internal key and value ArrayLists.
	 */
	public void clear() {
		int size = keys.size();
		for (int i = 0; i < size; i++) {
			keys.remove(0);
			vals.remove(0);
		}
	}

	/**
	 * Returns representation of Parser data.
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