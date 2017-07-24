package driver;

public class Access {
	/**
	 * User specifies whether to access Database ("db") or Comparator ("query") and
	 * passes arguments to their "run" classes
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {

			String cmd = args[0];
			String[] pass = new String[args.length - 1];
			for (int i = 1; i < args.length; i++) {
				pass[i - 1] = args[i];
			}

			switch (cmd) {
			case "db":
				AccessDB.run(pass);
				break;
			case "query":
				AccessUI.run(pass);
				break;
			default:
				System.err.println("Invalid input! Please specify \"db\" or \"query\" before passing arguments!");
				return;
			}

		} catch (Exception e) {
			System.err.println("Invalid input! Please specify \"db\" or \"query\" before passing arguments!");
		}
	}
}