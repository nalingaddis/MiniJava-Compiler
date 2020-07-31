package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;

public class Scanner {
	InputStream input;
	char currChar;
	boolean done = false;
	StringBuilder builder;
	
	private Token next;
	
	int line = 1, position = 1;
	
	public Scanner(InputStream input) {
		this.input = input;
		
		readChar();
		next = scanToken();
	}
	
	public boolean hasNext() {return !done;}
	
	public Token next() {
		Token out = next;
		
		if(done && next.kind == TokenKind.eot) {
			next = createToken(null,TokenKind.eot);
		} else {
			next = scanToken();
		}
		
		return out;
	}
	
	private Token createToken(String value, TokenKind kind) {
		return new Token(value, kind, line, position);
	}
	
	private Token scanToken() throws SyntaxError{
		while(Character.isWhitespace(currChar)&&!done) {readChar();}
		
		if(done) {return createToken(null,TokenKind.eot);}
		
		//Detect potential comments and scan through them
		//Sneaky recursive work around
		if(currChar == '/') {
			readChar();
			if(currChar == '/') {
				while(currChar != '\n' && !done) {
					readChar();
				}
				if(!done) {readChar();}
				return scanToken();
			} else if(currChar == '*') {
				boolean eoc = false; //have we reached the end of the comment
				readChar();
				while(!eoc) {
					if(currChar == '*') {
						readChar();
						eoc = currChar == '/';
					} else {
						readChar();
					}
					if(done) {
						throw new SyntaxError();
					}
				}
				readChar();
				return scanToken();
			} else {
				return createToken("/",TokenKind.divide);
			}
		}
		
		builder = new StringBuilder();
		
		//Scanning keywords and id's
		if(Character.isLetter(currChar)) {
			builder.append(currChar);
			readChar();
			while(Character.isLetterOrDigit(currChar)||currChar=='_') {
				builder.append(currChar);
				readChar();
			}
			String value = builder.toString();
			TokenKind kind;
			switch(value) {
				case "class":
					kind = TokenKind.kw_class;
					break;
				case "void":
					kind = TokenKind.kw_void;
					break;
				case "private":
					kind = TokenKind.kw_private;
					break;
				case "public":
					kind = TokenKind.kw_public;
					break;
				case "static":
					kind = TokenKind.kw_static;
					break;
				case "int":
					kind = TokenKind.kw_int;
					break;
				case "boolean":
					kind = TokenKind.kw_boolean;
					break;
				case "this":
					kind = TokenKind.kw_this;
					break;
				case "return":
					kind = TokenKind.kw_return;
					break;
				case "if":
					kind = TokenKind.kw_if;
					break;
				case "else":
					kind = TokenKind.kw_else;
					break;
				case "while":
					kind = TokenKind.kw_while;
					break;
				case "true":
					kind = TokenKind.kw_true;
					break;
				case "false":
					kind = TokenKind.kw_false;
					break;
				case "new":
					kind = TokenKind.kw_new;
					break;
				case "null":
					kind = TokenKind.kw_null;
					break;
				default:
					kind = TokenKind.id;
					break;
			}
			return createToken(value,kind);
		}
		
		//Scanning numbers
		if(Character.isDigit(currChar)) {
			while(Character.isDigit(currChar)) {
				builder.append(currChar);
				readChar();
			}
			return createToken(builder.toString(),TokenKind.num);
		}
		
		if(currChar == '>') {
			readChar();
			if(currChar == '=') {
				readChar();
				return createToken(">=",TokenKind.gte);
			} return createToken(">",TokenKind.gt);
		}
		
		if(currChar == '<') {
			readChar();
			if(currChar == '=') {
				readChar();
				return createToken("<=",TokenKind.lte);
			} return createToken("<",TokenKind.lt);
		}
		
		if(currChar == '=') {
			readChar();
			if(currChar == '=') {
				readChar();
				return createToken("==",TokenKind.eq);
			} return createToken("=",TokenKind.ass);
		}
		
		if(currChar == '&') {
			readChar();
			if(currChar =='&') {
				readChar();
				return createToken("&&",TokenKind.and);
			}
			System.out.println("Syntax Error: Expected '&'"+"at line "+line+" position "+position);
			throw new SyntaxError();
		}
		
		if(currChar == '|') {
			readChar();
			if(currChar == '|') {
				readChar();
				return createToken("||",TokenKind.or);
			}
			System.out.println("Syntax Error: Expected '|'"+"at line "+line+" position "+position);
			throw new SyntaxError();
		}
		
		if(currChar == '!') {
			readChar();
			if(currChar == '=') {
				readChar();
				return createToken("!=",TokenKind.neq);
			} return createToken("!",TokenKind.not);
		}
		
		switch(currChar) {
			case '+': 
				readChar(); 
				return createToken("+",TokenKind.plus);
			case '-': 
				readChar();
				return createToken("-",TokenKind.minus);
			case '*':
				readChar();
				return createToken("*",TokenKind.mult);
			case '(':
				readChar();
				return createToken("(",TokenKind.o_paren);
			case ')':
				readChar();
				return createToken(")",TokenKind.c_paren);
			case '[':
				readChar();
				return createToken("[",TokenKind.o_brack);
			case ']':
				readChar();
				return createToken("]",TokenKind.c_brack);
			case '{':
				readChar();
				return createToken("{",TokenKind.o_brace);
			case '}':
				readChar();
				return createToken("}",TokenKind.c_brace);
			case ';':
				readChar();
				return createToken(";",TokenKind.eol);
			case '.':
				readChar();
				return createToken(".",TokenKind.dot);
			case ',':
				readChar();
				return createToken(",",TokenKind.comma);
			default:
				System.out.println("Syntax Error: Unrecognized character: "+currChar+" at line "+line+" position "+position);
				throw new SyntaxError();
		}
	}
	
	private void readChar() {
		try {
			int c =  input.read();
			if(c == -1) {
				done = true;
			}
			currChar = (char)c;
			
			if(currChar == '\n') {
				line++;
				position = 0;
			} else {
				position++;
			}
		} catch(IOException e) {
			System.out.println("IO Exception.");
			done = true;
		}
	}
}
