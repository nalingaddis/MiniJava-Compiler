package miniJava.ContextualAnalysis;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Stack;

import miniJava.AbstractSyntaxTrees.Declaration;

public class IdentificationTable {
	HashMap<String,Declaration> l1 = new HashMap<>();
	HashMap<String,HashMap<String,Declaration>> l2 = new HashMap<>();
	Stack<HashMap<String,Declaration>> idTable = new Stack<>();
	
	
	public IdentificationTable() {
		
	}
	
	public boolean putClass(String name, Declaration decl){
		if(l1.put(name, decl)==null) {
			l2.put(name, new HashMap<>());
			return true;
		} return false;
	}
	
	public boolean putMember(String className, String memberName, Declaration decl) {
		if(l2.get(className)!=null) {
			if(l2.get(className).put(memberName, decl)==null) {
				return true;
			}
		} return false;
	}
	
	
	public boolean putId(String name, Declaration decl) {
		if(idTable.peek().put(name, decl)==null) {
			return true;
		} return false;
	}
	
	public Declaration getClass(String name) {
		return l1.get(name);
	}
	
	public Declaration getMember(String className, String memberName) {
		return l2.get(className).get(memberName);
	}
	
	public Declaration getId(String name) {
		if(idTable.empty()) return null;
		return idTable.peek().get(name);
	}
	
	public Declaration get(String idName) {
		Declaration decl = getId(idName);
		
		if(decl == null) {
			for(Entry<String, HashMap<String, Declaration>> x : l2.entrySet()) {
				decl = x.getValue().get(idName);
				if(decl != null) break;
			}
		}
		
		if(decl == null) {
			decl = getClass(idName);
		}
		
		return decl;
	}
	
	@SuppressWarnings("unchecked")
	public void scopeIn() {
		if(idTable.empty()) {
			idTable.push(new HashMap<>());
		} else {
			idTable.push((HashMap<String, Declaration>) idTable.peek().clone());
		}
		
	}
	
	public void scopeOut() {
		if(!idTable.empty()) {idTable.pop();}
	}
}