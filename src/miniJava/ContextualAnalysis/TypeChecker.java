package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.AssignStmt;
import miniJava.AbstractSyntaxTrees.BaseType;
import miniJava.AbstractSyntaxTrees.BinaryExpr;
import miniJava.AbstractSyntaxTrees.BlockStmt;
import miniJava.AbstractSyntaxTrees.BooleanLiteral;
import miniJava.AbstractSyntaxTrees.CallExpr;
import miniJava.AbstractSyntaxTrees.CallStmt;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.IxAssignStmt;
import miniJava.AbstractSyntaxTrees.IxExpr;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.NewArrayExpr;
import miniJava.AbstractSyntaxTrees.NewObjectExpr;
import miniJava.AbstractSyntaxTrees.NullLiteral;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.QualRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeDenoter;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class TypeChecker implements Visitor<TypeDenoter, TypeDenoter>{

	Package ast;
	boolean clean = true;
	boolean didReturn = false;
	boolean isConditional = false;
	
	public TypeChecker(Package ast) {
		this.ast = ast;
	}
	
	public boolean typify() {
		ast.visit(this,null);
		return clean;
	}
	
	private TypeDenoter log(SourcePosition posn, boolean error, String msg) {
		System.out.println("*** line "+posn.line+": Type Error - "+msg);
		
		clean = false;
		
		return error ? new BaseType(TypeKind.ERROR, posn) : new BaseType(TypeKind.UNSUPPORTED, posn);
	}
	
	private boolean equals(TypeDenoter a, TypeDenoter b) {
		
		if(a.typeKind == TypeKind.ERROR || b.typeKind == TypeKind.ERROR) return true;
		if(a.typeKind == TypeKind.UNSUPPORTED || b.typeKind == TypeKind.UNSUPPORTED) return false;
		
		if(a instanceof BaseType && b instanceof BaseType) 
			return a.typeKind == b.typeKind;
		
		else if(a instanceof ArrayType && b instanceof ArrayType) 
			return equals(((ArrayType)a).eltType, ((ArrayType)b).eltType);

		else if(a instanceof ClassType && b instanceof ClassType) 
			return ((ClassType)a).className.spelling.equals(((ClassType)b).className.spelling);
		
		else if(a instanceof ClassType && b.typeKind == TypeKind.NULL
				|| b instanceof ClassType && a.typeKind == TypeKind.NULL) 
			return true;
		
		return false;
	}			
	
	private boolean isString(TypeDenoter t) {
		if(t.typeKind == TypeKind.CLASS) {
			if(((ClassType)t).className.spelling.equals("String")) {
//				log(t.posn,false,"String type not supported");
				return true;
			}
		} return false;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// PACKAGE
	//
	/////////////////////////////////////////////////////////////////////////////// 
	
	public TypeDenoter visitPackage(Package prog, TypeDenoter arg) {
		boolean clean = true;
		
		for(ClassDecl cd : prog.classDeclList) {
			if(cd.visit(this,null) != null) clean = false;
		} 
		
		return clean ? null : new BaseType(TypeKind.ERROR, prog.posn);
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// DECLARATIONS
	//
	/////////////////////////////////////////////////////////////////////////////// 

	
	public TypeDenoter visitClassDecl(ClassDecl cd, TypeDenoter arg) {
		boolean clean = true;
		
		for(FieldDecl fd : cd.fieldDeclList) {
			TypeDenoter x = fd.visit(this,null);
			if(x.typeKind == TypeKind.ERROR && x.typeKind == TypeKind.UNSUPPORTED) clean = false;
			if(fd.exp != null && !equals(x,fd.exp.visit(this, null))) {
				log(fd.posn, true, "Field type does not equal expression type.");
				clean = false;
			}
		}
		
		for(MethodDecl md : cd.methodDeclList) {
			if(md.visit(this, null)!=null) clean = false;
		}
		
		return clean ? null : new BaseType(TypeKind.ERROR, cd.posn);
	}

	public TypeDenoter visitFieldDecl(FieldDecl fd, TypeDenoter arg) {
		return fd.type.visit(this, null);
	}

	public TypeDenoter visitMethodDecl(MethodDecl md, TypeDenoter arg) {
		boolean clean = true;
		
		md.type.visit(this, null);
		
		for(Statement stmt : md.statementList) {
			if(stmt.visit(this, md.type)!=null) clean = false;
		}
		
		if(!didReturn && md.type.typeKind != TypeKind.VOID) {
			log(md.posn,true,"Method fails to return value");
			clean = false;
		}
		return clean ? null : new BaseType(TypeKind.ERROR, md.posn);
	}

	
	public TypeDenoter visitParameterDecl(ParameterDecl pd, TypeDenoter arg) {
		return pd.type.visit(this, null);
	}

	@Override
	public TypeDenoter visitVarDecl(VarDecl decl, TypeDenoter arg) {
		return decl.type.visit(this, null);
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	//
	/////////////////////////////////////////////////////////////////////////////// 

	@Override
	public TypeDenoter visitBaseType(BaseType type, TypeDenoter arg) {
		return type;
	}

	@Override
	public TypeDenoter visitClassType(ClassType type, TypeDenoter arg) {
		return isString(type) ? log(type.posn, false, "String type not supported") : type;
	}

	@Override
	public TypeDenoter visitArrayType(ArrayType type, TypeDenoter arg) {
		if(isString(type.eltType)) log(type.posn, false, "String type not supported");
		return type;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	/////////////////////////////////////////////////////////////////////////////// 

	public TypeDenoter visitBlockStmt(BlockStmt stmt, TypeDenoter arg) {
		boolean clean = false;
		
		for(Statement s : stmt.sl) {
			if(s.visit(this, arg)!=null) clean=false;
		}
		
		return clean ? null : new BaseType(TypeKind.ERROR, stmt.posn);
	}

	public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, TypeDenoter arg) {
		return equals(
				stmt.initExp.visit(this, null), 
				stmt.varDecl.visit(this, null)
			) ? null : log(stmt.posn, true, "Expression type does not equal variable type");
	}

	public TypeDenoter visitAssignStmt(AssignStmt stmt, TypeDenoter arg) {
		return equals(
				stmt.ref.visit(this, null), 
				stmt.val.visit(this, null)
			) ? null : log(stmt.posn, true, "Expression type does not equal variable type");
	}

	public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, TypeDenoter arg) {
		if(!equals(stmt.ix.visit(this, null),new BaseType(TypeKind.INT,stmt.posn))) {
			log(stmt.ix.posn,true, "Index is not of int type");
		}
		
		TypeDenoter t = stmt.ref.visit(this,null);
		
		if(!(t instanceof ArrayType)) {
			return log(stmt.ref.posn,true, "Variable is not of array type");
		}
		
		return equals(
				stmt.exp.visit(this, null),
				((ArrayType)t).eltType
			) ? null : log(stmt.posn, true, "Expression type does not equal variable type");
	}

	public TypeDenoter visitCallStmt(CallStmt stmt, TypeDenoter arg) {
		if(!(stmt.methodRef.decl instanceof MethodDecl)) {
			log(stmt.methodRef.posn, true, "Reference is not a method");
			return new BaseType(TypeKind.ERROR,stmt.posn);
		}
			
		MethodDecl decl = (MethodDecl) stmt.methodRef.decl;
		boolean clean = true;
		
		if(decl.parameterDeclList.size() != stmt.argList.size()) {
			log(stmt.posn,true, "Incorrect number of arguements given");
			clean = false;
		} else {
			for(int i=0; i<stmt.argList.size(); i++) {
				if(!equals(
						stmt.argList.get(i).visit(this, null),
						decl.parameterDeclList.get(i).visit(this, null)
				)){
					log(stmt.argList.get(i).posn,true,"Argument type does not equal parameter type");
					clean = false;
				}
			}
		}
		
		return clean ? null : new BaseType(TypeKind.ERROR,stmt.posn);
	}

	public TypeDenoter visitReturnStmt(ReturnStmt stmt, TypeDenoter arg) {
		if(stmt.returnExpr == null && arg.typeKind == TypeKind.VOID)
			return null;
		
		didReturn = !isConditional;
		
		return equals(
				stmt.returnExpr.visit(this, arg),
				arg
			) ? null : log(stmt.posn, true, "Return type does not equal method type");
	}

	public TypeDenoter visitIfStmt(IfStmt stmt, TypeDenoter arg) {
		boolean clean = true;
		isConditional = stmt.elseStmt==null;
		
		if(!equals(
				stmt.cond.visit(this, null),
				new BaseType(TypeKind.BOOLEAN,stmt.cond.posn))
			)
		{
			log(stmt.cond.posn, true, "Conditional expression is not of boolean type");
			clean = false;
		}
		
		if(stmt.thenStmt.visit(this, arg)!=null) clean = false;
		if(stmt.elseStmt!=null && stmt.elseStmt.visit(this, arg)!=null) clean = false;
		
		isConditional = false;
		return clean ? null : new BaseType(TypeKind.ERROR,stmt.posn);
	}

	public TypeDenoter visitWhileStmt(WhileStmt stmt, TypeDenoter arg) {
		boolean clean = true;
		isConditional = true;
		
		if(!equals(
				stmt.cond.visit(this, null),
				new BaseType(TypeKind.BOOLEAN,stmt.cond.posn))
			)
		{
			log(stmt.cond.posn, true, "Conditional expression is not of boolean type");
			clean = false;
		}
		
		if(stmt.body.visit(this, arg)!=null) clean = false;
		
		isConditional = false;
		return clean ? null : new BaseType(TypeKind.ERROR,stmt.posn);
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	/////////////////////////////////////////////////////////////////////////////// 

	public TypeDenoter visitUnaryExpr(UnaryExpr expr, TypeDenoter arg) {
		expr.expr.visit(this, null);
		
		if(expr.expr.type.typeKind == TypeKind.INT) {
			expr.type = new BaseType(TypeKind.INT, expr.posn);
		} 
		else if(expr.expr.type.typeKind == TypeKind.BOOLEAN) {
			expr.type = new BaseType(TypeKind.BOOLEAN, expr.posn);
		} 
		else {
			expr.type = new BaseType(TypeKind.ERROR, expr.posn);
			log(expr.posn, true, "Invalid type for unary expression");
		}
		
		return expr.type;
	}

	public TypeDenoter visitBinaryExpr(BinaryExpr expr, TypeDenoter arg) {
		TypeDenoter left = expr.left.visit(this, null), right = expr.right.visit(this, null);
		
		if(left.typeKind == TypeKind.ERROR && right.typeKind == TypeKind.ERROR
			|| left.typeKind == TypeKind.UNSUPPORTED 
			|| right.typeKind == TypeKind.UNSUPPORTED) {
			expr.type = new BaseType(TypeKind.ERROR, expr.posn);
			return expr.type;
		}
		else if(left.typeKind == TypeKind.ERROR) {
			left = right;
		}
		else if(right.typeKind == TypeKind.ERROR) {
			right = left;
		}
		
		if(equals(left,right)) {
			switch(expr.operator.spelling) {
				// int X int -> int
				case "+": case "-": case "*": case "/":
					if(left.typeKind == TypeKind.INT && right.typeKind == TypeKind.INT) {
						expr.type = new BaseType(TypeKind.INT, expr.posn);
					} else {
						expr.type = log(expr.posn,true,expr.operator.spelling+" operator requires int operands");
					}
					break;
					
				// int X int -> bool
				case "<": case ">": case "<=": case ">=":
					if(left.typeKind == TypeKind.INT && right.typeKind == TypeKind.INT) {
						expr.type = new BaseType(TypeKind.BOOLEAN, expr.posn);
					} else {
						expr.type = log(expr.posn,true, expr.operator.spelling+" operator requires int operands");
					}
					break;
				
				// obj X obj -> bool
				case "==": case "!=":
					expr.type = new BaseType(TypeKind.BOOLEAN, expr.posn);
					break;
					
				// bool X bool -> bool
				case "&&": case "||":
					if(left.typeKind == TypeKind.BOOLEAN && right.typeKind == TypeKind.BOOLEAN) {
						expr.type = new BaseType(TypeKind.BOOLEAN, expr.posn);
					} else {
						expr.type = log(expr.posn,true,expr.operator.spelling+" operator requires boolean operands");
					}
					break;
			}
		} else {
			expr.type = log(expr.posn,true,"Operands are not of the same type");
		}
		
		return expr.type;
	}

	public TypeDenoter visitRefExpr(RefExpr expr, TypeDenoter arg) {
		expr.type =  expr.ref.visit(this, null);
		return expr.type;
	}

	public TypeDenoter visitIxExpr(IxExpr expr, TypeDenoter arg) {
		if(expr.ixExpr.visit(this, null).typeKind != TypeKind.INT)
			log(expr.ixExpr.posn, true, "Index is not of type int");
		
		TypeDenoter t = expr.ref.visit(this, null);
		
		if(!(t instanceof ArrayType)) {
			log(expr.ref.posn,true,"Reference is not of array type");
			return new BaseType(TypeKind.ERROR,expr.ref.posn);
		}	
		
		expr.type = ((ArrayType)t).eltType;
		return expr.type;
	}

	public TypeDenoter visitCallExpr(CallExpr expr, TypeDenoter arg) {
		if(!(expr.functionRef.decl instanceof MethodDecl)) {
			log(expr.functionRef.posn, true,"Reference is not a method");
			return new BaseType(TypeKind.ERROR,expr.posn);
		}
		
		MethodDecl decl = (MethodDecl) expr.functionRef.decl;
		boolean clean = true;
		
		if(decl.parameterDeclList.size() != expr.argList.size()) {
			log(expr.posn,true,"Incorrect number of arguements given");
			clean = false;
		} else {
			for(int i=0; i<expr.argList.size(); i++) {
				if(!equals(
						expr.argList.get(i).visit(this, null),
						decl.parameterDeclList.get(i).visit(this, null)
				)){
					log(expr.argList.get(i).posn,true,"Argument type does not equal parameter type");
					clean = false;
				}
			}
		}
		
		expr.type = clean ? decl.type : new BaseType(TypeKind.ERROR,expr.posn);
		return expr.type;
	}

	public TypeDenoter visitLiteralExpr(LiteralExpr expr, TypeDenoter arg) {
		expr.type = expr.lit.visit(this, null);
		return expr.type;
	}

	public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, TypeDenoter arg) {
		expr.type = expr.classtype.visit(this, null);
		
		MethodDecl decl = ((ClassDecl)expr.classtype.className.decl).constructor;
		if(decl!=null) {
			if(decl.parameterDeclList.size() != expr.arguments.size()) {
				log(expr.posn,true,"Incorrect number of arguements given");
				clean = false;
			} else {
				for(int i=0; i<expr.arguments.size(); i++) {
					if(!equals(
							expr.arguments.get(i).visit(this, null),
							decl.parameterDeclList.get(i).visit(this, null)
					)){
						log(expr.arguments.get(i).posn,true,"Argument type does not equal parameter type");
						clean = false;
					}
				}
			}
		} else {
			if(expr.arguments.size()!=0) {
				log(expr.posn,true,"Incorrect number of arguments given");
			}
		}
		return expr.type;
	}

	public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, TypeDenoter arg) {
		if(expr.sizeExpr.visit(this, null).typeKind != TypeKind.INT) {
			log(expr.sizeExpr.posn, true,"Size expression not of type int");
		}
		
		expr.type = new ArrayType(expr.eltType.visit(this, null),expr.posn);
		return expr.type;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	/////////////////////////////////////////////////////////////////////////////// 

	public TypeDenoter visitThisRef(ThisRef ref, TypeDenoter arg) {
		return new ClassType(new Identifier(new Token(ref.decl.name,TokenKind.id,0,0)),ref.posn);
	}

	public TypeDenoter visitIdRef(IdRef ref, TypeDenoter arg) {
		return ref.decl instanceof MethodDecl || ref.decl instanceof ClassDecl ? log(ref.posn, true, "Invalid reference") : ref.decl.visit(this,null);
	}

	public TypeDenoter visitQRef(QualRef ref, TypeDenoter arg) {
		if(!(ref.ref.decl instanceof ClassDecl)) 
			ref.ref.visit(this, null);
		if(ref.id.spelling.equals("length"))
			return new BaseType(TypeKind.INT,ref.posn);
		return ref.decl instanceof MethodDecl || ref.decl instanceof ClassDecl ? log(ref.posn, true, "Invalid reference") : ref.decl.visit(this,null);
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	/////////////////////////////////////////////////////////////////////////////// 

	public TypeDenoter visitIdentifier(Identifier id, TypeDenoter arg) {
		return id.spelling.equals("String") ? log(id.posn,false, "String type is not supported") : new ClassType(id,id.posn);
	}

	@Override
	public TypeDenoter visitOperator(Operator op, TypeDenoter arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypeDenoter visitIntLiteral(IntLiteral num, TypeDenoter arg) {
		return new BaseType(TypeKind.INT,num.posn);
	}

	@Override
	public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, TypeDenoter arg) {
		return new BaseType(TypeKind.BOOLEAN,bool.posn);
	}

	@Override
	public TypeDenoter visitNullLiteral(NullLiteral nul, TypeDenoter arg) {
		return new BaseType(TypeKind.NULL,nul.posn);
	}
}
