package Parser;

import java.io.*;

/**
 * Takes input file and parses according to file type.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 * @since 2017-06-27
 */
public class FileInputReader {
	private File input;
	private Standardizer data;
	
	public FileInputReader(File f) {
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
			data = new StandConf();
		} else if (fileType.equalsIgnoreCase("yaml") ||
				   fileType.equalsIgnoreCase("yml")) {
			data = new StandYaml();
		} else if (fileType.equalsIgnoreCase("properties")) {
			data = new StandProp();
		}
	}

	/**
	 * Adds File data to standardized ArrayLists.
	 */
	public void parseFile() {
		if (data != null) {
			data.standardize(input);
		} else {
			System.err.println("error: unsupported file type");
		}
	}
	
	/**
	 * Returns the File-specific parser.
	 * @return the File-specific parser
	 */
	public Standardizer getData() {
		if (data != null) { 
			return data;
		}
		System.err.println("error: unsupported file type");
		return null;
	}

	/**
	 * Clears the standardized ArrayLists.
	 */
	public void clearData() {
		if (data != null) {
			data.clear();
		} else {
			System.err.println("error: unsupported file type");
		} 
	}
}
