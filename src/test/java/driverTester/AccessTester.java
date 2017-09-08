package driverTester;

import static org.junit.Assert.*;

import org.junit.*;

import driver.Access;

/**
 * Tests {@link driver.Access}.
 * 
 * @author Sumeet Bansal
 * @version 1.3.0
 */
public class AccessTester {

	/**
	 * Tests {@link driver.Access#parseArgs(java.lang.String)}.
	 */
	@Test
	public void testParseArgs() {
		String command;
		String[] expected;

		command = "   db    clear --yes  ";
		expected = new String[3];
		expected[0] = "db";
		expected[1] = "clear";
		expected[2] = "--yes";
		assertArrayEquals(Access.parseArgs(command), expected);

		command = " populate \"root\"";
		expected = new String[2];
		expected[0] = "populate";
		expected[1] = "root";
		assertArrayEquals(Access.parseArgs(command), expected);

		command = " populate \"spaced root\"";
		expected = new String[2];
		expected[0] = "populate";
		expected[1] = "spaced root";
		assertArrayEquals(Access.parseArgs(command), expected);

		command = " populate \"New folder\"\\\"root - Copy\"";
		expected = new String[2];
		expected[0] = "populate";
		expected[1] = "New folder\\root - Copy";
		assertArrayEquals(Access.parseArgs(command), expected);

		command = " populate \"New folder\\root - Copy\"";
		expected = new String[2];
		expected[0] = "populate";
		expected[1] = "New folder\\root - Copy";
		assertArrayEquals(Access.parseArgs(command), expected);
	}

	/**
	 * Tests {@link driver.Access#parseStatements(java.lang.String)}.
	 */
	@Test
	public void testParseStatements() {
		String command;
		String[] expected;

		command = "db clear -y; populate root";
		expected = new String[2];
		expected[0] = "db clear -y";
		expected[1] = "populate root";
		assertArrayEquals(Access.parseStatements(command), expected);

		command = "db  clear   --yes;  asdf  ;;;;a; b;c ; populate   \"spaced   root\"";
		expected = new String[6];
		expected[0] = "db clear --yes";
		expected[1] = "asdf";
		expected[2] = "a";
		expected[3] = "b";
		expected[4] = "c";
		expected[5] = "populate \"spaced root\"";
		assertArrayEquals(Access.parseStatements(command), expected);

		command = "db clear --yes; populate \"test roots\"/\"messy\"; query grep \"multi grep\" ";
		expected = new String[3];
		expected[0] = "db clear --yes";
		expected[1] = "populate \"test roots\"/\"messy\"";
		expected[2] = "query grep \"multi grep\"";
		assertArrayEquals(Access.parseStatements(command), expected);

		command = "query grep \"ingestion\"; query find -l RWC-Dev \"lfs/ingestion/topics\"";
		expected = new String[2];
		expected[0] = "query grep \"ingestion\"";
		expected[1] = "query find -l RWC-Dev \"lfs/ingestion/topics\"";
		assertArrayEquals(Access.parseStatements(command), expected);

		command = "db clear -y; populate root; query compare RWC-Dev/storm/*/*.properties";
		expected = new String[3];
		expected[0] = "db clear -y";
		expected[1] = "populate root";
		expected[2] = "query compare RWC-Dev/storm/*/*.properties";
		assertArrayEquals(Access.parseStatements(command), expected);
	}
}
