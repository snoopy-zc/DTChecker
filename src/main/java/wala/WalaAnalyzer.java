//package wala;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.Properties;
//
//import com.ibm.wala.classLoader.IClass;
//import com.ibm.wala.classLoader.IMethod;
//import com.ibm.wala.ipa.callgraph.AnalysisCache;
//import com.ibm.wala.ipa.callgraph.AnalysisOptions;
//import com.ibm.wala.ipa.callgraph.AnalysisScope;
//import com.ibm.wala.ipa.callgraph.CGNode;
//import com.ibm.wala.ipa.callgraph.CallGraph;
//import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
//import com.ibm.wala.ipa.callgraph.CallGraphStats;
//import com.ibm.wala.ipa.callgraph.Entrypoint;
//import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
//import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
//import com.ibm.wala.ipa.callgraph.impl.Util;
//import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
//import com.ibm.wala.ipa.cha.ClassHierarchy;
//import com.ibm.wala.ipa.cha.IClassHierarchy;
//import com.ibm.wala.properties.WalaProperties;
//import com.ibm.wala.ssa.IR;
//import com.ibm.wala.ssa.ISSABasicBlock;
//import com.ibm.wala.ssa.SSACFG;
//import com.ibm.wala.ssa.SSAInstruction;
//import com.ibm.wala.ssa.SSAInvokeInstruction;
//import com.ibm.wala.ssa.SymbolTable;
//import com.ibm.wala.types.ClassLoaderReference;
//import com.ibm.wala.types.MethodReference;
//import com.ibm.wala.types.TypeReference;
//import com.ibm.wala.util.WalaException;
//import com.ibm.wala.util.collections.CollectionFilter;
//import com.ibm.wala.util.collections.HashSetFactory;
//import com.ibm.wala.util.config.AnalysisScopeReader;
//import com.ibm.wala.util.debug.Assertions;
//import com.ibm.wala.util.graph.Graph;
//import com.ibm.wala.util.graph.GraphIntegrity;
//import com.ibm.wala.util.graph.GraphSlicer;
//import com.ibm.wala.util.graph.InferGraphRoots;
//import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
//import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
//import com.ibm.wala.util.io.FileProvider;
//import com.ibm.wala.util.strings.StringStuff;
//import com.ibm.wala.viz.DotUtil;
//import com.ibm.wala.viz.PDFViewUtil;
//
//
//
//public class WalaAnalyzer {   
//
//	Path dirpath;            // Target dir. Should also support single *.jar file, But for now NOT NOT NOT 
//  
//    // WALA basis
//    AnalysisScope scope;
//    ClassHierarchy cha;
//    HashSet<Entrypoint> entrypoints;
//    CallGraph cg;
//    public List<String> packageScopePrefixes = new ArrayList<String>();  //read from 'package-scope.txt' if exists
//    
//    // Statistics
//    int nPackageFuncs = 0;       // the real functions we focuses      //must satisfy "isApplicationAndNonNativeMethod" first
//    int nTotalFuncs = 0;
//    int nApplicationFuncs = 0;    
//    int nPremordialFuncs = 0;
//    int nOtherFuncs = 0;
//    
//    // Others
//    private final static boolean CHECK_GRAPH = false;
//    final public static String CG_PDF_FILE = "cg.pdf";
//  
//
//    // Configuration For tests
//    // for all
//    String functionname_for_test = "doWork0("; //"org.apache.hadoop.hdfs.server.datanode.FSDataset$FSDir.getBlockInfo("; //"RetryCache.waitForCompletion(Lorg/apache/hadoop/ipc/RetryCache$CacheEntry;)"; //"org.apache.hadoop.hdfs.server.balancer.Balancer"; //"Balancer$Source.getBlockList";//"DirectoryScanner.scan"; //"ReadaheadPool.getInstance("; //"BPServiceActor.run("; //"DataNode.runDatanodeDaemon"; //"BPServiceActor.run("; //"BlockPoolManager.startAll"; //"NameNodeRpcServer"; //"BackupNode$BackupNodeRpcServer"; // //".DatanodeProtocolServerSideTranslatorPB"; //"DatanodeProtocolService$BlockingInterface"; //"sendHeartbeat("; //"org.apache.hadoop.hdfs.protocolPB.DatanodeProtocolServerSideTranslatorPB";  //java.util.regex.Matcher.match(";
//    int which_functionname_for_test = 1;   //1st? 2nd? 3rd?    //TODO - 0 means ALL, 1 to n means which one respectively
//  
//    /**
//     * Input - 1. a dir path including *.jar or *.class, 
//     * 		   2. 
//     * 		   3. 
//     */
//    public WalaAnalyzer(String dirstr) {
//    	this( Paths.get(dirstr) );
//    }
//    
//    public WalaAnalyzer(Path dirpath) {
//    	if ( !Files.exists(dirpath) ) {
//    		System.out.println("JX - ERROR - " + "!Files.exists @ " + dirpath + " for Wala");
//    		return;
//    	}
//    	this.dirpath = dirpath;
//    	doWork();
//    }
//    
//    public Path getTargetDirPath() {
//    	return this.dirpath;
//    }
//    
//    public CallGraph getCallGraph() {
//    	return this.cg;
//    }
//    
//    public ClassHierarchy getClassHierarchy() {
//    	return this.cha;
//    }
//    
//    public int getNPackageFuncs() {
//    	return nPackageFuncs;
//    }
//    
//    public int getNTotalFuncs() {
//    	return nTotalFuncs;
//    }
//    
//    public int getNApplicationFuncs() {
//    	return nApplicationFuncs;
//    }
//    
//    public int getNPremordialFuncs() {
//    	return nPremordialFuncs;
//    }
//    
//    public int getNOtherFuncs() {
//    	return nOtherFuncs;
//    }
//    
// 
//    
//    private void doWork() {
//    	System.out.println("JX - INFO - WalaAnalyzer: doWork...");
//    
//    	try {
//    		walaAnalysis(getJarsOrOthers());
//    		infoWalaAnalysisEnv();
//    		readPackageScope();
//    		//testIClass();
//    		//testTypeHierarchy();
//    		//testCGNode();
//    		//testPartialCallGraph();
//    		//testIR();         		 //JX - need to configurate Dot and PDFViewer
//    		//testWalaAPI();
//    	} catch (IllegalArgumentException | CallGraphBuilderCancelException | IOException | UnsoundGraphException
//			| WalaException e) {
//    		e.printStackTrace();
//    	}
//    }
//
//    
//    
//    
//    /********************************************************************************
//     * JX - Functions Region
//     *******************************************************************************/
//    
//    private String getJarsOrOthers() {
//    	String alljars = "";   
//    	// Check a dir including *.jar or *.class     #check *.jar first
//    	if (new File(dirpath.toString()).isDirectory()) {
//    		//try to get all *.jar files under the dir if could, format like "jarpath1:jarpath2:jarpath3:xxx"
//	    	try {
//				alljars = PDFCallGraph.findJarFiles( new String[]{dirpath.toString()} );
//			} catch (WalaException e) {
//				e.printStackTrace();
//			} 
//	    	if ( !alljars.equals("") ) {
//	    		System.out.println("JX - INFO - Test Goal - multi *.jar: " + alljars);
//	    	}
//	    	// ie, without *.jar under "dirpath" recursively. So
//	    	else {
//	    		alljars = dirpath.toString();
//	    		System.out.println("JX - INFO - Test Goal - multi *.class in dir: " + alljars);
//	    	}
//	    } 
//    	// Check a file like xx.jar or xx.class
//	    else if (dirpath.toString().endsWith(".jar")) {
//	    	alljars = dirpath.toString();
//	    	System.out.println("JX - INFO - Test Goal - a x.jar file: " + alljars);
//	    }	
//	    else if (dirpath.toString().endsWith(".class")) {
//	    	alljars = dirpath.toString();
//	    	System.out.println("JX - INFO - Test Goal - a x.class file: " + alljars);
//	    }
//	    else {
//	    	System.out.println("JX - ERROR - Test Goal - others: " + alljars);
//	    	System.exit(1);
//	    }
//	    return alljars;
//    }
//    
// 
//    /**
//     * @param alljars - absolute path or relative path, can be one of the following
//     * 					1. multiple .jar files separated by ":"(linux) or ";"(win), "src/sa/res/ca-6744/xx.jar;xxx/xx.jar"
//     * 					2. a class-file dir, can be any-level dir, like "bin/sa", "bin/sa/test", "bin/sa/test/testsrc", will identify all *.class recursively and overlook *.jar
//     * 					3. single .jar file, "src/sa/res/ca-6744/xx.jar"
//     * 					4. single .class file
//     */
//    private void walaAnalysis(String alljars) throws IOException, IllegalArgumentException, CallGraphBuilderCancelException, UnsoundGraphException, WalaException {
//		System.out.println("JX - INFO - WalaAnalyzer: walaAnalysis...");
//
//	    // Create a Scope                                                                           #"JXJavaRegressionExclusions.txt"
//	    scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(alljars, (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS)); //default: CallGraphTestUtil.REGRESSION_EXCLUSIONS
//	    // Create a Class Hierarchy
//	    cha = ClassHierarchy.make(scope);  
//	    //testTypeHierarchy();
//	    
//	    // Create a Entry Points
//	    entrypoints = new HashSet<Entrypoint>();
//	    Iterable<Entrypoint> allappentrypoints = new AllApplicationEntrypoints(scope, cha);  //Usually: entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);  //get main entrypoints
//	    // Get all entry points
//	    entrypoints = (HashSet<Entrypoint>) allappentrypoints;
//	    // TODO - can narrow entrypoints according to "scope.txt"
//	    
//	    // Create Analysis Options
//	    AnalysisOptions options = new AnalysisOptions(scope, entrypoints); 
//	    options.setReflectionOptions(ReflectionOptions.ONE_FLOW_TO_CASTS_NO_METHOD_INVOKE);   //ReflectionOptions.FULL will just cause a few more nodes and methods 
//
//	    // Create a builder - default: Context-insensitive   
//	    //#makeZeroCFABuilder(options, new AnalysisCache(), cha, scope, null, null); 
//	    //#makeVanillaZeroOneCFABuilder(options, new AnalysisCache(), cha, scope, null, null);   // this will take 20+ mins to finish
//	    //#makeZeroOneCFABuilder(options, new AnalysisCache(), cha, scope, null, null);
//	    //#makeZeroOneContainerCFABuilder(options, new AnalysisCache(), cha, scope, null, null);
//	    com.ibm.wala.ipa.callgraph.CallGraphBuilder builder = Util.makeZeroCFABuilder(options, new AnalysisCache(), cha, scope, null, null); 
//	    // Context-sensitive
//	    /*
//	    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha); 
//	    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha); 
//	    //ContextSelector contextSelector = new DefaultContextSelector(options);    
//	    //SSAContextInterpreter contextInterpreter = new DefaultSSAInterpreter(options, Cache);
//	    SSAPropagationCallGraphBuilder builder = new nCFABuilder(1, cha, options, new AnalysisCache(), null, null); 
//	    AllocationSiteInNodeFactory factory = new AllocationSiteInNodeFactory(options, cha);
//	    builder.setInstanceKeys(factory);
//	    */
//	    
//	    // Build the call graph JX: time-consuming
//	    cg = builder.makeCallGraph(options, null);
//	    System.out.println(CallGraphStats.getStats(cg));
//	    
//	    // Get pointer analysis results
//	    /*
//	    PointerAnalysis pa = builder.getPointerAnalysis();
//	    HeapModel hm = pa.getHeapModel();   //JX: #getHeapModel's reslult is com.ibm .wala.ipa.callgraph.propagation.PointerAnalysisImpl$HModel@24ccf6a8
//	    BasicHeapGraph hg = new BasicHeapGraph(pa, cg);
//	    System.err.println(hg);
//	    */
//	    //System.err.println(builder.getPointerAnalysis().getHeapGraph());  
//
//	    if (CHECK_GRAPH) {
//	      GraphIntegrity.check(cg);
//	    }
//	}
//	
//    
//    private void infoWalaAnalysisEnv() {
//    	System.out.println("JX - INFO - WalaAnalyzer: infoWalaAnalysisEnv");
//      
//    	int nAppNatives = 0;
//    	int nPriNatives = 0;
//    	int nOthNatives = 0;
//      
//    	for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext(); ) {
//    		CGNode f = it.next();
//	      	nTotalFuncs ++;
//	      	if ( isApplicationMethod(f) ) {
//	      		nApplicationFuncs ++;
//	      		if ( isNativeMethod(f) ) nAppNatives ++;
//	      	}
//	      	else if ( isPrimordialMethod(f) ) {
//	      		nPremordialFuncs ++;
//	      		if ( isNativeMethod(f) ) nPriNatives ++;
//	      	}
//	      	else {
//	      		nOtherFuncs++;
//	      		if ( isNativeMethod(f) ) nOthNatives ++;
//	      	}
//	      
//	      	if ( isInPackageScope(f) ) {
//	      		nPackageFuncs ++;
//	      	}
//    	}
//      
//    	System.out.println( "nTotalFuncs(" + nTotalFuncs 
//    		  + ") = nApplicationFuncs(" + nApplicationFuncs + ") + nPremordialFuncs(" + nPremordialFuncs + ") + nOtherFuncs(" + nOtherFuncs + ")" );
//    	System.out.println( "\t" + "nApplicationFuncs(" + nApplicationFuncs + ") includes " + nAppNatives + " native methods" );
//    	System.out.println( "\t" + "nPremordialFuncs(" + nPremordialFuncs + ") includes " + nPriNatives + " native methods" );
//    	System.out.println( "\t" + "nOtherFuncs(" + nOtherFuncs + ") includes " + nOthNatives + " native methods" );
//    	System.out.println( "nPackageFuncs(" + nPackageFuncs + ") - Note: this should be isApplicationAndNonNativeMethod first" );
//    }
//  
//    
//    private void readPackageScope() {
//    	System.out.println("JX - INFO - WalaAnalyzer: readPackageScope");
//    	String filepath = Paths.get(dirpath.toString(), "package-scope.txt").toString();
//
//    	File f = new File( filepath );
//	  
//    	if ( !f.exists() ) {
//    		System.out.println("NOTICE - not find the 'package-scope.txt' file, so SCOPE is ALL methods!!");
//    		return;
//    	}
//
//    	TextFileReader reader;
//    	String tmpline;
//		try {
//			reader = new TextFileReader(filepath);
//			while ( (tmpline = reader.readLine()) != null ) {
//				String[] strs = tmpline.split("\\s+");
//				packageScopePrefixes.add( strs[0] );
//			}
//			reader.close();
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			System.out.println("JX - ERROR - when reading package-scpoe.txt files");
//			e.printStackTrace();
//		}
//		System.out.print("NOTICE - successfully read the 'package-scope.txt' file as SCOPE, including:");
//		for (String str: packageScopePrefixes)
//			System.out.print( " " + str );
//		System.out.println();
//	
//    }
//  
//    
//    // must satisfy "isApplicationAndNonNativeMethod" first
//    public boolean isInPackageScope(CGNode cgNode) {
//    	// added 
//    	if ( !isApplicationAndNonNativeMethod(cgNode) )
//    		return false;
//    	// if without 'package-scope.txt'
//    	if (packageScopePrefixes.size() == 0)
//    		return true;
//    	String signature = cgNode.getMethod().getSignature();
//    	for (String str: packageScopePrefixes)
//    		if (signature.startsWith(str))
//    			return true;
//    	return false;
//    }
//    	  
//	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//	// Static Methods - Begin
//	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    public static boolean isApplicationAndNonNativeMethod(CGNode f) {
//  	  	if ( isApplicationMethod(f) && !isNativeMethod(f) )  //IMPO:  some native methods are App class, but can't IR#getControlFlowGraph or viewIR     #must be
//  	  		return true;
//  	  	return false;
//    }
//	    
//    public static boolean isApplicationMethod(CGNode f) {
//  	  	IMethod m = f.getMethod();
//  	  	ClassLoaderReference classloader_ref = m.getDeclaringClass().getClassLoader().getReference();
//  	  	if ( classloader_ref.equals(ClassLoaderReference.Application) )
//  	  		return true;
//  	  	return false;
//    }
//    
//    public static boolean isNativeMethod(CGNode f) {
//  	  	IMethod m = f.getMethod();
//  	  	if ( m.isNative() )
//  	  		return true;
//  	  	return false;
//    }
//
//    public static boolean isPrimordialMethod(CGNode f) {
//  	  	IMethod m = f.getMethod();
//  	  	ClassLoaderReference classloader_ref = m.getDeclaringClass().getClassLoader().getReference();
//  	  	if ( classloader_ref.equals(ClassLoaderReference.Primordial) )
//  	  		return true;
//  	  	return false;
//    }
//    // End - Static Methods 
//    
//}
//
//
//
////===============================================================================================
////++++++++++++++++++++++++++++++++++ External Classes +++++++++++++++++++++++++++++++++++++++++++
////===============================================================================================
//
//
//class ApplicationLoaderFilter extends Predicate<CGNode> {
//@Override public boolean test(CGNode o) {
//  //return true;   //by JX
//  if (o instanceof CGNode) {
//    CGNode n = (CGNode) o;
//    return n.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application);
//  }
//  else if (o instanceof LocalPointerKey) {
//    LocalPointerKey l = (LocalPointerKey) o;
//    return test(l.getNode());
//  } 
//  else {
//    return false;
//  }
//}
//}
//
//
//
