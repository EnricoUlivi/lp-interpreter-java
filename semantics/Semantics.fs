module Semantics

(* polymorphic environments, the same definitions work both for static and dynamic environments *)

type variable = Name of string

type 'a scopeLevel = Map<variable, 'a>

type 'a environment = 'a scopeLevel list

exception EnvironmentError of string

let undeclaredVariable var =
    match var with
    | Name str -> raise (EnvironmentError $"undeclared variable {str}")

let redeclaredVariable var =
    match var with
    | Name str -> raise (EnvironmentError $"redeclared variable {str}")

let emptyLevel: 'a scopeLevel = Map.empty
let initialEnv: 'a environment = [ emptyLevel ] (* the empty top-level *)

(* enterLevel: 'a environment -> 'a environment *)

let enterLevel (env: 'a environment) : 'a environment =
    emptyLevel :: env (* enters a new nested level *)

(* exitLevel: 'a environment -> 'a environment *)
(* only used in the dynamic semantics *)

let exitLevel: 'a environment -> 'a environment =
    function (* removes the innermost level, only needed for the dynamic semantics *)
    | _ :: env -> env
    | [] -> failwith "unexpected error" (* should never happen *)

(* variable lookup *)
(* lookup: variable -> environment<'a> -> 'a *)

(* lookup uses Map.tryFind: ('a -> Map<'a,'b> -> 'b option) when 'a: comparison *)

let rec lookup var : 'a environment -> 'a =
    function
    | map :: env ->
        match Map.tryFind var map with
        | Some res -> res
        | None -> lookup var env
    | [] -> undeclaredVariable var

(* variable declaration *)
(* dec: variable -> 'a -> environment<'a> -> environment<'a> *)

(* example:
   dec x ty env1 = env2 means that 'env2' is the new environment after declaring variable 'x' of type 'ty' in the environment 'env1'
   dec x value env1 = env2 means that 'env2' is the new environment after declaring variable 'x' initialized with value 'value' in the environment 'env1'
*)

(* dec uses Map.containsKey: ('a -> Map<'a,'b> -> bool) when 'a: comparison *)

let dec var info : 'a environment -> 'a environment =
    function
    | map :: env ->
        if Map.containsKey var map then
            redeclaredVariable var
        else
            Map.add var info map :: env
    | [] -> failwith "unexpected error" (* should never happen *)

(* variable update *)
(* update: variable -> 'a -> environment<'a> -> environment<'a> *)

(* only used in the dynamic semantics *)

(* update uses Map.containsKey *)

let rec update var info : 'a environment -> 'a environment =
    function
    | map :: env ->
        if Map.containsKey var map then
            Map.add var info map :: env
        else
            map :: update var info env
    | [] -> undeclaredVariable var

(* abstract syntax of the language *)

(* AST of expressions *)
type exp =
    | Add of exp * exp // addition
    | And of exp * exp // logical AND
    | BoolLiteral of bool // boolean literal
    | Eq of exp * exp // equality
    | Fst of exp // first element of a pair
    | IntLiteral of int // integer literal
    | Minus of exp // unary subtraction
    | Mul of exp * exp // multiplication
    | Not of exp // logical NOT
    | PairLit of exp * exp // pair constructor
    | Snd of exp // second element of a pair
    | Variable of variable // variable
    | Vector of exp // singleton vector constructor
    | Cat of exp * exp // vector concatenation
    | Flatten of exp // vector flattening
    | Zip of exp * exp // vector zip

(* AST of statements and sequence of statements, mutually recursive *)
type stmt =
    | AssertStmt of exp // assert statement
    | AssignStmt of variable * exp // assignment
    | IfStmt of exp * block * block // if-then-else
    | PrintStmt of exp // print statement
    | VarStmt of variable * exp // declaration statement
    | ForEachStmt of variable * exp * block // for-each statement

and block = Block of stmtSeq // non-empty block

and stmtSeq =
    | EmptyStmtSeq // empty sequence of statements
    | NonEmptyStmtSeq of stmt * stmtSeq // non-empty sequence of statements

(* AST of programs *)
type prog = ExpProg of stmtSeq // program

(* static semantics of the language *)

(* static types *)

type staticType =
    | Bool
    | Int
    | PairType of staticType * staticType
    | VectorType of staticType * int (* vector type with element type and size *)

(* examples
    PairType(IntType,BoolType) corresponds to int * bool
    PairType(IntType,PairType(IntType,BoolType)) corresponds to int * (int * bool)
    VectorType(IntType,3) corresponds to int[3]
*)

type staticEnv = staticType environment

(* static errors *)

exception TypeError of string

let pairTypeName = "PairType"

let vectorTypeName = "VectorType"

let rec nameOfType =
    function
    | Int -> "INT"
    | Bool -> "BOOL"
    | PairType (type1, type2) -> $"({nameOfType type1}*{nameOfType type2})"
    | VectorType (type1, size) -> $"{nameOfType type1}[{size}]"

let expectingType found expected =
    raise (TypeError $"Found {found}, expected {expected}")

let expectingTypes found expected1 expected2 =
    raise (TypeError $"Found {found}, expected {expected1} or {expected2}")

(* static semantic functions *)


(*
    typecheckExp: staticEnv -> exp -> staticType
    typecheckType: staticType -> staticEnv -> exp -> staticType
    mutually recursive functions, typecheckType auxiliary
*)

(* typecheckExp env exp = ty means that expressions 'exp' is type correct in the environment 'env' and has static type 'ty' *)
(* checkHasType expectedTy env exp = ty means that 'exp' has type 'ty' in 'env' and 'ty'='expectedTy'
   checkIntOrIntVector env exp = ty means that 'exp' has type 'ty' in 'env' and 'ty' is either Int or VectorType(Int, size) for some size
 *)
let rec typecheckExp (env: staticEnv) =
    function
    | Add (left, right) ->
        let type1 = typecheckExp env left

        match type1 with
        | Int
        | VectorType (Int, _) -> checkHasType type1 env right
        | _ -> expectingTypes (nameOfType type1) (nameOfType Int) $"{nameOfType Int}[]"

    | Mul (left, right) ->
        let type1 = typecheckExp env left

        match type1 with
        | Int -> checkHasType Int env right
        | VectorType (Int, size1) ->
            let type2 = typecheckExp env right

            match type2 with
            | VectorType (Int, size2) -> VectorType(VectorType(Int, size1), size2)
            | _ -> expectingType (nameOfType type2) $"{nameOfType Int}[]"
        | _ -> expectingTypes (nameOfType type1) (nameOfType Int) $"{nameOfType Int}[]"
    | And (left, right) ->
        checkHasType Bool env left |> ignore // returned value ignored
        checkHasType Bool env right
    | BoolLiteral _ -> Bool
    | Eq (left, right) ->
        let type1 = typecheckExp env left
        checkHasType type1 env right |> ignore // returned value ignored
        Bool
    | Fst exp ->
        let type1 = typecheckExp env exp

        match type1 with
        | PairType (fstType, _) -> fstType
        | VectorType (PairType (fstType, _), size) -> VectorType(fstType, size)
        | VectorType (elemType, _) -> expectingType $"{nameOfType elemType}[]" $"{pairTypeName}[]"
        | _ -> expectingTypes (nameOfType type1) pairTypeName $"{pairTypeName}[]"
    | IntLiteral _ -> Int
    | Minus exp -> checkHasType Int env exp
    | Not exp -> checkHasType Bool env exp
    | PairLit (left, right) ->
        let type1 = typecheckExp env left
        let type2 = typecheckExp env right
        PairType(type1, type2)
    | Snd exp ->
        let type1 = typecheckExp env exp

        match type1 with
        | PairType (_, sndType) -> sndType
        | VectorType (PairType (_, sndType), size) -> VectorType(sndType, size)
        | VectorType (elemType, _) -> expectingType $"{nameOfType elemType}[]" $"{pairTypeName}[]"
        | _ -> expectingTypes (nameOfType type1) pairTypeName $"{pairTypeName}[]"
    | Variable var -> lookup var env
    | Vector exp -> VectorType(typecheckExp env exp, 1)
    | Cat (left, right) ->
        let elemType1, size1 = getVectorTypeSize env left
        let type2 = typecheckExp env right

        match type2 with
        | VectorType (elemType2, size2) ->
            if elemType2 = elemType1 then
                VectorType(elemType1, size1 + size2)
            else
                expectingType ($"{nameOfType elemType2}[]") $"{nameOfType elemType1}[]"
        | _ -> expectingType $"{nameOfType type2}" vectorTypeName
    | Flatten exp ->
        let type1 = typecheckExp env exp

        match type1 with
        | VectorType (VectorType (elemType, size1), size2) -> VectorType(elemType, size1 * size2)
        | VectorType (elemType, _) -> expectingType ($"{nameOfType elemType}[]") $"{vectorTypeName}[]"
        | _ -> expectingType (nameOfType type1) vectorTypeName
    | Zip (left, right) ->
        let type1, size1 = getVectorTypeSize env left
        let type2, size2 = getVectorTypeSize env right

        if size1 = size2 then
            VectorType(PairType(type1, type2), size1)
        else
            expectingType $"{vectorTypeName}[{size2}]" $"{vectorTypeName}[{size1}]"

and checkHasType expectedTy env exp =
    let foundType = typecheckExp env exp

    if foundType = expectedTy then
        foundType
    else
        expectingType (nameOfType foundType) (nameOfType expectedTy)

and getVectorTypeSize env exp =
    let foundType = typecheckExp env exp

    match foundType with
    | VectorType (type1, size) -> type1, size
    | _ -> expectingType $"{nameOfType foundType}" vectorTypeName

(* mutually recursive functions

 typecheckStmt : staticEnv -> stmt -> staticEnv
 typecheckBlock : staticEnv -> block -> unit
 typecheckStmtSeq : staticEnv -> stmtSeq -> unit

*)

(* typecheckStmt env1 st = env2 means that statement 'st' is type correct in the environment 'env1' and defines the new environment 'env2' *)
(* typecheckBlock env block = () means that the block 'block' is type correct in the environment 'env' *)
(* typecheckStmtSeq env1 stSeq = env2 means that statement sequence 'stSeq' is type correct in the environment 'env1' and defines the new environment 'env2' *)

let rec typecheckStmt (env: staticEnv) =
    function
    | AssertStmt exp ->
        checkHasType Bool env exp |> ignore // returned value ignored
        env
    | AssignStmt (var, exp) ->
        let type1 = lookup var env
        checkHasType type1 env exp |> ignore // returned value ignored
        env
    | IfStmt (exp, thenBlock, elseBlock) ->
        checkHasType Bool env exp |> ignore // returned value ignored
        typecheckBlock env thenBlock
        typecheckBlock env elseBlock
        env
    | PrintStmt exp ->
        typecheckExp env exp |> ignore // returned value ignored
        env
    | VarStmt (var, exp) -> dec var (typecheckExp env exp) env
    | ForEachStmt (var, exp, block) ->
        let type1, _ = getVectorTypeSize env exp
        let forEnv = dec var type1 (enterLevel env)
        typecheckBlock forEnv block
        env

and typecheckBlock env =
    function
    | Block stmtSeq -> typecheckStmtSeq (enterLevel env) stmtSeq

and typecheckStmtSeq (env: staticEnv) =
    function
    | EmptyStmtSeq -> ()
    | NonEmptyStmtSeq (stmt, stmtSeq) -> typecheckStmtSeq (typecheckStmt env stmt) stmtSeq

(*
  typecheckProg : prog -> unit
*)

(* typecheckProg p = () means that program 'p' is well defined with respect to the static semantics *)

let typecheckProg =
    function
    | ExpProg stmtSeq ->
        typecheckStmtSeq initialEnv stmtSeq
        ()

(* dynamic semantics of the language *)

(* values *)

type value =
    | IntValue of int
    | BoolValue of bool
    | PairValue of value * value
    | VectorValue of value list

(* examples
    PairLit(IntLiteral 2,BoolLiteral false) corresponds to  2,false
    PairLit(IntLiteral 2,PairLit(IntLiteral 3,BoolLiteral true)) corresponds to 2,(3,true)
*)

type dynamicEnv = value environment

type output = string

(* dynamic errors *)

exception DynamicTypeError of string (* dynamic conversion error *)

let expectingDynamicType found expected =
    raise (DynamicTypeError $"Found {found}, expected {expected}")

let expectingDynamicTypes found expected1 expected2 =
    raise (DynamicTypeError $"Found {found}, expected {expected1} or {expected2}")

let intValueName = "IntValue"
let boolValueName = "BoolValue"
let pairValueName = "PairValue"
let vectorValueName = "VectorValue"

let vectorValueOf typeName = $"{vectorValueName}<{typeName}>"

let rec nameOfValue =
    function
    | IntValue _ -> intValueName
    | BoolValue _ -> boolValueName
    | PairValue _ -> pairValueName
    | VectorValue _ -> vectorValueName

(* auxiliary functions *)

(* dynamic conversion to int type *)
(* toInt : value -> int *)

let toInt value =
    match value with
    | IntValue i -> i
    | _ -> expectingDynamicType (nameOfValue value) intValueName

(* dynamic conversion to bool type *)
(* toBool : value -> bool *)

let toBool value =
    match value with
    | BoolValue b -> b
    | _ -> expectingDynamicType (nameOfValue value) boolValueName

(* toPair : value -> value * value *)
(* dynamic conversion to product  type *)

let toPair value =
    match value with
    | PairValue (e1, e2) -> e1, e2
    | _ -> expectingDynamicType (nameOfValue value) pairValueName

(* toVector : value -> value list *)
(* dynamic conversion to vector type *)

let toVector value =
    match value with
    | VectorValue lst -> lst
    | _ -> expectingDynamicType (nameOfValue value) vectorValueName

(* fst and snd operators *)
(* fst: 'a * 'b -> 'a  and snd: 'a * 'b -> 'b predefined in F# *)

(* conversion to string *)

(* toString : value -> string *)

let rec toString =
    function (* uses interpolated strings *)
    | IntValue i -> $"{i}"
    | BoolValue b -> if b then "true" else "false" // no interpolated string here, to avoid capitalization of the first letter
    | PairValue (v1, v2) -> $"({toString v1},{toString v2})"
    | VectorValue lst -> $"{vectorValueName}[{List.length lst}]"

(* auxiliary functions on vectors *)
let checkSameSize list1 list2 =
    if List.length list1 <> List.length list2 then
        raise (DynamicTypeError "vectors must have the same size")
    else
        ()

(* vectorAddition : Value list -> Value list -> Value list*)
(* vectorAddition l1 l2 computes a list of IntValue by component-wise addition
   raises an error if the two lists have different lengths or if some element is not an IntValue *)
let vectorAddition list1 list2 =
    checkSameSize list1 list2
    List.map2 (fun x y -> toInt x + toInt y |> IntValue) list1 list2

(* scalarMultiplication : Value -> Value list -> Value list *)
(* scalarMultiplication s l computes a list of IntValue by component-wise multiplication with s
   raises an error if s or some element of l is not an IntValue *)
let scalarMultiplication scalar =
    let s = toInt scalar
    List.map (fun x -> s * toInt x |> IntValue)

(* outerProduct : Value list -> Value list -> Value list *)
(* outerProduct computes the outer product of two IntValue vectors,
   by storing the resulting matrix  in column-major order
   example:
   outerProduct [IntValue 1;IntValue 2;IntValue 3;IntValue 4] [IntValue 2;IntValue 1;IntValue 0] =
        [
         VectorValue [IntValue 2; IntValue 4; IntValue 6; IntValue 8]; // column 1
         VectorValue [IntValue 1; IntValue 2; IntValue 3; IntValue 4]; // column 2
         VectorValue [IntValue 0; IntValue 0; IntValue 0; IntValue 0]  // column 3
        ]
    raise an error if some element is not an IntValue
   *)
let outerProduct list =
    List.map (fun x -> scalarMultiplication x list |> VectorValue)

let fstVector list =
    List.map (fun x -> x |> toPair |> fst) list
    |> VectorValue

let sndVector list =
    List.map (fun x -> x |> toPair |> snd) list
    |> VectorValue

let catVector list1 list2 = list1 @ list2 |> VectorValue

(* evalExp : dynamicEnv -> exp -> value *)
(* evalExp env exp = val means that expressions 'exp' successfully evaluates to 'val' in the environment 'env' *)

let flattenVector list =
    List.fold (fun acc el -> acc @ toVector el) [] list
    |> VectorValue

let zipVector list1 list2 =
    checkSameSize list1 list2

    List.map2 (fun x y -> PairValue(x, y)) list1 list2
    |> VectorValue

(* semantic functions *)
let rec evalExp (env: dynamicEnv) =
    function
    | Add (left, right) ->
        let val1 = evalExp env left

        match val1 with
        | IntValue int1 -> int1 + (evalExp env right |> toInt) |> IntValue
        | VectorValue list1 ->
            vectorAddition list1 (evalExp env right |> toVector)
            |> VectorValue
        | _ -> expectingDynamicTypes (nameOfValue val1) intValueName (vectorValueOf intValueName)
    | Mul (left, right) ->
        let val1 = evalExp env left

        match val1 with
        | IntValue int1 -> int1 * (evalExp env right |> toInt) |> IntValue
        | VectorValue list1 ->
            outerProduct list1 (evalExp env right |> toVector)
            |> VectorValue
        | _ -> expectingDynamicTypes (nameOfValue val1) intValueName (vectorValueOf intValueName)
    | And (left, right) ->
        (evalExp env left |> toBool
         && evalExp env right |> toBool)
        |> BoolValue
    | BoolLiteral b -> BoolValue b
    | Eq (left, right) -> evalExp env left = evalExp env right |> BoolValue
    | Fst exp ->
        let value = evalExp env exp

        match value with
        | PairValue (fstValue, _) -> fstValue
        | VectorValue list -> fstVector list
        | _ -> expectingDynamicTypes (nameOfValue value) pairValueName (vectorValueOf pairValueName)
    | IntLiteral i -> IntValue i
    | Minus exp -> evalExp env exp |> toInt |> (~-) |> IntValue // (~-) is the unary minus
    | Not exp -> evalExp env exp |> toBool |> not |> BoolValue
    | PairLit (left, right) -> (evalExp env left, evalExp env right) |> PairValue
    | Snd exp ->
        let value = evalExp env exp

        match value with
        | PairValue (_, sndValue) -> sndValue
        | VectorValue list -> sndVector list
        | _ -> expectingDynamicTypes (nameOfValue value) pairValueName (vectorValueOf pairValueName)
    | Variable var -> lookup var env
    | Vector exp -> VectorValue [ evalExp env exp ]
    | Cat (left, right) -> catVector (evalExp env left |> toVector) (evalExp env right |> toVector)
    | Flatten exp -> flattenVector (evalExp env exp |> toVector)
    | Zip (left, right) -> zipVector (evalExp env left |> toVector) (evalExp env right |> toVector)

(* mutually recursive
   executeStmt : dynamicEnv * output -> stmt -> dynamicEnv * output
   executeBlock : dynamicEnv * output -> block -> dynamicEnv * output
   executeStmtSeq : dynamicEnv * output -> stmtSeq -> dynamicEnv * output
*)

(* executeStmt (env1,out1) 'stmt' = (env2,out2) means that the execution of statement 'stmt' in environment 'env1' and with output 'out1' successfully returns the new environment 'env2' and output 'out2' *)
(* executeBlock (env1,out1) block = (env2,out2) means that the execution of block 'block' in environment 'env1' and with output 'out1' successfully returns the new environment 'env2' and output 'out2' *)
(* executeStmtSeq (env1,out1) stmtSeq = (env2,out2) means that the execution of sequence 'stmtSeq' in environment 'env1' and with output 'out1' successfully returns the new environment 'env2' and output 'out2' *)
(* executeStmt, executeBlock and executeStmtSeq write on the standard output if some 'print' statement is executed *)

let rec executeStmt (env, out) : stmt -> dynamicEnv * output =
    function
    | AssignStmt (var, exp) -> update var (evalExp env exp) env, out
    | AssertStmt exp ->
        assert (toBool (evalExp env exp))
        env, out
    | IfStmt (exp, thenBlock, elseBlock) ->
        if toBool (evalExp env exp) then
            executeBlock (env, out) thenBlock
        else
            executeBlock (env, out) elseBlock
    | PrintStmt exp -> env, out + (evalExp env exp |> toString) + "\n"
    | VarStmt (var, exp) -> dec var (evalExp env exp) env, out
    | ForEachStmt (var, exp, block) ->
        let forEnv = dec var (IntValue 0) (enterLevel env)

        let env2, out2 =
            List.fold
                (fun (env1, out1) el -> executeBlock (update var el env1, out1) block)
                (forEnv, out)
                (evalExp env exp |> toVector)

        exitLevel env2, out2

and executeBlock (env1, out1) =
    function (* note the differences with the static semantics *)
    | Block stmtSeq ->
        let env2, out2 = executeStmtSeq (enterLevel env1, out1) stmtSeq
        exitLevel env2, out2

and executeStmtSeq (envOutPair: dynamicEnv * output) : stmtSeq -> dynamicEnv * output =
    function
    | EmptyStmtSeq -> envOutPair
    | NonEmptyStmtSeq (stmt, stmtSeq) -> executeStmtSeq (executeStmt envOutPair stmt) stmtSeq

(* executeProg : prog -> output *)
(* executeProg prog = out means that program 'prog' has been executed successfully with output 'out' *)

let executeProg =
    function
    | ExpProg stmtSeq -> snd (executeStmtSeq (initialEnv, "") stmtSeq)
