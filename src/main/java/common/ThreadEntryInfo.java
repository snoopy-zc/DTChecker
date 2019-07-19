package common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;


public class ThreadEntryInfo {

	String thread_type; //tmp worker thread - "TMP"; constant thread - "CONST"; thread pool - "POOL"
	
	CGNode invoke_cgNode;	
	SSAInstruction invoke_ssa;
	List<LoopInfo> surrounding_loops = null;                 //Type - ArrayList<LoopInfo>
	
	
	
	List<SSAInstruction> task_entries = null;
	
	
	List<LockInfo> containg_locks = null;               //Type - ArrayList<LoopInfo>
	
	
	public ThreadEntryInfo(CGNode cgNode, SSAInstruction ssa) {
		this.invoke_cgNode = cgNode;
		this.invoke_ssa = ssa;
	}
	
	
	
	@Override
	public String toString() {
	    String result = "ThreadEntryInfo{ ";
//	    for (int i = 0; i < instructions.size(); i++) {
//	      result.concat(instructions.get(i).toString() + " ");
//	    }
	    result.concat("}");
	    return result;
	}
}



