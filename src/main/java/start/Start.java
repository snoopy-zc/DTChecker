package start;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;


import util.Config;
import util.Timer;
import wala.WalaAnalyzer;

public class Start {

	public static void main(String[] args) {
		
		Map<?, ?> targetList = Config.getTargetList();
		System.out.println(targetList);
		
		for(Object targetID: targetList.keySet()) {
			System.out.println(targetID);
			System.out.println(targetList.get(targetID));
			
			@SuppressWarnings("unchecked")
			Map<String,String> arg = (Map<String, String>) targetList.get(targetID);	
			
			Timer timer = new Timer(Paths.get(arg.get("timer_out")).toAbsolutePath());
			
			timer.tic("WalaAnalyzer begin");
			
			WalaAnalyzer walaAnalyzer = new WalaAnalyzer(arg);
			
			timer.toc("WalaAnalyzer end");			
		}
	}
}