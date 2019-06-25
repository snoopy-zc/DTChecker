import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Test implements Runnable {
//public class Test extends Thread{
	private static final String ArrayList = null;
	Set<Integer> mySet;
	boolean myF;
	int[] a;

	public void run() {
		for (int i = 0; i < 10; i++)
			// System.out.println("hello....sb");
			this.mySet.add(5);

		int a = 3;
		int b = 9;
		int x = a + b;
		int y = b - a;
		int z = a - y;
		testFor1(x, z);

	}

	public static void main(String[] args) {

		Set<Integer> z = new HashSet<Integer>();
		Test a = new Test(z, true);
		(new Thread(a)).start(); // for implements
		
		
	}

}
