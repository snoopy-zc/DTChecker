package start;

import com.ibm.wala.cfg.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import com.ibm.wala.*;

public class Start {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		
		Path cfg = Paths.get("src/main/resources/config.yaml");
		System.out.println(cfg.toString());
		System.out.println(cfg.toAbsolutePath().toString());
		
		Map m = parseYaml(cfg);
		
		System.out.println(m);
		System.out.println(m.get("title"));
		

		System.out.println(m.get("title").getClass());
		
        
		System.out.println("hello world!");
		
	}

	private static Map parseYaml(Path cf) {
		Yaml yaml = new Yaml();
		try {
			return yaml.load(new FileInputStream(cf.toFile()));
		} catch (FileNotFoundException e) {
			// e.printStackTrace();
		}
		return null;
	}
//
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