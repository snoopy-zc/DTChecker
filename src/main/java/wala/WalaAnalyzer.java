package wala;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.io.FileUtil;
import util.TextFileReader;

public class WalaAnalyzer {

	Path dirpath; // Target dir. Should also support single *.jar file, But for now NOT NOT NOT
	Map<String,String> args;

	// WALA basis
	AnalysisScope scope;
	ClassHierarchy cha;
	HashSet<Entrypoint> entrypoints;
	CallGraph cg;
	public List<String> packageScopePrefixes = new ArrayList<String>(); // read from 'package-scope.txt' if exists

	// Statistics
	int nPackageFuncs = 0; // the real functions we focuses //must satisfy
							// "isApplicationAndNonNativeMethod" first
	int nTotalFuncs = 0;
	int nApplicationFuncs = 0;
	int nPremordialFuncs = 0;
	int nOtherFuncs = 0;

	// Others
	private final static boolean CHECK_GRAPH = false;

	/**
	 * Input - 1. a dir path including *.jar or *.class, 2. 3.
	 */
	public WalaAnalyzer(Map<String,String> args) {
		
		Path dirpath = Paths.get(args.get("pkgDir"));
		
		if (!Files.exists(dirpath)) {
			System.out.println("ERROR - " + "Files do not exist @ " + dirpath + " for Wala");
			return;
		}
		this.dirpath = dirpath;
		this.args = args;
		doWork();
	}

	public Path getTargetDirPath() {
		return this.dirpath;
	}
	
	public Map<String,String> getTargetArgs() {
		return this.args;
	}

	public CallGraph getCallGraph() {
		return this.cg;
	}

	public ClassHierarchy getClassHierarchy() {
		return this.cha;
	}

	public int getNPackageFuncs() {
		return nPackageFuncs;
	}

	public int getNTotalFuncs() {
		return nTotalFuncs;
	}

	public int getNApplicationFuncs() {
		return nApplicationFuncs;
	}

	public int getNPremordialFuncs() {
		return nPremordialFuncs;
	}

	public int getNOtherFuncs() {
		return nOtherFuncs;
	}

	private void doWork() {
		System.out.println("INFO - WalaAnalyzer: doWork...");

		try {
			walaAnalysis(getJarsOrOthers());
			infoWalaAnalysisEnv();
			readPackageScope();
		} catch (IllegalArgumentException | CallGraphBuilderCancelException | IOException | UnsoundGraphException
				| WalaException e) {
			e.printStackTrace();
		}
	}

	/********************************************************************************
	 * Functions Region
	 *******************************************************************************/


