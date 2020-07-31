package miniJava.SyntacticAnalyzer;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.AssignStmt;
import miniJava.AbstractSyntaxTrees.BaseType;
import miniJava.AbstractSyntaxTrees.BinaryExpr;
import miniJava.AbstractSyntaxTrees.BlockStmt;
import miniJava.AbstractSyntaxTrees.BooleanLiteral;
import miniJava.AbstractSyntaxTrees.CallExpr;
import miniJava.AbstractSyntaxTrees.CallStmt;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassDeclList;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.ExprList;
import miniJava.AbstractSyntaxTrees.Expression;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.FieldDeclList;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.IxAssignStmt;
import miniJava.AbstractSyntaxTrees.IxExpr;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.MethodDeclList;
import miniJava.AbstractSyntaxTrees.NewArrayExpr;
import miniJava.AbstractSyntaxTrees.NewObjectExpr;
import miniJava.AbstractSyntaxTrees.NullLiteral;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.ParameterDeclList;
import miniJava.AbstractSyntaxTrees.QualRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.Reference;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeDenoter;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.WhileStmt;

public class Parser {
	
	Scanner scanner;
	Token token;
	AST ast;
	
	public Parser(Scanner scanner) {
		this.scanner = scanner;
	}
	
	private void accept(TokenKind kind) {
		if(token.kind != kind) {
			System.out.println("Error: Expected \""+kind+"\" but got \""+token.kind+"\" at line "+token.line+" position "+token.position+".");
			throw new SyntaxError();
		}
		//System.out.println("Accpeted: "+kind+" (\""+token.spelling+"\")");
		token = scanner.next();
	}
	
	private void acceptIt() {accept(token.kind);}
	
	private SourcePosition getCurrPosn() {
		return new SourcePosition(token.line);
	}
	
	public AST parse() {
		try {token = scanner.next();}
		catch(SyntaxError e) {return null;}
		
		ClassDeclList classList = new ClassDeclList();
		SourcePosition posn = getCurrPosn();
		
		while(token.kind != TokenKind.eot) {
			try {
				classList.add(parseClass());
			} catch(SyntaxError e) {
				return null;
			}
		} acceptIt();
		
		ast = new Package(classList,posn);
		
		return ast;
	}
	
	private ClassDecl parseClass() throws SyntaxError{
		String cn;
		FieldDeclList fields = new FieldDeclList();
		MethodDeclList methods = new MethodDeclList();
		MethodDecl constructor = null;
		SourcePosition classPosn;
		
		accept(TokenKind.kw_class);
		cn = token.spelling;
		classPosn = getCurrPosn();
		accept(TokenKind.id);
		accept(TokenKind.o_brace);
		
		boolean isPrivate, isStatic;
		TypeDenoter type;
		String name;
		SourcePosition memberPosn;
		
		while(token.kind != TokenKind.c_brace) {
			isPrivate = parseVisibility();
			isStatic = parseAccess();
			
			if(token.kind == TokenKind.kw_void) {
				SourcePosition voidPosn = getCurrPosn();
				acceptIt();
				
				type = new BaseType(TypeKind.VOID,voidPosn);
				name = token.spelling;
				memberPosn = getCurrPosn();
				
				accept(TokenKind.id);
				
				methods.add(parseMethod(new FieldDecl(isPrivate,isStatic,type,name,memberPosn)));
			} else {
				type = parseType();
				memberPosn = getCurrPosn();
				
				if(token.kind == TokenKind.o_paren 
						&& type instanceof ClassType 
						&& ((ClassType)type).className.spelling.equals(cn)) {
					type = new BaseType(TypeKind.VOID,getCurrPosn());
					if(constructor!=null) {
						System.out.println("Only one constructor declaration per class");
						throw new SyntaxError();
					}
					constructor = parseMethod(new FieldDecl(isPrivate,isStatic,type,"_constructor",memberPosn));
					methods.add(constructor);
				} else {
					name = token.spelling;					
					accept(TokenKind.id);
					
					if(token.kind == TokenKind.eol) {
						fields.add(new FieldDecl(isPrivate,isStatic,type,name,memberPosn));
						acceptIt();
					} else if(token.kind == TokenKind.ass && isStatic) {
						acceptIt();
						fields.add(new FieldDecl(isPrivate,isStatic,type,name,parseExpression(),memberPosn));
						acceptIt();
					} else {
						methods.add(parseMethod(new FieldDecl(isPrivate,isStatic,type,name,memberPosn)));
					}
				}
			}
		}
		acceptIt();
		
		return new ClassDecl(cn,fields,methods,constructor,classPosn);
	}
	
