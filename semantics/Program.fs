open Semantics

(* test success/prog08.txt *)

let prog08 =
    ExpProg(
        NonEmptyStmtSeq(
            VarStmt(Name "i", IntLiteral 1),
            NonEmptyStmtSeq(
                ForEachStmt(
                    Name "x",
                    Cat(Cat(Vector(IntLiteral 1), Vector(IntLiteral 2)), Vector(IntLiteral 3)),
                    Block(
                        NonEmptyStmtSeq(
                            AssertStmt(Eq(Variable(Name "x"), Variable(Name "i"))),
                            NonEmptyStmtSeq(AssignStmt(Name "i", Add(Variable(Name "i"), IntLiteral 1)), EmptyStmtSeq)
                        )
                    )
                ),
                EmptyStmtSeq
            )
        )
    )

(* con typechecking *)

printf
    "%s"
    (try
        typecheckProg prog08
        executeProg prog08
     with
     | EnvironmentError msg
     | TypeError msg -> "Static error: " + msg)

(* test failure/static-semantics/prog09.txt *)

let prog09 =
    ExpProg(
        NonEmptyStmtSeq(
            ForEachStmt(
                Name "x",
                PairLit(IntLiteral 1, IntLiteral 2),
                Block(NonEmptyStmtSeq(PrintStmt(Variable(Name "x")), EmptyStmtSeq))
            ),
            EmptyStmtSeq
        )
    )

(* con typechecking *)

printfn
    "%s"
    (try
        typecheckProg prog09
        executeProg prog09
     with
     | EnvironmentError msg
     | TypeError msg -> "Static error: " + msg)


(* senza typechecking *)

printfn
    "%s"
    (try
        executeProg prog09
     with
     | EnvironmentError msg
     | DynamicTypeError msg -> "Dynamic error: " + msg)
