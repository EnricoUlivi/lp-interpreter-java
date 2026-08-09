package projectLabo.visitors;

import projectLabo.parser.ast.Block;
import projectLabo.parser.ast.Exp;
import projectLabo.parser.ast.Stmt;
import projectLabo.parser.ast.StmtSeq;
import projectLabo.parser.ast.Variable;

public interface Visitor<T> {

	T visitExpProg(StmtSeq stmtSeq);

	T visitEmptyStmtSeq();

	T visitNonEmptyStmtSeq(Stmt first, StmtSeq rest);

	T visitIfStmt(Exp exp, Block thenBlock, Block elseBlock);

	T visitPrintStmt(Exp exp);

	T visitVarStmt(Variable var, Exp exp);

	T visitBlock(StmtSeq stmtSeq);

	T visitAdd(Exp left, Exp right);

	T visitBoolLiteral(boolean value);

	T visitEq(Exp left, Exp right);

	T visitFst(Exp exp);

	T visitIntLiteral(int value);

	T visitMinus(Exp exp);

	T visitMul(Exp left, Exp right);

	T visitPairLit(Exp left, Exp right);

	T visitSnd(Exp exp);

	T visitVariable(Variable var); // notice the difference with T visitVariable(String name)

	T visitAnd(Exp left, Exp right);

	T visitNot(Exp exp);

	T visitAssertStmt(Exp exp);

	T visitAssignStmt(Variable var, Exp exp);

	// New methods

	T visitVectorLit(Exp exp);

	T visitCat(Exp left, Exp right);

	T visitZip(Exp left, Exp right);

	T visitFlatten(Exp exp);

	T visitForEachStmt(Variable var, Exp exp, Block body);
}