	private boolean parseVisibility() {
		if(token.kind == TokenKind.kw_public) {acceptIt(); return false;}
		if(token.kind == TokenKind.kw_private) {acceptIt(); return true;}
		return false;
	}
	
	private boolean parseAccess() {
		if(token.kind == TokenKind.kw_static) {acceptIt(); return true;}
		return false;
	}
	
	private TypeDenoter parseType() {
		TypeDenoter base;
		SourcePosition typePosn = getCurrPosn();
		
		if(token.kind == TokenKind.kw_int) {
			acceptIt();
			base = new BaseType(TypeKind.INT, typePosn);
			if(token.kind == TokenKind.o_brack) {
				acceptIt();
				accept(TokenKind.c_brack);
				return new ArrayType(base,typePosn);
			}
		} else if(token.kind == TokenKind.id) {
			base = new ClassType(new Identifier(token),typePosn);
			acceptIt();
			if(token.kind == TokenKind.o_brack) {
				acceptIt();
				accept(TokenKind.c_brack);
				return new ArrayType(base,typePosn);
			}
		} else if(token.kind == TokenKind.kw_boolean) {
			acceptIt();
			base = new BaseType(TypeKind.BOOLEAN,typePosn);
		} else {
			throw new SyntaxError();
		}
		
		return base;
	}
	
	private MethodDecl parseMethod(MemberDecl md) {
		ParameterDeclList params;
		StatementList statements = new StatementList();
		SourcePosition posn = getCurrPosn();
		
		accept(TokenKind.o_paren);
		params = parseParameterList();
		accept(TokenKind.c_paren);
		
		accept(TokenKind.o_brace);
		while(token.kind != TokenKind.c_brace) {
			statements.add(parseStatement());
		}
		acceptIt();
		
		return new MethodDecl(md,params,statements,posn);
	}
	
	private ParameterDeclList parseParameterList() {
		ParameterDeclList params = new ParameterDeclList();
		
		if(token.kind == TokenKind.c_paren) {return params;}
		
		TypeDenoter type;
		String name;
		
		type = parseType();
		name = token.spelling;
		params.add(new ParameterDecl(type,name,getCurrPosn()));
		accept(TokenKind.id);
		
		while(token.kind == TokenKind.comma) {
			acceptIt();
			
			type = parseType();
			name = token.spelling;
			params.add(new ParameterDecl(type,name,getCurrPosn()));
			accept(TokenKind.id);
		}
		
		return params;
	}
	
