package projectLabo.visitors.execution;

import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;

import projectLabo.visitors.environments.EnvironmentException;
import projectLabo.parser.ast.Block;
import projectLabo.parser.ast.Exp;
import projectLabo.parser.ast.Stmt;
import projectLabo.parser.ast.StmtSeq;
import projectLabo.parser.ast.Variable;
import projectLabo.visitors.Visitor;

import static java.util.Objects.requireNonNull;

public class Execute implements Visitor<Value> {

	private final DynamicEnv env = new DynamicEnv();
	private final PrintWriter printWriter; // output stream used to print values

	public Execute() {
		printWriter = new PrintWriter(System.out, true);
	}

	public Execute(PrintWriter printWriter) {
		this.printWriter = requireNonNull(printWriter);
	}

	// dynamic semantics for programs; no value returned by the visitor

	@Override
	public Value visitExpProg(StmtSeq stmtSeq) {
		try {
			stmtSeq.accept(this);
			// possible runtime errors
			// EnvironmentException: undefined variable
		} catch (EnvironmentException e) {
			throw new InterpreterException(e);
		}
		return null;
	}

	// dynamic semantics for sequences of statements
	// no value returned by the visitor

	@Override
	public Value visitEmptyStmtSeq() {
		return null;
	}

	@Override
	public Value visitNonEmptyStmtSeq(Stmt first, StmtSeq rest) {
		first.accept(this);
		rest.accept(this);
		return null;
	}

	// dynamic semantics for statements; no value returned by the visitor

	@Override
	public Value visitIfStmt(Exp exp, Block thenBlock, Block elseBlock) {
		if (exp.accept(this).toBool())
			thenBlock.accept(this);
		else if (elseBlock != null)
			elseBlock.accept(this);
		return null;
	}

	@Override
	public Value visitPrintStmt(Exp exp) {
		printWriter.println(exp.accept(this));
		return null;
	}

	@Override
	public Value visitVarStmt(Variable var, Exp exp) {
		env.dec(var, exp.accept(this));
		return null;
	}

	@Override
	public Value visitBlock(StmtSeq stmtSeq) {
		env.enterLevel();
		stmtSeq.accept(this);
		env.exitLevel();
		return null;
	}

	@Override
	public Value visitAssertStmt(Exp exp) {
		if (!exp.accept(this).toBool())
			throw new InterpreterException(new AssertionError());
		return null;
	}

	@Override
	public Value visitAssignStmt(Variable var, Exp exp) {
		env.update(var, exp.accept(this));
		return null;
	}

	@Override
	public Value visitForEachStmt(Variable var, Exp exp, Block body) {
		VectorValue vec = exp.accept(this).toVector();
		env.enterLevel();                       
		env.dec(var, new IntValue(0));  
		for (Value v : vec.elements()) {
			env.update(var, v);               
			body.accept(this);              
		}
		env.exitLevel();                   
		return null;
	}

	// dynamic semantics of expressions; a value is returned by the visitor

	@Override
	public Value visitAdd(Exp left, Exp right) {
		Value leftVal = left.accept(this);
		if (leftVal instanceof VectorValue leftVec) {
			VectorValue rightVec = right.accept(this).toVector();
			if (leftVec.elements().size() != rightVec.elements().size())
				throw new InterpreterException("vectors must have the same size");
			List<Value> res = new ArrayList<>();
			for (int i = 0; i < leftVec.elements().size(); i++) {
				int sum = leftVec.elements().get(i).toInt() + rightVec.elements().get(i).toInt();
				res.add(new IntValue(sum));
			}
			return new VectorValue(res);
		} else if (leftVal instanceof IntValue leftInt) {
			return new IntValue(leftInt.toInt() + right.accept(this).toInt());
		} else {
			throw new InterpreterException(leftVal.getClass().getSimpleName(), "IntValue", "VectorValue<IntValue>");
		}
	}

	@Override
	public BoolValue visitBoolLiteral(boolean value) {
		return new BoolValue(value);
	}

	@Override
	public BoolValue visitEq(Exp left, Exp right) {
		return new BoolValue(left.accept(this).equals(right.accept(this)));
	}

	@Override
	public Value visitFst(Exp exp) {
		Value val = exp.accept(this);
		if (val instanceof VectorValue vec) {
			List<Value> res = new ArrayList<>();
			for (Value v : vec.elements())
				res.add(v.toPair().fstVal());
			return new VectorValue(res);
		} else if (val instanceof PairValue pair) {
			return pair.fstVal();
		} else {
			throw new InterpreterException(val.getClass().getSimpleName(), "PairValue", "VectorValue<PairValue>");
		}
	}

