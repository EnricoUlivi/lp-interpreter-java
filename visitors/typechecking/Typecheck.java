package projectLabo.visitors.typechecking;

import static projectLabo.visitors.typechecking.AtomicType.*;

import projectLabo.visitors.environments.EnvironmentException;
import projectLabo.parser.ast.Block;
import projectLabo.parser.ast.Exp;
import projectLabo.parser.ast.Stmt;
import projectLabo.parser.ast.StmtSeq;
import projectLabo.parser.ast.Variable;
import projectLabo.visitors.Visitor;

public class Typecheck implements Visitor<Type> {

	private final StaticEnv env = new StaticEnv();

	// useful to typecheck binary operations where operands must have the same type
	private void checkBinOp(Exp left, Exp right, Type type) {
		type.checkEqual(left.accept(this));
		type.checkEqual(right.accept(this));
	}

	// static semantics for programs; no value returned by the visitor

	@Override
	public Type visitExpProg(StmtSeq stmtSeq) {
		try {
			stmtSeq.accept(this);
		} catch (EnvironmentException e) { // undeclared variable
			throw new TypecheckerException(e);
		}
		return null;
	}

	// static semantics for sequences of statements
	// no value returned by the visitor

	@Override
	public Type visitEmptyStmtSeq() {
		return null;
	}

	@Override
	public Type visitNonEmptyStmtSeq(Stmt first, StmtSeq rest) {
		first.accept(this);
		rest.accept(this);
		return null;
	}

	// static semantics for statements; no value returned by the visitor

	@Override
	public Type visitIfStmt(Exp exp, Block thenBlock, Block elseBlock) {
		BOOL.checkEqual(exp.accept(this));
		thenBlock.accept(this);
		if (elseBlock != null)
			elseBlock.accept(this);
		return null;
	}

	@Override
	public Type visitPrintStmt(Exp exp) {
		exp.accept(this);
		return null;
	}

	@Override
	public Type visitVarStmt(Variable var, Exp exp) {
		env.dec(var, exp.accept(this));
		return null;
	}

	@Override
	public Type visitBlock(StmtSeq stmtSeq) {
		env.enterLevel();
		stmtSeq.accept(this);
		env.exitLevel();
		return null;
	}

	@Override
	public Type visitAssertStmt(Exp exp) {
		BOOL.checkEqual(exp.accept(this));
		return null;
	}

	@Override
	public Type visitAssignStmt(Variable var, Exp exp) {
		env.lookup(var).checkEqual(exp.accept(this));
		return null;
	}

	@Override
	public Type visitForEachStmt(Variable var, Exp exp, Block body) {
		VectorType vecType = exp.accept(this).toVectorType();
		env.enterLevel();
		env.dec(var, vecType.elemType());
		body.accept(this);
		env.exitLevel();
		return null;
	}

	// static semantics of expressions; a type is returned by the visitor

	@Override
	public Type visitAdd(Exp left, Exp right) {
		Type leftType = left.accept(this);
		if (leftType instanceof VectorType leftVec && leftVec.elemType().equals(INT)) {
			leftVec.checkEqual(right.accept(this));
			return leftVec;
			
		} else if (leftType.equals(INT)) {
			INT.checkEqual(right.accept(this));
			return INT;
		} else {
			throw new TypecheckerException(leftType.toString(), INT.toString(), INT.toString() + "[]");
		}
	}

	@Override
	public AtomicType visitBoolLiteral(boolean value) {
		return BOOL;
	}

	@Override
	public AtomicType visitEq(Exp left, Exp right) {
		left.accept(this).checkEqual(right.accept(this));
		return BOOL;
	}

	@Override
	public Type visitFst(Exp exp) {
		Type type = exp.accept(this);
		if (type instanceof VectorType vecType) {
			Type elemType = vecType.elemType();
			if (!(elemType instanceof PairType pairType))
				throw new TypecheckerException(elemType.toString() + "[]", "PairType[]");
			return new VectorType(pairType.fstType(), vecType.size());
		} else {
			return type.toPairType().fstType();
		}
	}

