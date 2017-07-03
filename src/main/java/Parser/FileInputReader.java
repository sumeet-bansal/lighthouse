package parser;

import java.io.*;

/**
 * Takes input file and parses according to file type.
 * 
 * @author ActianceEngInterns
 * @version 1.1
 * @since 2017-06-30
 */
public class FileInputReader {
	private File input;
	private Standardizer data;
	private String errorDescription;

	public FileInputReader(File f) {
		this.input = f;
		instantiateParser();
	}
	
	/**
	 * Getter method for errorDescription
	 * @return errorDescription
	 */
	public String getErrorDescription() {
		return errorDescription;
	}

	/**
	 * Determines the type of the File and instantiates the parser accordingly.
	 */
	public void instantiateParser() {
		String filename = input.getAbsolutePath();
		String fileType = filename.substring(filename.lastIndexOf('.') + 1);
		if (fileType.equalsIgnoreCase("config") || fileType.equalsIgnoreCase("conf")) {
			data = new StandConf();
		} else if (fileType.equalsIgnoreCase("yaml") || fileType.equalsIgnoreCase("yml")) {
			data = new StandYaml();
		} else if (fileType.equalsIgnoreCase("properties")) {
			data = new StandProp();
		} else if (input.getName().equals("hosts")) {
			data = new StandHost();
		} else {
			errorDescription = input.getName();
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
	 * 
	 * @return the File-specific parser
	 */
	public Standardizer getData() {
		parseFile();
		if (data != null) {
			return data;
		}
		return null;
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
