package common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.Pair;

import wala.IRUtil;

public class LoopInfo {
	// where
	CGNode cgNode;
	int line_number;
	// its Basic Block info
	Set<Integer> bbs;
	int begin_bb; // bb - basic block in WALA
	int end_bb;

	// included time-consuming operation info
	public int numOfTcOperations_recusively;
	public List<SSAInstruction> tcOperations_recusively;
	public List<TcOperationInfo> tcOperations_recusively_info;

	// nested loop info
	public int max_depthOfLoops; // ie, nested loops inside the loop
	public List<Integer> function_chain_for_max_depthOfLoops;
	public List<Integer> hasLoops_in_current_function_for_max_depthOfLoops;

	String var_name;

	// bound related info
	public boolean whileTrue = true; // maybe false negative when break-block in the final basic block
	public Set<Integer> conditional_branch_block; // branch block
	// mediate variables
	Set<Pair<CGNode, Integer>> V_nul = new HashSet<Pair<CGNode, Integer>>();
	HashMap<Pair<CGNode, Integer>, Collection<Statement>> V_nul_ht = new HashMap<Pair<CGNode, Integer>, Collection<Statement>>(); //the same to val_nul but store the statement

	Set<Pair<CGNode, Integer>> V_def = new HashSet<Pair<CGNode, Integer>>();
	Set<Pair<CGNode, SSAInstruction>> fieldCollection = new HashSet<Pair<CGNode, SSAInstruction>>();
	Set<Statement> IOs = new HashSet<Statement>();// unanalysis or I/O

	// conclusion
	public boolean bounded = false;

	/*
	 * can be referred public LinkedList<String> info = new LinkedList<String>();
	 * public HashSet<String> relatedConf = new HashSet<String>(); public
	 * HashSet<String> constVar = new HashSet<String>(); public boolean
	 * isLibraryInvovled = false; public HashSet<MyTriple> newlyCGN = new
	 * HashSet<MyTriple>(); boolean notFindCaller = false; public HashSet<CGNode>
	 * opt = new HashSet<CGNode>(); public LabelledSSA retSource = null; public
	 * boolean hasRetSource = false; public boolean collectionBounded; public
	 * boolean collectionIsBounded; public boolean specailBounded; public boolean
	 * strongBounded; public String level = "-1";
	 */

	public LoopInfo(CGNode cgNode, int begin_bb, int end_bb) {
		this.cgNode = cgNode;
		this.begin_bb = begin_bb;
		this.end_bb = end_bb;
		// added by computation
		this.line_number = IRUtil.getSourceLineNumberFromBB(cgNode, IRUtil.getBasicBlock(cgNode, begin_bb));
		// init
		this.bbs = new TreeSet<Integer>();

		this.numOfTcOperations_recusively = 0; // jx: yes
		this.tcOperations_recusively = new ArrayList<SSAInstruction>();
		this.tcOperations_recusively_info = new ArrayList<TcOperationInfo>();

		this.max_depthOfLoops = 0;
		this.function_chain_for_max_depthOfLoops = new ArrayList<Integer>();
		this.hasLoops_in_current_function_for_max_depthOfLoops = new ArrayList<Integer>();

		// more init
		computeBasicBlocks();

		// add to analysis bound
		this.conditional_branch_block = new TreeSet<Integer>();
	}

	// APIs
	public CGNode getCGNode() {
		return this.cgNode;
	}

	public int getLineNumber() {
		return this.line_number;
	}

	public Set<Integer> getBasicBlockNumbers() {
		return this.bbs;
	}

	public int getBeginBasicBlockNumber() {
		return this.begin_bb;
	}

	public int getEndBasicBlockNumber() {
		return this.end_bb;
	}

	public SSAInstruction getConditionSSA() { // TODO only return one instruction, but maybe multiple ???
		for (Integer bindex : this.conditional_branch_block) {
			BasicBlock bb = this.cgNode.getIR().getControlFlowGraph().getBasicBlock(bindex);
			for (SSAInstruction ssa : bb.getAllInstructions()) {
				if (ssa instanceof SSAConditionalBranchInstruction) {
					return ssa;
				}
			}
		}
		return null;
	}

	public BasicBlock getBeginBasicBlock() {
		return this.cgNode.getIR().getControlFlowGraph().getBasicBlock(this.begin_bb);
	}

	public BasicBlock getEndBasicBlock() {
		return this.cgNode.getIR().getControlFlowGraph().getBasicBlock(this.end_bb);
	}

	public void setEndBasicBlock(int end_bb) {
		this.end_bb = end_bb;
		computeBasicBlocks();
	}

