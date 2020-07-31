package miniJava.ContextualAnalysis;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class IdentificationException extends Exception{
	private static final long serialVersionUID = 1L;	
	
	SourcePosition posn;
	String msg;
	
	public IdentificationException(SourcePosition posn, String msg) {
		this.posn = posn;
		this.msg = msg;
	}
}
