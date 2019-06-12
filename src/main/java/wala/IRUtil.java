package wala;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.viz.PDFViewUtil;

import util.Config;

import com.ibm.wala.ssa.SSAInstruction;

public class IRUtil {

	public static BasicBlock getBasicBlock(CGNode cgNode, int bbNum) {
		IR ir = cgNode.getIR();
		SSACFG cfg = ir.getControlFlowGraph();
		return cfg.getBasicBlock(bbNum);
	}

	public static int getSSAIndex(CGNode cgNode, SSAInstruction ssa) {
		IR ir = cgNode.getIR();
		SSAInstruction[] ssas = ir.getInstructions();
		int index = -1;
		for (int i = 0; i < ssas.length; i++)
			if (ssas[i] != null)
				if (ssas[i].equals(ssa)) {
					index = i;
					break;
				}
		return index;
	}

	public static int getSourceLineNumberFromBB(CGNode cgNode, ISSABasicBlock bb) {
		for (Iterator<SSAInstruction> it = bb.iterator(); it.hasNext();) {
			SSAInstruction ssa = it.next();
			int linenum = getSourceLineNumberFromSSA(cgNode, ssa);
			if (linenum != -1)
				return linenum;
		}
		return -1;
	}

	public static int getSourceLineNumberFromSSA(CGNode cgNode, SSAInstruction ssa) {
		IR ir = cgNode.getIR();
		IBytecodeMethod bytecodemethod = (IBytecodeMethod) ir.getMethod();

		int index = getSSAIndex(cgNode, ssa);
		if (index != -1) {
			try {
				int bytecodeindex = bytecodemethod.getBytecodeIndex(index);
				int sourcelinenum = bytecodemethod.getLineNumber(bytecodeindex);
				if (sourcelinenum != -1)
					return sourcelinenum;
			} catch (InvalidClassFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return -1;
	}
	
	  
	@SuppressWarnings("unchecked")
	public static void viewIR(IR ir) throws WalaException {
	    /**
	     * JX: it seems "viewIR" is not suitable for some functions like "LeaseManager.checkLeases", 
	     * its Exception: failed to find <Application,Lorg/apache/hadoop/fs/UnresolvedLinkException>
	     */
		
		Map<String, String> tools = (Map<String, String>) Config.getTools();
		
	    IClassHierarchy cha = ir.getMethod().getClassHierarchy();
	    String dotExe = tools.get("dotExe");
	    String pdfExe = tools.get("pdfExe");
		String dotFile = tools.get("dotFile");
		String pdfFile = tools.get("pdfFile");
	    
		if ( !Files.exists( Paths.get(dotExe)) ) 
	    	System.out.println("ZC - ERROR - the software location of 'dot' is wrong");
		if ( !Files.exists( Paths.get(pdfExe)) )
	    	System.out.println("ZC - ERROR - the software location of 'pdfviewer' is wrong");
	   
	    // Generate IR ControlFlowGraph's SWT viewer
	    //SSACFG cfg = ir.getControlFlowGraph();
	    
		// Generate IR PDF viewer
	    PDFViewUtil.ghostviewIR(cha, ir, pdfFile, dotFile, dotExe, pdfExe); //that is, psFile, dotFile, dotExe, gvExe, originally
	}

}