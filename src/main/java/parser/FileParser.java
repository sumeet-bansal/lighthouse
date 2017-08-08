package parser;

import java.io.*;

/**
 * Takes input file and parses according to file type.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class FileParser {
	private File input;
	private AbstractParser data;
	private String errorDescription;

	/**
	 * Constructor. Initializes internal File and Standardizer variables.
	 * 
	 * @param f
	 *            input File being read
	 */
	public FileParser(File f) {
		this.input = f;
		instantiateParser();
	}

	/**
	 * Determines the type of the File and instantiates the parser accordingly.
	 */
	public void instantiateParser() {
		try {
			String filepath = input.getAbsolutePath();
			String fileType = filepath.substring(filepath.lastIndexOf('.') + 1);
			if (fileType.equalsIgnoreCase("config") || fileType.equalsIgnoreCase("conf")
					|| fileType.equalsIgnoreCase("cfg")) {
				data = new ParseConf();
			} else if (fileType.equalsIgnoreCase("yaml") || fileType.equalsIgnoreCase("yml")) {
				data = new ParseYaml();
			} else if (fileType.equalsIgnoreCase("properties") || fileType.equalsIgnoreCase("prop")
					|| fileType.equalsIgnoreCase("env")) {
				data = new ParseProp();
			} else if (input.getName().equalsIgnoreCase("hosts")) {
				data = new ParseHost();
			} else if (fileType.equalsIgnoreCase("xml")) {
				data = new ParseXML();
			} else if (fileType.equalsIgnoreCase("info")) {
				data = new ParseInfo();
			} else if (fileType.equalsIgnoreCase("whitelist") || fileType.equalsIgnoreCase("blacklist")) {
				data = new ParseList();
			} else if (fileType.equalsIgnoreCase("keyring") || fileType.equalsIgnoreCase("gateway")) {
				data = new ParseCephData();
			} else {
				String[] arr = filepath.split("\\.");
				String backup = arr[arr.length - 1];
				String end = backup.substring(backup.length() - 2, backup.length());
				if (!((backup.charAt(0) == 'b' && end.equals("up")) || backup.equals("debug")
						|| backup.equals("workingCopy") || backup.equals("mp"))) {
					errorDescription = "unsupported: " + filepath;
				}
				data = null;
			}
			if (data != null) {
				data.setPath(filepath);
			}
		} catch (Exception e) {
			errorDescription = e.getMessage();
			data = null;
		}
	}

	/**
	 * Adds File data to standardized ArrayLists.
	 * 
	 * @return true if the File was successfully parsed, else false
	 */
	public boolean parseFile() {
		if (data != null) {
			data.standardize(input);
			return true;
		}
		return false;
	}

	/**
	 * Returns the File-specific parser.
	 * 
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
	 * 
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
