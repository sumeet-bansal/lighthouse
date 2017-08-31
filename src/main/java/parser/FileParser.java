package parser;

import java.io.*;

/**
 * Takes input file and parses according to file type.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class FileParser {
	private File root;
	private File input;
	private AbstractParser data;
	private String errorDescription;

	/**
	 * Constructor. Initializes internal File and Standardizer variables.
	 * 
	 * @param f
	 *            input File being read
	 */
	public FileParser(File root, File f) {
		this.root = root;
		this.input = f;
		instantiateParser();
	}

	/**
	 * Determines the type of the File and instantiates the parser accordingly.
	 */
	public void instantiateParser() {
		try {
			String rootpath = root.getAbsolutePath();
			String filepath = input.getAbsolutePath();
			String fileType = filepath.substring(filepath.lastIndexOf('.') + 1).toLowerCase();
			switch(fileType) {
			case "cfg":
			case "conf":
			case "config":
				data = new ParseConf();
				break;
			case "yml":
			case "yaml":
				data = new ParseYaml();
				break;
			case "env":
			case "prop":
			case "properties":
				data = new ParseProp();
				break;
			case "hosts":
				data = new ParseHosts();
				break;
			case "xml":
				data = new ParseXML();
				break;
			case "info":
				data = new ParseInfo();
				break;
			case "whitelist":
			case "blacklist":
				data = new ParseList();
				break;
			case "keyring":
			case "gateway":
				data = new ParseCephData();
				break;
			case "ignore":
				data = new ParseIgnore();
				data.setInternal(true);
				break;
			default:
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
				data.setPath(rootpath, filepath);
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
		return data;
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
