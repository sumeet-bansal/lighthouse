package parser;

import java.io.*;
import java.util.*;

/**
 * Standardizes .property files.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class ParseProp extends AbstractParser {
	private Properties prop = new Properties();

	/**
	 * Standardizes input File into a Map of keys and values.
	 * @param input the File to be standardized
	 */
	public void standardize(File input) {
		
		FileInputStream fins = null;
		try {
			fins = new FileInputStream(input);
			if (fins != null) {
				prop.load(fins);
			}
		} catch (Exception e) {
			String name = input.getAbsolutePath();
			System.err.println("\n[DATABASE ERROR] " + name + " is not in correct .properties format.\n");
			error = true;
			return;
		}
		
		ArrayList<String> keys = new ArrayList<>();
		keys.addAll(prop.stringPropertyNames());
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			data.put(key, prop.getProperty(key));
		}
	}
	
}