	/**
	 * @param alljars - absolute path or relative path, can be one of the following
	 *                1. multiple .jar files separated by ":"(linux) or ";"(win),
	 *                "src/sa/res/ca-6744/xx.jar;xxx/xx.jar" 2. a class-file dir,
	 *                can be any-level dir, like "bin/sa", "bin/sa/test",
	 *                "bin/sa/test/testsrc", will identify all *.class recursively
	 *                and overlook *.jar 3. single .jar file,
	 *                "src/sa/res/ca-6744/xx.jar" 4. single .class file
	 */
	private void walaAnalysis(String alljars) throws IOException, IllegalArgumentException,
			CallGraphBuilderCancelException, UnsoundGraphException, WalaException {
		System.out.println("INFO - WalaAnalyzer: walaAnalysis...");

		// Create a Scope #"JXJavaRegressionExclusions.txt"
		scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(alljars,
				(new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS)); // default:
																						// CallGraphTestUtil.REGRESSION_EXCLUSIONS
		// Create a Class Hierarchy
		cha = ClassHierarchyFactory.make(scope);
		// testTypeHierarchy();

		// Create a Entry Points
		entrypoints = new HashSet<Entrypoint>();
		Iterable<Entrypoint> allappentrypoints = new AllApplicationEntrypoints(scope, cha); // Usually: entrypoints =
																							// com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope,
																							// cha); //get main
																							// entrypoints
		// Get all entry points
		entrypoints = (HashSet<Entrypoint>) allappentrypoints;
		// TODO - can narrow entrypoints according to "scope.txt"

		// Create Analysis Options
		AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
		options.setReflectionOptions(ReflectionOptions.ONE_FLOW_TO_CASTS_NO_METHOD_INVOKE); // ReflectionOptions.FULL
																							// will just cause a few
																							// more nodes and methods

		// Create a builder - default: Context-insensitive
		// #makeZeroCFABuilder(options, new AnalysisCache(), cha, scope, null, null);
		// #makeVanillaZeroOneCFABuilder(options, new AnalysisCache(), cha, scope, null,
		// null); // this will take 20+ mins to finish
		// #makeZeroOneCFABuilder(options, new AnalysisCache(), cha, scope, null, null);
		// #makeZeroOneContainerCFABuilder(options, new AnalysisCache(), cha, scope,
		// null, null);
		CallGraphBuilder builder = Util.makeZeroCFABuilder(options, new AnalysisCache(null, null, null), cha,
				scope, null, null);
		// Context-sensitive
		/*
		 * com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha);
		 * com.ibm.wala.ipa.callgraph.impl.Util.addDefaultBypassLogic(options, scope,
		 * Util.class.getClassLoader(), cha); //ContextSelector contextSelector = new
		 * DefaultContextSelector(options); //SSAContextInterpreter contextInterpreter =
		 * new DefaultSSAInterpreter(options, Cache); SSAPropagationCallGraphBuilder
		 * builder = new nCFABuilder(1, cha, options, new AnalysisCache(), null, null);
		 * AllocationSiteInNodeFactory factory = new
		 * AllocationSiteInNodeFactory(options, cha); builder.setInstanceKeys(factory);
		 */

		// Build the call graph JX: time-consuming
		cg = builder.makeCallGraph(options, null);
		System.out.println(CallGraphStats.getStats(cg));

		// Get pointer analysis results
		/*
		 * PointerAnalysis pa = builder.getPointerAnalysis(); HeapModel hm =
		 * pa.getHeapModel(); //JX: #getHeapModel's reslult is com.ibm
		 * .wala.ipa.callgraph.propagation.PointerAnalysisImpl$HModel@24ccf6a8
		 * BasicHeapGraph hg = new BasicHeapGraph(pa, cg); System.err.println(hg);
		 */
		// System.err.println(builder.getPointerAnalysis().getHeapGraph());

		if (CHECK_GRAPH) {
			GraphIntegrity.check(cg);
		}
	}

	private void infoWalaAnalysisEnv() {
		System.out.println("INFO - WalaAnalyzer: infoWalaAnalysisEnv");

		int nAppNatives = 0;
		int nPriNatives = 0;
		int nOthNatives = 0;

		for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext();) {
			CGNode f = it.next();
			nTotalFuncs++;
			if (isApplicationMethod(f)) {
				nApplicationFuncs++;
				if (isNativeMethod(f))
					nAppNatives++;
			} else if (isPrimordialMethod(f)) {
				nPremordialFuncs++;
				if (isNativeMethod(f))
					nPriNatives++;
			} else {
				nOtherFuncs++;
				if (isNativeMethod(f))
					nOthNatives++;
			}

			if (isInPackageScope(f)) {
				nPackageFuncs++;
			}
		}

