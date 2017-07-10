package parserTester;

import cachingLayer.Comparator;

public class ComparatorTester {

	public static void main(String[] args) {
		Comparator c = new Comparator("C:/Test root");
		String c1 = "Test root/*/fabric1/*/ceph.conf";
		String c2 = "Test root/*/fabric2/*/ceph.conf";
		c.query(c1, c2);
	}
}
