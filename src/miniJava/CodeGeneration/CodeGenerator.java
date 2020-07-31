package miniJava.CodeGeneration;

import java.util.ArrayList;
import java.util.HashMap;

import mJAM.Machine;
import mJAM.Machine.Op;
import mJAM.Machine.Prim;
import mJAM.Machine.Reg;
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
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.Expression;
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
import miniJava.AbstractSyntaxTrees.Reference;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.SyntacticAnalyzer.TokenKind;

public class CodeGenerator implements Visitor<Object,Object>{
	
	Package ast;
	
/* Ingenius patching solution for:
 * - Method jumps
 * - Field addresses
 * - Class sizes
 */
	
	int mainAddr; // Address of main method
	
	HashMap<Declaration, ArrayList<Integer>> patches = new HashMap<>();
	
	/* Must be called IMMEDIATELY before instruction to be patched is generated
	 * Attempts to fetch red value, if failure it adds to the declarations patch list
	 */
	private int fetchAndPatch(Declaration d) {
		int value = -1;
		if(d.red == null) {
			if(!patches.containsKey(d)) {
				patches.put(d, new ArrayList<>());
			}
			patches.get(d).add(Machine.nextInstrAddr());
		} else {
			value = d.red.value;
		}
		
		return value;
	}
	
	// Patches all missing values once declaration red is created
	private void patchAll(Declaration d) {
		if(patches.containsKey(d)) {
			for(int patchAddr : patches.get(d)) {
				Machine.patch(patchAddr, d.red.value);
			}
		}
	}
	
/* End Patch Solution */
	
	public CodeGenerator(Package ast) {
		this.ast = ast;
	}
	
	public int generateCode() {
		Machine.initCodeGen();
		ast.visit(this, null);
		return 0;
	}
	
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// PACKAGE
	//
	///////////////////////////////////////////////////////////////////////////////
	
