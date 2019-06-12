package util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public final class Config {
	
	@SuppressWarnings("unchecked")
	public static Map<String, ?> m = (Map<String, ?>) parseYaml(Paths.get("src/main/resources/config.yaml"));
	
	@SuppressWarnings("unchecked")
	public static Map<String, ?> getTargetList() {
		return (Map<String, ?>) m.get("target_list");
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String, ?> getTools() {
		return (Map<String, ?>) m.get("tools");
	}
	
		
	/**
	 * Independent function, parse the .yaml file and return a map object. 
	 * @param cf
	 * @return
	 */
	public static Map<?, ?> parseYaml(Path cf) {
		Yaml yaml = new Yaml();
		try {
			return yaml.load(new FileInputStream(cf.toFile()));
		} catch (FileNotFoundException e) {
			// e.printStackTrace();
		}
		return null;
	}

//	public static void updateBuild(Path cf, Map<String, String> map) {
//		String secn = "build";
//		try {
//			DumperOptions dumperOptions = new DumperOptions();
//			dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
//			dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
//			dumperOptions.setPrettyFlow(false);
//			Yaml yaml = new Yaml(dumperOptions);
//			Map m = yaml.load(new FileInputStream(cf.toFile()));
//			if (m == null) {
//				m = new LinkedHashMap();
//			}
//			if (m.containsKey(secn)) {
//				Map<String, String> mm = (Map<String, String>) m.get(secn);
//				mm.putAll(map);
//			} else {
//				m.put(secn, map);
//			}
//			yaml.dump(m, new OutputStreamWriter((new FileOutputStream(cf.toFile()))));
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

}
