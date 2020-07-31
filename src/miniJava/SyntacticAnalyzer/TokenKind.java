package miniJava.SyntacticAnalyzer;

public enum TokenKind {
	id, 
	kw_class, 
	kw_void, 
	kw_private, 
	kw_public,
	kw_static,
	kw_int,
	kw_boolean,
	kw_this,
	kw_return,
	kw_if,
	kw_else,
	kw_while,
	kw_true,
	kw_false,
	kw_new,
	kw_null,
	num,
	gt, lt, eq, gte, lte, neq,
	and, or, not,
	plus, minus, mult, divide,
	ass, //assignment operator
	o_paren, c_paren, // ( )
	o_brack, c_brack, // [ ]
	o_brace, c_brace, // { }
	dot, comma,
	eol,
	eot
}
