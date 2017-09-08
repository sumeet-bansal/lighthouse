package databaseModuleTester;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import databaseModule.DirTree;

/**
 * Tests {@link databaseModule.DirTree}.
 * 
 * @author Sumeet Bansal
 * @version 1.3.0
 */
public class DirTreeTester {

	ArrayList<String> paths;
	DirTree tree;

	/**
	 * Sets up the testbed by populating the DirTree.
	 */
	@Before
	public void setup() {
		tree = new DirTree();
		paths = new ArrayList<>();
		int dev, fabric, node, file;
		dev = fabric = node = file = 3;
		for (int i = 0; i < dev; i++) {
			for (int j = 0; j < fabric; j++) {
				for (int k = 0; k < node; k++) {
					for (int l = 0; l < file; l++) {
						paths.add("dev" + i + "/fabric" + j + "/node" + k + "/file" + l);
					}
				}
			}
		}
		paths.add("file");
		paths.add("dev1/fabric2/file");
		for (String path : paths) {
			tree.insert(path);
		}
	}

	/**
	 * Tests {@link databaseModule.DirTree#insert(java.lang.String)} and
	 * {@link databaseModule.DirTree#hasKey(java.lang.String)}.
	 */
	@Test
	public void testInsert() {
		tree = new DirTree();
		for (String path : paths) {
			assertFalse(tree.hasKey(path));
			tree.insert(path);
			assertTrue(tree.hasKey(path));
		}
		assertEquals(paths.size(), tree.getSize());

		try {
			tree.insert(null);
			assertTrue(false);
		} catch (NullPointerException e) {
			assertTrue(true);
		}
	}

	/**
	 * Tests {@link databaseModule.DirTree#getChildren(java.lang.String)}.
	 */
	@Test
	public void testGetChildren() {

		// verifies child at every level
		int dev, fabric, node, file;
		dev = fabric = node = file = 3;
		for (int i = 0; i < dev; i++) {
			for (int j = 0; j < fabric; j++) {
				assertTrue(tree.getChildren("dev" + i).contains("fabric" + j));
				for (int k = 0; k < node; k++) {
					assertTrue(tree.getChildren("dev" + i + "/fabric" + j).contains("node" + k));
					for (int l = 0; l < file; l++) {
						assertTrue(tree.getChildren("dev" + i + "/fabric" + j + "/node" + k).contains("file" + l));
					}
				}
			}
		}
		assertTrue(tree.getChildren("").contains("file"));
		assertTrue(tree.getChildren("dev1/fabric2").contains("file"));

		// verifies throwing NullPointerException
		try {
			tree.getChildren(null);
			assertTrue(false);
		} catch (NullPointerException e) {
			assertTrue(true);
		}

		// verifies throwing IllegalArgumentException
		try {
			tree.getChildren("BENG-Dev");
			assertTrue(false);
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	/**
	 * Tests
	 * {@link databaseModule.DirTree#countNodes(databaseModule.DirTree.DirNode, int, boolean)}.
	 */
	@Test
	public void testCountNodes() {

		// verifies directories at each level
		assertEquals(tree.countNodes(tree.getRoot(), 1, true), 3);
		assertEquals(tree.countNodes(tree.getRoot(), 2, true), 9);
		assertEquals(tree.countNodes(tree.getRoot(), 3, true), 27);
		assertEquals(tree.countNodes(tree.getRoot(), 4, true), 0);

		// verifies directories and files at each level
		assertEquals(tree.countNodes(tree.getRoot(), 1, false), 4);
		assertEquals(tree.countNodes(tree.getRoot(), 3, false), 28);
		assertEquals(tree.countNodes(tree.getRoot(), 4, false), 81);
	}

}