	private Statement parseStatement() {
		SourcePosition stmtPosn = getCurrPosn();
		
		//another braced statement
		if(token.kind == TokenKind.o_brace) {
			acceptIt();
			StatementList statements = new StatementList();
			while(token.kind != TokenKind.c_brace) {
				statements.add(parseStatement());
			} acceptIt();
			return new BlockStmt(statements,stmtPosn);
		//return statement
		} else if(token.kind == TokenKind.kw_return) {
			acceptIt();
			Expression exp = null;
			if(token.kind != TokenKind.eol) {
				exp = parseExpression();
			} accept(TokenKind.eol);
			return new ReturnStmt(exp,stmtPosn);
		//if statement
		} else if(token.kind == TokenKind.kw_if) {
			acceptIt();
			
			accept(TokenKind.o_paren);
			Expression exp = parseExpression();
			accept(TokenKind.c_paren);
			
			Statement thenStmt = parseStatement(), elseStmt = null;
			
			if(token.kind == TokenKind.kw_else) {
				acceptIt();
				elseStmt = parseStatement();
			}
			return new IfStmt(exp, thenStmt, elseStmt, stmtPosn);
		//while statement
		} else if(token.kind == TokenKind.kw_while) {
			acceptIt();
			accept(TokenKind.o_paren);
			Expression exp = parseExpression();
			accept(TokenKind.c_paren);
			
			Statement stmt = parseStatement();
			return new WhileStmt(exp,stmt,stmtPosn);
			
		//the sticky situation (if we see an id)
		} else if(token.kind == TokenKind.id) {
			Token id_ = token;
			SourcePosition id_posn = getCurrPosn();
			acceptIt();
			
			//This covers the declaration&assignment cases
			//id id = expression;
			if(token.kind == TokenKind.id) {
				VarDecl varDecl = new VarDecl(new ClassType(new Identifier(id_),id_posn),token.spelling,getCurrPosn());
				acceptIt();
				Expression exp = parseAssignment();
				return new VarDeclStmt(varDecl,exp,stmtPosn);
			} else if(token.kind == TokenKind.o_brack) {
				acceptIt();
				//id[] id = expression;
				if(token.kind == TokenKind.c_brack) {
					acceptIt();
					VarDecl varDecl = new VarDecl(new ArrayType(new ClassType(new Identifier(id_),id_posn),id_posn),token.spelling,getCurrPosn());
					accept(TokenKind.id);
					Expression exp = parseAssignment();
					return new VarDeclStmt(varDecl,exp,stmtPosn);
				//id [expression] = expression;
				} else {
					IdRef idRef = new IdRef(new Identifier(id_),id_posn);
					Expression exp = parseExpression();
					accept(TokenKind.c_brack);
					Expression ass = parseAssignment();
					return new IxAssignStmt(idRef,exp,ass,stmtPosn);
				}
			//This covers cases that start with reference
			} else {
				Reference ref = new IdRef(new Identifier(id_),id_posn);
				//id(.id)*
				if(token.kind == TokenKind.dot) {
					acceptIt();
					ref = parseNestedReference(ref);
				}
				
				//id(.id)* (ArgumentList);
				if(token.kind == TokenKind.o_paren) {
					acceptIt();
					ExprList expList = new ExprList();
					if(token.kind != TokenKind.c_paren) {
						expList = parseArgumentList();
					} 
					accept(TokenKind.c_paren);
					accept(TokenKind.eol);
					
					return new CallStmt(ref, expList, stmtPosn);
				
				//id(.id)* [expression] = expression;
				} else if(token.kind == TokenKind.o_brack){
					acceptIt();
					Expression exp = parseExpression();
					accept(TokenKind.c_brack);
					Expression ass = parseAssignment();
					
					return new IxAssignStmt(ref,exp,ass,stmtPosn);
					
				//id(.id)* = expression;
				} else {
					Expression exp = parseAssignment();
					return new AssignStmt(ref,exp,stmtPosn);
				}
			}
		} else if(token.kind == TokenKind.kw_this) {
			SourcePosition thisPosn = getCurrPosn();
			acceptIt();
			Reference ref = new ThisRef(thisPosn);
			
			//this(.id)*
			if(token.kind == TokenKind.dot) {
				acceptIt();
				ref = parseNestedReference(ref);
			}
			
			//this(.id)* [expression] = expression;
			if(token.kind == TokenKind.o_brack) {
				acceptIt();
				Expression exp = parseExpression();
				accept(TokenKind.c_brack);
				Expression ass = parseAssignment();
				return new IxAssignStmt(ref,exp,ass,stmtPosn);
				
			//this(.id)* (argumentList) ;
			} else if(token.kind == TokenKind.o_paren) {
				acceptIt();
				ExprList expList = parseArgumentList();
				accept(TokenKind.c_paren);
				accept(TokenKind.eol);
				return new CallStmt(ref,expList,stmtPosn);
				
			//this(.id)* = expression;
			} else {
				Expression exp = parseAssignment();
				return new AssignStmt(ref,exp,stmtPosn);
			}
		} else {
			//type id = expression;
			TypeDenoter type = parseType();
			String name = token.spelling;
			SourcePosition idPosn = getCurrPosn();
			accept(TokenKind.id);
			Expression exp = parseAssignment();
			return new VarDeclStmt(new VarDecl(type,name,idPosn),exp,stmtPosn);
		}
	}
	
	private Expression parseAssignment() {
		accept(TokenKind.ass);
		Expression exp = parseExpression();
		accept(TokenKind.eol);
		return exp;
	}
	
	private ExprList parseArgumentList() {
		ExprList expList = new ExprList();
		if(token.kind == TokenKind.c_paren) {return expList;}
		expList.add(parseExpression());
		while(token.kind == TokenKind.comma) {
			acceptIt();
			expList.add(parseExpression());
		}
		return expList;
	}
	
	private Reference parseNestedReference(Reference ref) {
		Token id_= token;
		Reference qualRef = new QualRef(ref,new Identifier(id_),getCurrPosn());
		accept(TokenKind.id);
		while(token.kind == TokenKind.dot) {
			acceptIt();
			id_= token;
			qualRef = new QualRef(qualRef,new Identifier(id_),getCurrPosn());
			accept(TokenKind.id);
		}
		
		return qualRef;
	}
	
	private Expression parseExpression() {
		return parseD();
	}
	
	private Expression parseD() {
		Expression e = parseC();
		while(token.kind == TokenKind.or) {
			Token t = token;
			SourcePosition posn = getCurrPosn();
			acceptIt();
			e = new BinaryExpr(new Operator(t), e,parseC(),posn);
			
		}
		return e;
	}
	
	private Expression parseC() {
		Expression e = parseQ();
		while(token.kind == TokenKind.and) {
			Token t = token;
			SourcePosition posn = getCurrPosn();
			acceptIt();
			e = new BinaryExpr(new Operator(t), e,parseQ(),posn);
		}
		return e;
	}
	