	public Object visitPackage(Package ast, Object obj) {
		
		
		
		for(ClassDecl c : ast.classDeclList) {
			c.visit(this, true);
		}
		
		/* Preamble */
		Machine.emit(Op.LOADL, 0); 
		Machine.emit(Prim.newarr); //Create new empty array	
		
		int mainPatch = Machine.nextInstrAddr();
		
		Machine.emit(Op.CALL, Reg.CB, -1);
		Machine.emit(Op.HALT,0,0,0);
		
		for(ClassDecl c : ast.classDeclList) {
			c.visit(this, false);
		}
		
		/* Postamble */
		Machine.patch(mainPatch, mainAddr);
				
		return null;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// DECLARATION
	//
	///////////////////////////////////////////////////////////////////////////////
	
	public Object visitClassDecl(ClassDecl cd, Object obj) {
		
		if(obj!=null && (boolean)obj) {
			int size = 0;
			for(FieldDecl fd : cd.fieldDeclList) {
				if(!fd.isStatic) {
					fd.visit(this, size);
					size++;
				} else {
					fd.visit(this, null);
				}
			}
			
			cd.red = new RuntimeEntity(size);
			patchAll(cd);
		} else {
			for(MethodDecl md: cd.methodDeclList) {
				md.visit(this, null);
			}
		}
		
		return null;
	}
	
	int SB_offset = 0;
	public Object visitFieldDecl(FieldDecl fd, Object obj) {
		if(fd.isStatic) {
			fd.red = new RuntimeEntity(SB_offset);
			
			if(fd.exp == null) {
				Machine.emit(Op.LOADL,0);
			} else {
				fd.exp.visit(this, null);
			}
			
			
			SB_offset++;
		} else {
			fd.red = new RuntimeEntity((int)obj);
		}
		
		patchAll(fd);
		
		return null;
	}
	
	int paramCount;
	int LB_offset;
	public Object visitMethodDecl(MethodDecl md, Object obj) {
		md.red = new RuntimeEntity(Machine.nextInstrAddr());
		
		if(md.isMain) mainAddr = md.red.value;
		
		LB_offset = 3;
		
		patchAll(md);
		
		for(int i=0; i<md.parameterDeclList.size(); i++) {
			md.parameterDeclList.get(i).red = new RuntimeEntity(i-md.parameterDeclList.size());
		}
		
		paramCount = md.parameterDeclList.size();
		for(Statement s : md.statementList) {
			s.visit(this, null);
		}
		
		if(md.type.typeKind == TypeKind.VOID) {
			if(md.name.equals("_constructor")) {
				Machine.emit(Op.LOADA,Reg.OB,0);
				Machine.emit(Op.RETURN,1,1,md.parameterDeclList.size());
			} else {
				Machine.emit(Op.RETURN, 0, 0, md.parameterDeclList.size());				
			}
		}
		
		return null;
	}
	
	public Object visitParameterDecl(ParameterDecl pd, Object obj) {return null;}
	
	public Object visitVarDecl(VarDecl vd, Object obj) {return null;}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// Statements
	//
	///////////////////////////////////////////////////////////////////////////////
	
	public Object visitAssignStmt(AssignStmt s, Object obj) {
		s.val.visit(this, null); //push value/address of expr onto stack
		s.ref.visit(this, true); //push address of ref onto stack
		
		Machine.emit(Op.STOREI); //store the value/address of expr at address of ref
		
		return null;
	}
	
	int newVarCount;
	public Object visitBlockStmt(BlockStmt s, Object obj) {
		newVarCount = 0;
		
		for(Statement stmt : s.sl)
			stmt.visit(this,null);
		
		if(newVarCount > 0) {
			Machine.emit(Op.POP,0,0,newVarCount);
			LB_offset = LB_offset - newVarCount;
		}
		
		return null;
	}
	
	private boolean isPrintln(Reference ref) {
		if(ref instanceof QualRef) {
			QualRef p = (QualRef) ref;
			if(p.ref instanceof QualRef) {
				QualRef o = (QualRef) p.ref;
				if(o.decl.type instanceof ClassType) {
					if(((ClassType)o.decl.type).className.spelling.equals("_PrintStream")) {
						Machine.emit(Prim.putintnl);
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public Object visitCallStmt(CallStmt s, Object obj) {
		for(Expression e : s.argList)
			e.visit(this,null);
		
		if(isPrintln(s.methodRef)) {
			return null;
		}
		
		int addr = (int)s.methodRef.visit(this, null);
		
		if(((MethodDecl)s.methodRef.decl).isStatic) {
			Machine.emit(Op.CALL,Reg.CB,addr);
		} else {
			Machine.emit(Op.CALLI,Reg.CB,addr);
		}
		
		if(((MethodDecl)s.methodRef.decl).type.typeKind != TypeKind.VOID){
			Machine.emit(Op.POP,0,0,1);
		}
		
		return null;
	}
	
	public Object visitIfStmt(IfStmt s, Object obj) {
		s.cond.visit(this, null);
		
		int p1, p2, j1, j2;
		
		p1 = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF,0,Reg.CB,-1);
		
		s.thenStmt.visit(this, null);
		
		if(s.elseStmt!=null) {
			p2 = Machine.nextInstrAddr();
			Machine.emit(Op.JUMP,Reg.CB,-1); 
			
			j1 = Machine.nextInstrAddr();
			if(s.elseStmt!=null) s.elseStmt.visit(this, null);
			j2 = Machine.nextInstrAddr();
			
			Machine.patch(p1, j1);
			Machine.patch(p2, j2);
		} else {
			Machine.patch(p1, Machine.nextInstrAddr());
		}
		
		return null;
	}
	
	public Object visitIxAssignStmt(IxAssignStmt s, Object obj) {
		s.ref.visit(this, null);
		s.ix.visit(this, null);
		s.exp.visit(this, null);
		
		Machine.emit(Prim.arrayupd);
		
		return null;
	}
	
	public Object visitReturnStmt(ReturnStmt s, Object obj) {
		int n = 0;
		if(s.returnExpr!=null) {
			s.returnExpr.visit(this, null);
			n = 1;
		}
		
		Machine.emit(Op.RETURN, n, 0, paramCount);
		
		return null;
	}
	
	public Object visitVardeclStmt(VarDeclStmt s, Object obj) {
		s.initExp.visit(this, null);
		s.varDecl.red = new RuntimeEntity(LB_offset);
		LB_offset++;
		newVarCount++;

		return null;
	}
	
	public Object visitWhileStmt(WhileStmt s, Object obj) {
		
		int j, j1, p1;
		
		j = Machine.nextInstrAddr();
		s.cond.visit(this, null);
		
		p1 = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF,0,Reg.CB,-1);
		
		s.body.visit(this, null);
		Machine.emit(Op.JUMP,Reg.CB,j);
		
		j1 = Machine.nextInstrAddr();
		
		Machine.patch(p1, j1);
		
		return null;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// References
	//
	///////////////////////////////////////////////////////////////////////////////
	
	public Object visitIdRef(IdRef ref, Object obj) {
		Machine.Op op;
		if(obj==null)
			op = Op.LOAD;
		else
			op = Op.LOADA;
		
		if(ref.decl instanceof FieldDecl) {
			FieldDecl fd = (FieldDecl) ref.decl;
			int fdAddr = fetchAndPatch(fd);
			if(fd.isStatic) {
				Machine.emit(op, Reg.SB, fdAddr);
			} else {
				Machine.emit(op, Reg.OB, fdAddr);
			}
		} else if(ref.decl instanceof VarDecl || ref.decl instanceof ParameterDecl) {
			Machine.emit(op, Reg.LB, ref.decl.red.value);
		} else if(ref.decl instanceof ClassDecl) {
			Machine.emit(op, Reg.SB, 0);
		} else if(ref.decl instanceof MethodDecl) {
			MethodDecl md = (MethodDecl) ref.decl;
			if(!md.isStatic) Machine.emit(Op.LOADA, Reg.OB, 0);
			return fetchAndPatch(md);
		}
		
		return null;
	}
	
	public Object visitThisRef(ThisRef ref, Object obj) {
		Machine.emit(Op.LOADA, Reg.OB, 0);
		
		return null;
	}
	
	public Object visitQRef(QualRef ref, Object obj) {
		
		
		/* Method Reference
		 * - will return method addr or add to patch list
		 * - will push instance addr if non-static method
		 */
		if(ref.decl instanceof MethodDecl) {
			//Get MethodDecl
			MethodDecl md = (MethodDecl) ref.decl;
			
			//Load instance address onto stack
			if(!md.isStatic) {
				ref.ref.visit(this, null);
			}

			return fetchAndPatch(md);
		}
		
		/* Array Length Reference */
		if(ref.id.spelling.equals("length")) {
			ref.ref.visit(this, null);
			Machine.emit(Prim.arraylen);
			return null;
		}
		
		/* Field Reference */
		FieldDecl fd = (FieldDecl) ref.id.decl;
		
		if(!fd.isStatic) {
			ref.ref.visit(this, null);
		}
		
		int fdAddr = fetchAndPatch(fd);
		
		if(fd.isStatic) {
			Machine.emit(Op.LOADA, Reg.SB, fdAddr);
		} else {
			if(fdAddr!=0) {
				Machine.emit(Op.LOADL, fdAddr);
				Machine.emit(Prim.add);
			}
		}
		
		if(obj == null)
			Machine.emit(Op.LOADI);
		
		return null;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// Expressions
	//
	///////////////////////////////////////////////////////////////////////////////
	
	public Object visitBinaryExpr(BinaryExpr expr, Object obj) {
		
		expr.left.visit(this, null);
		
		int j = -1;
		if(expr.operator.kind == TokenKind.and || expr.operator.kind == TokenKind.or) {
			Machine.emit(Op.LOAD, 1,Reg.ST,-1);
			j = Machine.nextInstrAddr();
			Machine.emit(Op.JUMPIF, expr.operator.kind == TokenKind.and ? 0:1, Reg.CB, -1);
		}
		
		
		expr.right.visit(this, null);
		expr.operator.visit(this, null);
		
		if(j!=-1) Machine.patch(j, Machine.nextInstrAddr());
		
		return null;
	}
	
	public Object visitCallExpr(CallExpr expr, Object obj) {
		for(Expression e : expr.argList)
			e.visit(this,null);
		
		if(isPrintln(expr.functionRef)) {
			return null;
		}
		
		int addr = (int)expr.functionRef.visit(this, null);
	
		if(((MethodDecl)expr.functionRef.decl).isStatic) {
			Machine.emit(Op.CALL,Reg.CB,addr);
		} else {
			Machine.emit(Op.CALLI,Reg.CB,addr);
		}
			
		return null;
	}
	
	public Object visitIxExpr(IxExpr expr, Object obj) {
		expr.ref.visit(this, null);
		expr.ixExpr.visit(this, null);
		Machine.emit(Prim.arrayref);
		
		return null;
	}
	
	public Object visitLiteralExpr(LiteralExpr expr, Object obj) {
		expr.lit.visit(this, null);
		return null;
	}
	
	public Object visitNewArrayExpr(NewArrayExpr expr, Object obj) {
		expr.sizeExpr.visit(this, null);
		Machine.emit(Prim.newarr);
		return null;
	}

	public Object visitNewObjectExpr(NewObjectExpr expr, Object obj) {
		for(Expression e : expr.arguments)
			e.visit(this,null);
		
		Machine.emit(Op.LOADL, -1);
		
		ClassDecl cd = (ClassDecl) expr.classtype.className.decl;
		int size = fetchAndPatch(cd);
		
		Machine.emit(Op.LOADL, size);
		Machine.emit(Prim.newobj);
		
		if(cd.constructor!=null) {
			int addr = fetchAndPatch(cd.constructor);
			Machine.emit(Op.CALLI,Reg.CB,addr);
		}
		
		return null;
	}

	public Object visitRefExpr(RefExpr expr, Object obj) {
		expr.ref.visit(this, null);
		
		return null;
	}
	
	public Object visitUnaryExpr(UnaryExpr expr, Object obj) {
		expr.expr.visit(this, null);
		if(expr.operator.kind == TokenKind.minus)
			Machine.emit(Prim.neg);
		if(expr.operator.kind == TokenKind.not)
			Machine.emit(Prim.not);
		
		return null;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// Terminals
	//
	///////////////////////////////////////////////////////////////////////////////
	
	public Object visitIdentifier(Identifier id, Object arg) {return null;}
	
    public Object visitOperator(Operator op, Object arg) {
		switch(op.kind) {
			case plus:
				Machine.emit(Prim.add);
				break;
			case minus:
				Machine.emit(Prim.sub);
				break;
			case mult:
				Machine.emit(Prim.mult);
				break;
			case divide:
				Machine.emit(Prim.div);
				break;
			case and:
				Machine.emit(Prim.and);
				break;
			case or:
				Machine.emit(Prim.or);
				break;
			case gt:
				Machine.emit(Prim.gt);
				break;
			case lt:
				Machine.emit(Prim.lt);
				break;
			case gte:
				Machine.emit(Prim.ge);
				break;
			case lte:
				Machine.emit(Prim.le);
				break;
			case eq:
				Machine.emit(Prim.eq);
				break;
			case neq:
				Machine.emit(Prim.ne);
				break;
				
			//never reach
			default: break;
    	}
    	return null;
    }
    
    
    public Object visitIntLiteral(IntLiteral num, Object arg) {
    	Machine.emit(Op.LOADL, Integer.parseInt(num.spelling));
    	
    	return null;
    }
    
    public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
    	if(bool.spelling.equals("true"))
    		Machine.emit(Op.LOADL, Machine.trueRep);
    	else
    		Machine.emit(Op.LOADL, Machine.falseRep);
    	
    	return null;
    }
    
    public Object visitNullLiteral(NullLiteral nul, Object arg) {
    	Machine.emit(Op.LOADL, Machine.nullRep);
    	
    	return null;
    }
    
	///////////////////////////////////////////////////////////////////////////////
	//
	// Types
	//
	///////////////////////////////////////////////////////////////////////////////
    
    public Object visitBaseType(BaseType type, Object arg) {return null;}
    public Object visitClassType(ClassType type, Object arg) {return null;}
    public Object visitArrayType(ArrayType type, Object arg) {return null;}
}