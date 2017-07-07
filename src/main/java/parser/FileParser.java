package parser;

import java.io.*;

/**
 * Takes input file and parses according to file type.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 * @since 2017-07-06
 */
public class FileParser {
	private File input;
	private AbstractParser data;
	private String errorDescription;
	
	/**
	 * Constructor. Initializes internal File and Standardizer variables.
	 * @param f input File being read
	 */
	public FileParser(File f) {
		this.input = f;
		instantiateParser();
	}

	/**
	 * Determines the type of the File and instantiates the parser accordingly.
	 */
	public void instantiateParser() {
		String filename = input.getAbsolutePath();
		String fileType = filename.substring(filename.lastIndexOf('.') + 1);
		if (fileType.equalsIgnoreCase("config") ||
			fileType.equalsIgnoreCase("conf")) {
			data = new ParseConf();
		} else if (fileType.equalsIgnoreCase("yaml") ||
				   fileType.equalsIgnoreCase("yml")) {
			data = new ParseYaml();
		} else if (fileType.equalsIgnoreCase("properties")) {
			data = new ParseProp();
		} else if (input.getName().equalsIgnoreCase("hosts")) {
			data = new ParseHost();
		} else {
			errorDescription = filename;
			System.err.println("error: unsupported file type in " + errorDescription);
		}
		if (data != null) {
			data.setPath(filename);
		}
	}

	/**
	 * Adds File data to standardized ArrayLists.
	 */
	public void parseFile() {
		if (data != null) {
			data.standardize(input);
		}
	}
	
	/**
	 * Returns the File-specific parser.
	 * @return the File-specific parser
	 */
	public AbstractParser getData() {
		if (data != null) { 
			return data;
		}
		return null;
	}
	
	/**
	 * Getter method for error description.
	 * @return the error description
	 */
	public String getErrorDescription() {
		return errorDescription;
	}

	/**
	 * Clears the standardized ArrayLists.
	 */
	public void clearData() {
		if (data != null) {
			data.clear();
		} 
	}
}