	@Override
	public AtomicType visitIntLiteral(int value) {
		return INT;
	}

	@Override
	public AtomicType visitMinus(Exp exp) {
		INT.checkEqual(exp.accept(this));
		return INT;
	}

	@Override
	public Type visitMul(Exp left, Exp right) {
		Type leftType = left.accept(this);
		if (leftType instanceof VectorType leftVec) {
			INT.checkEqual(leftVec.elemType());
			Type rightType = right.accept(this);
			if (!(rightType instanceof VectorType rightVec))
				throw new TypecheckerException(rightType.toString(), INT.toString() + "[]");
			INT.checkEqual(rightVec.elemType());
			VectorType innerVec = new VectorType(INT, leftVec.size());
			return new VectorType(innerVec, rightVec.size());
		} else if (leftType.equals(INT)) {
			INT.checkEqual(right.accept(this));
			return INT;
		} else {
			throw new TypecheckerException(leftType.toString(), INT.toString(), INT.toString() + "[]");
		}
	}

	@Override
	public PairType visitPairLit(Exp left, Exp right) {
		return new PairType(left.accept(this), right.accept(this));
	}

	@Override
	public Type visitSnd(Exp exp) {
		Type type = exp.accept(this);
		if (type instanceof VectorType vecType) {
			Type elemType = vecType.elemType();
			if (!(elemType instanceof PairType pairType))
				throw new TypecheckerException(elemType.toString() + "[]", "PairType[]");
			return new VectorType(pairType.sndType(), vecType.size());
		} else {
			return type.toPairType().sndType();
		}
	}

	@Override
	public Type visitVariable(Variable var) {
		return env.lookup(var);
	}

	@Override
	public AtomicType visitAnd(Exp left, Exp right) {
		checkBinOp(left, right, BOOL);
		return BOOL;
	}

	@Override
	public AtomicType visitNot(Exp exp) {
		BOOL.checkEqual(exp.accept(this));
		return BOOL;
	}

	@Override
	public VectorType visitVectorLit(Exp exp) {
		return new VectorType(exp.accept(this), 1);
	}

	@Override
	public VectorType visitCat(Exp left, Exp right) {
		VectorType leftVec = left.accept(this).toVectorType();
		VectorType rightVec = right.accept(this).toVectorType();
		if (!leftVec.elemType().equals(rightVec.elemType()))
			throw new TypecheckerException(rightVec.elemType().toString() + "[]", leftVec.elemType().toString() + "[]");
		return new VectorType(leftVec.elemType(), leftVec.size() + rightVec.size());
	}

	@Override
	public VectorType visitZip(Exp left, Exp right) {
		VectorType leftVec = left.accept(this).toVectorType();
		VectorType rightVec = right.accept(this).toVectorType();
		if (leftVec.size() != rightVec.size()) {
			throw new TypecheckerException(
				"VectorType[" + rightVec.size() + "]",
				"VectorType[" + leftVec.size() + "]"
			);
		}
		PairType pairType = new PairType(leftVec.elemType(), rightVec.elemType());
		return new VectorType(pairType, leftVec.size());
	}

	@Override
	public VectorType visitFlatten(Exp exp) {
		VectorType outerVec = exp.accept(this).toVectorType();
		Type elemType = outerVec.elemType();
		if (!(elemType instanceof VectorType innerVec))
			throw new TypecheckerException(elemType.toString() + "[]", "VectorType[]");
		return new VectorType(innerVec.elemType(), outerVec.size() * innerVec.size());
	}

	public Type inter(Exp vector1, Exp vector2){
		VectorType vec1 = vector1.accept(this).toVectorType();
		VectorType vec2 = vector2.accept(this).toVectorType();
		vec1.checkEqual(vec2);
		return new VectorType(vec1.elemType(), vec1.size() * 2);
	}
	
}