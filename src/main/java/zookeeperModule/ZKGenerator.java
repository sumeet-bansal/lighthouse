package zookeeperModule;

import java.util.*;
import java.io.*;

/**
 * Generates .properties files for all fabrics in an environment.
 * 
 * @author ActianceEngInterns
 * @version 1.1
 */
public class ZKGenerator {
	
	private ZKClientManager zkmanager;
	private String basepath = "/alcatrazproperties/2.5", fabric;
	private Map<String, String> map;
	private String root, env;
	
	// separates specific branches from .properties for UI readability
	private Map<String, ArrayList<String>> exceptions = new HashMap<>();
	
	/**
	 * Constructor.
	 * @param host the host being connected to
	 * @param root the root directory being written to
	 * @param environment the name of the environment
	 */
	public ZKGenerator(String host, String root, String environment) {
		this.root = root;
		System.out.println(root + environment);
		if (root.charAt(root.length()-1) != '/') {
			this.root +="/";
		}
		env = environment;
		if (env.charAt(env.length()-1) != '/') {
			env +="/";
		}
		zkmanager = new ZKClientManager(host);
		exceptions.put("properties", new ArrayList<>());
	}
	
	/**
	 * Runs the .properties file generator.
	 */
	public void generate() {
		
		// gets all fabrics in environment
		List<String> fabrics = zkmanager.getZNodeChildren(basepath);
		
		for (int i = 0; i < fabrics.size(); i++) {
			
			// generates directory structure
			new File(root + env + fabrics.get(i) + "/common").mkdirs();
			
			// generates .properties files
			map = new LinkedHashMap<>();
			fabric =  fabrics.get(i);
			recursive(basepath + "/" + fabric);
			write(fabric, map);
			System.out.println("generated .properties file(s) for " + fabric);
		}
	}	
	
	/**
	 * Recursively traverses ZNodes to generate full path keys.
	 * @param path the path of the current ZNode
	 */
	private void recursive(String path) {
		
		// base case: current ZNode is leaf
		if (zkmanager.getZNodeStats(path).getNumChildren() == 0) {
			String key = path.substring(basepath.length()+1);
			String data = (String) zkmanager.getZNodeData(path, false);
			map.put(key.substring(fabric.length() + 1), data);
			return;
		}
		
		// recursive case: iterates through all children
		List<String> children = zkmanager.getZNodeChildren(path);
		for (int i = 0; i < children.size(); i++) {
			recursive(path + "/" + children.get(i));
		}
	}
	
	/**
	 * Writes fabric data to .properties files.
	 * @param fabric the current fabric
	 * @param map a Map of root-to-leaf keys and respective data from fabric
	 */
	private void write(String fabric, Map<String, String> map) {
		try {
			String directory = root + env + fabric + "/common/";
			
			Writer writer = new BufferedWriter(new OutputStreamWriter(
			          new FileOutputStream(directory + "server.properties"),
			          "utf-8"));
			for (Map.Entry<String, String> entry : map.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();

				// sorts excepted properties into separate branches
				boolean excepted = false;
				ArrayList<String> exceptedProps = exceptions.get("properties");
				for (int i = 0; i < exceptedProps.size(); i++) {
					String prop = exceptedProps.get(i);
					if (entry.getKey().startsWith(prop)) {
					    addException(prop, key + "=" + value + "\n");
					    excepted = true;
					}	
				}

				if (!excepted) {
					writer.write(key + "=" + value + "\n");
				}
				
			}
		    writer.close();

			// writes each excepted property branch to own separate .properties file
		    for (Map.Entry<String, ArrayList<String>> entry : exceptions.entrySet()) {
		    	if (!entry.getKey().equals("properties")) {
					if (entry.getValue().size() > 0) {
						writer = new BufferedWriter(new OutputStreamWriter(
						          new FileOutputStream(directory +
						          "server." + entry.getKey() + ".properties"), "utf-8"));
						for (int i = 0; i < entry.getValue().size(); i++) {
							writer.write(entry.getValue().get(i));
						}
						writer.close();
					}
		    	}
		    }
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds a property branch to the list of excepted property branches.
	 * @param prop the excepted property
	 */
	public void addException(String prop) {
		exceptions.get("properties").add(prop);
		exceptions.put(prop, new ArrayList<>());
	}
	
	/**
	 * Adds a specific property to the excepted branch instead of the normal
	 * .properties file.
	 * @param prop the base property
	 * @param line the full-path property and its respective value
	 */
	private void addException(String prop, String line) {
		exceptions.get(prop).add(line);
	}
	
}
