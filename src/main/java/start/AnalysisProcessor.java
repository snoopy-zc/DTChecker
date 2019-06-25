package start;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;

import util.Timer;
import wala.WalaAnalyzer;

public class AnalysisProcessor {

	Map<String,String> args;
	
	AnalysisProcessor(Map<String,String> args){
		this.args = args;
		doWork();
	}
	
	void doWork() {
		
		
		// System.out.println(arg.get("timer_out_path"));

		Timer timer = new Timer(Paths.get(this.args.get("timer_out_path")).toAbsolutePath());

		timer.tic("WalaAnalyzer begin");

		WalaAnalyzer walaAnalyzer = new WalaAnalyzer(this.args);

		timer.toc("WalaAnalyzer end\n");
		
		
		
		
		
		
		
		

		getCaller4ThreadRun(walaAnalyzer.getCallGraph());
		
		
		
		
		

		timer.close();
		
		
		
		
		
	}

	public static HashSet<CGNode> getCaller4ThreadRun(CallGraph cg) {
		// HashSet<CGNode> whoHasRun = new HashSet<CGNode>();
		// HashSet<MyPair> caller4run = new HashSet<MyPair>();
		for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext();) {
			CGNode f = it.next();
			if(f==null) {System.err.print("f==null\n");continue;}
			if (WalaAnalyzer.isApplicationMethod(f) 
					//&& !WalaAnalyzer.isNativeMethod(f)
					) {
				IR ir = f.getIR();
				if (ir==null) {System.err.print("ir==null\n");continue;}
				SSACFG cfg = ir.getControlFlowGraph();
				if (cfg==null) {System.err.print("cfg==null\n");continue;}
				for (Iterator<ISSABasicBlock> cfg_it = cfg.iterator(); cfg_it.hasNext();) {
					ISSABasicBlock bb = cfg_it.next();
					for (Iterator<SSAInstruction> bb_it = bb.iterator(); bb_it.hasNext();) {
						SSAInstruction ssaInst = bb_it.next();
						if (ssaInst instanceof SSAInvokeInstruction) {
//	      			    			if(((SSAInvokeInstruction)ssaInst).getDeclaredTarget().getDeclaringClass().getName().toString().equals("Ljava/lang/Thread") &&
							if (((SSAInvokeInstruction) ssaInst).getDeclaredTarget().getDeclaringClass().getName()
									.toString().indexOf("Thread") >= 0
									&& ((SSAInvokeInstruction) ssaInst).getDeclaredTarget().getName().toString()
											.equals("<init>")) {
								System.out.println(f);
								System.out.println(ssaInst.toString());
//	      			    				if(ssaInst.getNumberOfUses() >= 2){
//	      			    					SSAInstruction ssa =SSAUtil.getSSAByDU(f,ssaInst.getUse(1));
//	      			    					if(ssa != null && ssa instanceof SSANewInstruction){
//	      			    						String s = ((SSANewInstruction)ssa).getNewSite().getDeclaredType().getName().toString();
//	      			    						System.out.println(s);
//	      			    						MyPair tmp = new MyPair(s,ssaInst,f);
//	      			    						caller4run.add(tmp);
//	      			    					}
//	      			    				}
								// System.out.println(((SSAInvokeInstruction)ssaInst).getNumberOfParameters());
								// whoHasRun.add(f);
								// return dataSet;
							}
						}
					}
				}

			}
		}
		return null;
	}
	
}
