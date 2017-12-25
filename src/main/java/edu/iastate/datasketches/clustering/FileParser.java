
package edu.iastate.datasketches.clustering;

import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;

public class FileParser {

	public static void main(String[] args) throws Exception{
		File f = new File("E:/dataset/clean/shuttle.txt");
		Scanner sc = new Scanner(f);
		FileWriter fw = new FileWriter(new File("shuttle.txt"));
		while (sc.hasNextLine()) {
			// String str = "";
			String line = sc.nextLine();
			String[] attrs = line.split(",");
			if (attrs.length == 9) {
				fw.write(line + "\n");
			}
//			for (int i=1; i<attrs.length; i++) {
//				String[] nums = attrs[i].split(":");
//				str += nums[1] + ",";
//			}
//			str = str.substring(0, str.length()-1);
//			fw.write(str + "\n");
		}
		sc.close();
		fw.close();
	}

}