	public boolean isContain(SSAInstruction o_ssa) {

		CGNode cgNode = this.getCGNode();
		IR ir = cgNode.getIR();
		SSACFG cfg = ir.getControlFlowGraph();
		SSAInstruction[] instructions = ir.getInstructions();

		for (int bbnum : this.getBasicBlockNumbers()) {
			int first_index = cfg.getBasicBlock(bbnum).getFirstInstructionIndex();
			int last_index = cfg.getBasicBlock(bbnum).getLastInstructionIndex();
			for (int i = first_index; i <= last_index; i++) {
				SSAInstruction ssa = instructions[i];
				if (ssa == null)
					continue;
				if (ssa.equals(o_ssa))
					return true;
			}
		}
		return false;
	}

	// newly added, haven't been tested
	public boolean containsSSA(SSAInstruction targetSSA) {
		IR ir = cgNode.getIR();
		SSACFG cfg = ir.getControlFlowGraph();

		for (int bbnum : bbs) {
			ISSABasicBlock bb = cfg.getNode(bbnum);
			for (Iterator<SSAInstruction> it = bb.iterator(); it.hasNext();) {
				SSAInstruction ssa = it.next();
				if (ssa.equals(targetSSA))
					return true;
			}
		}
		return false;
	}

	public void computeBasicBlocks() {
		IR ir = cgNode.getIR();
		SSACFG cfg = ir.getControlFlowGraph();
		// Get the basic block set for this loop
		Set<Integer> succbbs = new HashSet<Integer>(); // used as a temporary variable
		Set<Integer> predbbs = new HashSet<Integer>(); // used as a temporary variable

		succbbs.add(begin_bb);
		dfsFromEnter(cfg.getNode(begin_bb), cfg, succbbs);
		predbbs.add(end_bb);
		dfsFromExit(cfg.getNode(end_bb), cfg, predbbs);
		mergeLoop(succbbs, predbbs);
	}

	private void dfsFromEnter(ISSABasicBlock bb, SSACFG cfg, Set<Integer> succbbs) {
		for (Iterator<ISSABasicBlock> it = cfg.getSuccNodes(bb); it.hasNext();) {
			ISSABasicBlock succ = it.next();
			int succnum = succ.getNumber();
			if (!succbbs.contains(succnum)) {
				succbbs.add(succnum);
				dfsFromEnter(succ, cfg, succbbs);
			}
		}
	}

	private void dfsFromExit(ISSABasicBlock bb, SSACFG cfg, Set<Integer> predbbs) {
		if (bb.equals(cfg.getNode(this.begin_bb)))
			return;
		for (Iterator<ISSABasicBlock> it = cfg.getPredNodes(bb); it.hasNext();) {
			ISSABasicBlock pred = it.next();
			int prednum = pred.getNumber();
			if (!predbbs.contains(prednum)) {
				predbbs.add(prednum);
				dfsFromExit(pred, cfg, predbbs);
			}
		}
	}

	private void mergeLoop(Set<Integer> succbbs, Set<Integer> predbbs) {
		predbbs.retainAll(succbbs);
		this.bbs.addAll(predbbs);
	}

	@Override
	public String toString() {
		return "LOOP - " + cgNode.getMethod().getSignature() + ":" + line_number + ", " + "Time-consumingOps("
				+ numOfTcOperations_recusively + "):" + tcOperations_recusively + ", " + "{begin:" + begin_bb + " end:"
				+ end_bb + " var_name:" + var_name + " bbs:" + bbs + "}";
	}

	public String toString_get() {
		return "LOOP - " + cgNode.getMethod().getSignature() + ":" + line_number + ", \n" + "Time-consumingOps("
				+ numOfTcOperations_recusively + "):" + tcOperations_recusively;
	}

	// tmp for loop's Time-consuming operations
	public String toString_detail() {
		return "LOOP - " + cgNode.getMethod().getSignature() + ":" + line_number + ", " + "Time-consumingOps("
				+ numOfTcOperations_recusively + "):" + tcOperations_recusively_info;
	}

}

/*
 * public LoopInfo() { this.cgNode = null; this.line_number = 0;
 * 
 * this.bbs = new TreeSet<Integer>(); this.succbbs = new HashSet<Integer>();
 * this.predbbs = new HashSet<Integer>();
 * 
 * this.numOfTcOperations_recusively = 0; //jx: yes this.tcOperations_recusively
 * = new ArrayList<SSAInstruction>(); this.tcOperations_recusively_info = new
 * ArrayList<TcOperationInfo>();
 * 
 * this.max_depthOfLoops = 0; this.function_chain_for_max_depthOfLoops = new
 * ArrayList<Integer>(); this.hasLoops_in_current_function_for_max_depthOfLoops
 * = new ArrayList<Integer>(); }
 */