	private Expression parseQ() {
		Expression e = parseR();
		while(token.kind == TokenKind.eq ||
				token.kind == TokenKind.neq) {
			Token t = token;
			SourcePosition posn = getCurrPosn();
			acceptIt();
			e = new BinaryExpr(new Operator(t), e,parseR(),posn);
		}
		return e;
	}
	
	private Expression parseR() {
		Expression e = parseA();
		while(token.kind == TokenKind.gt ||
				token.kind == TokenKind.lt ||
				token.kind == TokenKind.gte ||
				token.kind == TokenKind.lte) {
			Token t = token;
			SourcePosition posn = getCurrPosn();
			acceptIt();
			e = new BinaryExpr(new Operator(t), e,parseA(),posn);
		}
		return e;
	}
	
	private Expression parseA() {
		Expression e = parseM();
		while(token.kind == TokenKind.plus ||
				token.kind == TokenKind.minus) {
			Token t = token;
			SourcePosition posn = getCurrPosn();
			acceptIt();
			e = new BinaryExpr(new Operator(t), e,parseM(),posn);
		}
		return e;
	}
	
	private Expression parseM() {
		Expression e = parseF();
		while(token.kind == TokenKind.mult ||
				token.kind == TokenKind.divide) {
			Token t = token;
			SourcePosition posn = getCurrPosn();
			acceptIt();
			e = new BinaryExpr(new Operator(t), e,parseF(),posn);
		}
		return e;
	}
	
	private Expression parseF() {
		Expression e = null;
		SourcePosition exprPosn = getCurrPosn();
		// num
		if(token.kind == TokenKind.num) {
			e = new LiteralExpr(new IntLiteral(token),exprPosn);
			acceptIt();
			
		// true||false
		} else if(
				token.kind == TokenKind.kw_true ||
				token.kind == TokenKind.kw_false) {
			e = new LiteralExpr(new BooleanLiteral(token),exprPosn);
			acceptIt();
		} else if(token.kind == TokenKind.kw_new) {
			acceptIt();
			if(token.kind == TokenKind.id) {
				Token id = token;
				SourcePosition idPosn = getCurrPosn();
				acceptIt();
				
				// new id()
				if(token.kind == TokenKind.o_paren) {
					acceptIt();
					ExprList arguments = parseArgumentList();
					accept(TokenKind.c_paren);
					e = new NewObjectExpr(new ClassType(new Identifier(id), idPosn), arguments,exprPosn);
				
				// new id[ expression ]
				} else {
					accept(TokenKind.o_brack);
					e = parseExpression();
					accept(TokenKind.c_brack);
					e = new NewArrayExpr(new ClassType(new Identifier(id),idPosn),e,exprPosn);
				}
			
			//new int[ expression ]
			} else {
				SourcePosition typePosn = getCurrPosn();
				accept(TokenKind.kw_int);
				accept(TokenKind.o_brack);
				e = parseExpression();
				accept(TokenKind.c_brack);
				e = new NewArrayExpr(new BaseType(TypeKind.INT,typePosn),e,exprPosn);
			}
		
		// UniOp Expression
		} else if(token.kind == TokenKind.minus || token.kind == TokenKind.not) {
			Token t = token;
			acceptIt();
			e = new UnaryExpr(new Operator(t), parseF(), exprPosn);
		
		// ( Expression )
		} else if(token.kind == TokenKind.o_paren) {
			acceptIt();
			e = parseExpression();
			accept(TokenKind.c_paren);
		} else if (token.kind == TokenKind.id || token.kind == TokenKind.kw_this){
			Reference ref = 
					token.kind == TokenKind.id ?
							new IdRef(new Identifier(token),getCurrPosn()) :
								new ThisRef(getCurrPosn());
			acceptIt();
			
			if(token.kind == TokenKind.dot) {
				acceptIt();
				ref = parseNestedReference(ref);
			}
			
			//reference [ expression ]
			if(token.kind == TokenKind.o_brack) {
				acceptIt();
				e = parseExpression();
				accept(TokenKind.c_brack);
				e = new IxExpr(ref, e, exprPosn);
				
			//reference ( argumentList )
			} else if(token.kind == TokenKind.o_paren) {
				acceptIt();
				ExprList expList = parseArgumentList();
				accept(TokenKind.c_paren);
				e = new CallExpr(ref,expList,exprPosn);
			} else {
				e = new RefExpr(ref,exprPosn);
			}
		} else if (token.kind == TokenKind.kw_null) {
			e = new LiteralExpr(new NullLiteral(token),exprPosn);
			acceptIt();
		}
		if(e == null) {throw new SyntaxError();}
		return e;
	}
}