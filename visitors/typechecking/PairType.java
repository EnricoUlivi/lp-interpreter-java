package projectLabo.visitors.typechecking;

import static java.util.Objects.requireNonNull;

public record PairType(Type fstType, Type sndType) implements Type {

	public static final String NAME = PairType.class.getName();

	public PairType {
		requireNonNull(fstType);
		requireNonNull(sndType);
	}

	@Override
	public String toString() {
		return String.format("(%s*%s)", fstType, sndType);
	}

	@Override
	public PairType toPairType() {
		return this;
	}
	
}