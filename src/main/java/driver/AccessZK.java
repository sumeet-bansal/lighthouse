package driver;

import zookeeperModule.*;

/**
 * Runs ZKGenerator from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class AccessZK {
	
	/**
	 * Accesses a ZooKeeper instance to generate .properties from a given
	 * environment.
	 * @param args command-line arguments
	 */
	public static void run(String[] args) {
		
		// describes proper usage of 'zk' command
		if (args.length < 3) {
				System.err.println("Usage: java -jar <jar file> zk "
						+ "<host IP> <root folder> <environment name> "
						+ "<exception1> <exception2> ... <exceptionN>");
				return;
		}

		// initializes and utilizes ZKGenerator, handles specified branch exceptions
		ZKGenerator generator = new ZKGenerator(args[0], args[1], args[2]);
		for (int i = 3; i < args.length; i++) {
			generator.addException(args[i]);
		}
		generator.generate();
			
	}
}