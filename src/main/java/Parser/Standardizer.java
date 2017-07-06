package Parser;

import java.io.*;
import java.util.ArrayList;

/**
 * Abstract class that declares common methods and instance variables for each
 * file type parser.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 * @since 2017-06-27
 */
public abstract class Standardizer {
	
	static ArrayList<String> keys = new ArrayList<>();
	static ArrayList<Object> vals = new ArrayList<>();
	static boolean error = false;
	
	/**
	 * Getter method for key ArrayList.
	 * @return the key ArrayList
	 */
	public ArrayList<String> getKeys() {
		
		// due to MongoDB constraints, all dot characters (.) are being
		// converted to an extremely infrequently used substring--in this
		// case, a series of three backticks ("```").
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			while (key.indexOf('.') != -1) {
				int dotIndex = key.indexOf('.');
				String pre = key.substring(0, dotIndex);
				String post = key.substring(dotIndex+1);
				key = pre + "```" + post;
			}
			keys.set(i, key);
		}
		
		return keys;
	}
	
	/**
	 * Getter method for value ArrayList.
	 * @return the value ArrayList
	 */
	public ArrayList<Object> getVals() {
		return vals;
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