		System.out.println("nTotalFuncs(" + nTotalFuncs + ") = nApplicationFuncs(" + nApplicationFuncs
				+ ") + nPremordialFuncs(" + nPremordialFuncs + ") + nOtherFuncs(" + nOtherFuncs + ")");
		System.out.println(
				"\t" + "nApplicationFuncs(" + nApplicationFuncs + ") includes " + nAppNatives + " native methods");
		System.out.println(
				"\t" + "nPremordialFuncs(" + nPremordialFuncs + ") includes " + nPriNatives + " native methods");
		System.out.println("\t" + "nOtherFuncs(" + nOtherFuncs + ") includes " + nOthNatives + " native methods");
		System.out.println(
				"nPackageFuncs(" + nPackageFuncs + ") - Note: this should be isApplicationAndNonNativeMethod first");
	}
	
	

	private void readPackageScope() {
		System.out.println("INFO - WalaAnalyzer: readPackageScope");
		
		String filepath = this.args.get("scope_file");

		File f = new File(filepath);

		if (!f.exists()) {
			System.out.println("NOTICE - not find the 'package-scope.txt' file, so SCOPE is ALL methods!!");
			return;
		}

		TextFileReader reader;
		String tmpline;
		try {
			reader = new TextFileReader(filepath);
			while ((tmpline = reader.readLine()) != null) {
				String[] strs = tmpline.split("\\s+");
				packageScopePrefixes.add(strs[0]);
			}
			reader.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("ERROR - when reading package-scpoe.txt files");
			e.printStackTrace();
		}
		System.out.print("NOTICE - successfully read the 'package-scope.txt' file as SCOPE, including:");
		for (String str : packageScopePrefixes)
			System.out.print(" " + str);
		System.out.println();

	}

	// must satisfy "isApplicationAndNonNativeMethod" first
	public boolean isInPackageScope(CGNode cgNode) {
		// added
		if (!isApplicationAndNonNativeMethod(cgNode))
			return false;
		// if without 'package-scope.txt'
		if (packageScopePrefixes.size() == 0)
			return true;
		String signature = cgNode.getMethod().getSignature();
		for (String str : packageScopePrefixes)
			if (signature.startsWith(str))
				return true;
		return false;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Static Methods - Begin
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static boolean isApplicationAndNonNativeMethod(CGNode f) {
		if (isApplicationMethod(f) && !isNativeMethod(f)) // IMPO: some native methods are App class, but can't
															// IR#getControlFlowGraph or viewIR #must be
			return true;
		return false;
	}

	public static boolean isApplicationMethod(CGNode f) {
		IMethod m = f.getMethod();
		ClassLoaderReference classloader_ref = m.getDeclaringClass().getClassLoader().getReference();
		if (classloader_ref.equals(ClassLoaderReference.Application))
			return true;
		return false;
	}

	public static boolean isNativeMethod(CGNode f) {
		IMethod m = f.getMethod();
		if (m.isNative())
			return true;
		return false;
	}

	public static boolean isPrimordialMethod(CGNode f) {
		IMethod m = f.getMethod();
		ClassLoaderReference classloader_ref = m.getDeclaringClass().getClassLoader().getReference();
		if (classloader_ref.equals(ClassLoaderReference.Primordial))
			return true;
		return false;
	}
	// End - Static Methods
	

	private String getJarsOrOthers() {
		String alljars = "";
		// Check a dir including *.jar or *.class #check *.jar first
		if (new File(dirpath.toString()).isDirectory()) {
			// try to get all *.jar files under the dir if could, format like
			// "jarpath1:jarpath2:jarpath3:xxx"
			try {
				alljars = findJarFiles(new String[] { dirpath.toString() });
			} catch (WalaException e) {
				e.printStackTrace();
			}
			if (!alljars.equals("")) {
				System.out.println("INFO - Test Goal - multi *.jar: " + alljars);
			}
			// ie, without *.jar under "dirpath" recursively. So
			else {
				alljars = dirpath.toString();
				System.out.println("INFO - Test Goal - multi *.class in dir: " + alljars);
			}
		}
		// Check a file like xx.jar or xx.class
		else if (dirpath.toString().endsWith(".jar")) {
			alljars = dirpath.toString();
			System.out.println("INFO - Test Goal - a x.jar file: " + alljars);
		} else if (dirpath.toString().endsWith(".class")) {
			alljars = dirpath.toString();
			System.out.println("INFO - Test Goal - a x.class file: " + alljars);
		} else {
			System.out.println("ERROR - Test Goal - others: " + alljars);
			System.exit(1);
		}
		return alljars;
	}

	public static String findJarFiles(String[] directories) throws WalaException {
		Collection<String> result = HashSetFactory.make();
		for (int i = 0; i < directories.length; i++) {
			// System.out.println(directories[i]); //JX
			for (Iterator<File> it = FileUtil.listFiles(directories[i], ".*\\.jar", true).iterator(); it.hasNext();) {
				File f = (File) it.next();
				// System.out.println(f.getAbsolutePath()); //JX
				result.add(f.getAbsolutePath());
			}
		}
		return composeString(result);
	}

	private static String composeString(Collection<String> s) {
		StringBuffer result = new StringBuffer();
		Iterator<String> it = s.iterator();
		for (int i = 0; i < s.size() - 1; i++) {
			result.append(it.next());
			result.append(File.pathSeparator);
		}
		if (it.hasNext()) {
			result.append(it.next());
		}
		return result.toString();
	}

}
