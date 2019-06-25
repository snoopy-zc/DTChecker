package start;

import java.util.Map;

import util.Config;

public class Start {

	public static void main(String[] args) {

		Map<?, ?> targetList = Config.getTargetList();		
		//System.out.println(targetList);
		for (Object targetID : targetList.keySet()) {
			//System.out.println("\n\n\n\n" + targetID);
			//System.out.println(targetList.get(targetID));
			@SuppressWarnings("unchecked")
			Map<String, String> arg = (Map<String, String>) targetList.get(targetID);
			new AnalysisProcessor(arg);
		}
	}
}