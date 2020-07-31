package miniJava.SyntacticAnalyzer;

public class Token {
	public String spelling;
	public TokenKind kind;
	public SourcePosition posn;
	
	int line, position;
	
	public Token(String spelling, TokenKind kind, int line, int position) {
		this.spelling = spelling;
		this.kind = kind;
		this.line = line;
		this.position = position;
	}
}