	@Override
	public IntValue visitIntLiteral(int value) {
		return new IntValue(value);
	}

	@Override
	public IntValue visitMinus(Exp exp) {
		return new IntValue(-exp.accept(this).toInt());
	}

	@Override
	public Value visitMul(Exp left, Exp right) {
		Value leftVal = left.accept(this);
		if (leftVal instanceof VectorValue leftVec) {
			VectorValue rightVec = right.accept(this).toVector();
			List<Value> columns = new ArrayList<>();
			for (Value rVal : rightVec.elements()) {
				List<Value> columnElements = new ArrayList<>();
				for (Value lVal : leftVec.elements()) {
					int prod = lVal.toInt() * rVal.toInt();
					columnElements.add(new IntValue(prod));
				}
				columns.add(new VectorValue(columnElements));
			}
			return new VectorValue(columns);
		} else if (leftVal instanceof IntValue leftInt) {
			return new IntValue(leftInt.toInt() * right.accept(this).toInt());
		} else {
			throw new InterpreterException(leftVal.getClass().getSimpleName(), "IntValue", "VectorValue<IntValue>");
		}
	}

	@Override
	public PairValue visitPairLit(Exp left, Exp right) {
		return new PairValue(left.accept(this), right.accept(this));
	}

	@Override
	public Value visitSnd(Exp exp) {
		Value val = exp.accept(this);
		if (val instanceof VectorValue vec) {
			List<Value> res = new ArrayList<>();
			for (Value v : vec.elements())
				res.add(v.toPair().sndVal());
			return new VectorValue(res);
		} else if (val instanceof PairValue pair) {
			return pair.sndVal();
		} else {
			throw new InterpreterException(val.getClass().getSimpleName(), "PairValue", "VectorValue<PairValue>");
		}
	}

	@Override
	public Value visitVariable(Variable var) {
		return env.lookup(var);
	}

	@Override
	public Value visitAnd(Exp left, Exp right) {
		return new BoolValue(left.accept(this).toBool() && right.accept(this).toBool());
	}

	@Override
	public Value visitNot(Exp exp) {
		return new BoolValue(!exp.accept(this).toBool());
	}

	@Override
	public VectorValue visitVectorLit(Exp exp) {
		List<Value> elements = new ArrayList<>();
		elements.add(exp.accept(this));
		return new VectorValue(elements);
	}

	@Override
	public VectorValue visitCat(Exp left, Exp right) {
		VectorValue leftVec = left.accept(this).toVector();
		VectorValue rightVec = right.accept(this).toVector();
		List<Value> res = new ArrayList<>(leftVec.elements());
		res.addAll(rightVec.elements());
		return new VectorValue(res);
	}

	@Override
	public VectorValue visitZip(Exp left, Exp right) {
		VectorValue leftVec = left.accept(this).toVector();
		VectorValue rightVec = right.accept(this).toVector();
		if (leftVec.elements().size() != rightVec.elements().size())
			throw new InterpreterException("vectors must have the same size");
		List<Value> res = new ArrayList<>();
		for (int i = 0; i < leftVec.elements().size(); i++) {
			res.add(new PairValue(leftVec.elements().get(i), rightVec.elements().get(i)));
		}
		return new VectorValue(res);
	}

	@Override
	public VectorValue visitFlatten(Exp exp) {
		VectorValue outerVec = exp.accept(this).toVector();
		List<Value> res = new ArrayList<>();
		for (Value innerVal : outerVec.elements()) {
			res.addAll(innerVal.toVector().elements());
		}
		return new VectorValue(res);
	}

	public Value inter(Exp vector1, Exp vector2){
		VectorValue vec1 = vector1.accept(this).toVector();
		VectorValue vec2 = vector2.accept(this).toVector();
		if(vec1.elements().size() != vec2.elements().size()){
			throw new InterpreterException("size diversa");
		}
		List<Value> res = new ArrayList<>();
		for(int i = 0; i < vec1.elements().size() * 2; i++){
			if(i % 2 == 0){
				res.add(vec1.elements().get(i/2));
			} else {
				res.add(vec2.elements().get((i/2)));
			}
		}
		return new VectorValue(res);
	}

}