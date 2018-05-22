/**
 * Checker.java
 * Sun Apr 24 15:57:55 AEST 2016
 **/

package VC.Checker;

import java.util.ArrayList;

import VC.ASTs.AST;
import VC.ASTs.Arg;
import VC.ASTs.ArgList;
import VC.ASTs.ArrayExpr;
import VC.ASTs.ArrayType;
import VC.ASTs.AssignExpr;
import VC.ASTs.BinaryExpr;
import VC.ASTs.BooleanExpr;
import VC.ASTs.BooleanLiteral;
import VC.ASTs.BooleanType;
import VC.ASTs.BreakStmt;
import VC.ASTs.CallExpr;
import VC.ASTs.CompoundStmt;
import VC.ASTs.ContinueStmt;
import VC.ASTs.Decl;
import VC.ASTs.DeclList;
import VC.ASTs.EmptyArgList;
import VC.ASTs.EmptyCompStmt;
import VC.ASTs.EmptyDeclList;
import VC.ASTs.EmptyExpr;
import VC.ASTs.EmptyExprList;
import VC.ASTs.EmptyParaList;
import VC.ASTs.EmptyStmt;
import VC.ASTs.EmptyStmtList;
import VC.ASTs.ErrorType;
import VC.ASTs.Expr;
import VC.ASTs.ExprList;
import VC.ASTs.ExprStmt;
import VC.ASTs.FloatExpr;
import VC.ASTs.FloatLiteral;
import VC.ASTs.FloatType;
import VC.ASTs.ForStmt;
import VC.ASTs.FuncDecl;
import VC.ASTs.GlobalVarDecl;
import VC.ASTs.Ident;
import VC.ASTs.IfStmt;
import VC.ASTs.InitExpr;
import VC.ASTs.IntExpr;
import VC.ASTs.IntLiteral;
import VC.ASTs.IntType;
import VC.ASTs.List;
import VC.ASTs.LocalVarDecl;
import VC.ASTs.Operator;
import VC.ASTs.ParaDecl;
import VC.ASTs.ParaList;
import VC.ASTs.Program;
import VC.ASTs.ReturnStmt;
import VC.ASTs.SimpleVar;
import VC.ASTs.StmtList;
import VC.ASTs.StringExpr;
import VC.ASTs.StringLiteral;
import VC.ASTs.StringType;
import VC.ASTs.Type;
import VC.ASTs.UnaryExpr;
import VC.ASTs.VarExpr;
import VC.ASTs.Visitor;
import VC.ASTs.VoidType;
import VC.ASTs.WhileStmt;
import VC.ErrorReporter;
import VC.Scanner.SourcePosition;
import VC.StdEnvironment;

public final class Checker implements Visitor {

    private final static String ERR_MESG[] = {
            "*0: main function is missing",
            "*1: return type of main is not int",

            // defined occurrences of identifiers
            // for global, local and parameters
            "*2: identifier redeclared",
            "*3: identifier declared void",
            "*4: identifier declared void[]",

            // applied occurrences of identifiers
            "*5: identifier undeclared",

            // assignments
            "*6: incompatible type for =",
            "*7: invalid lvalue in assignment",

            // types for expressions
            "*8: incompatible type for return",
            "*9: incompatible type for this binary operator",
            "*10: incompatible type for this unary operator",

            // scalars
            "*11: attempt to use an array/function as a scalar",

            // arrays
            "*12: attempt to use a scalar/function as an array",
            "*13: wrong type for element in array initialiser",
            "*14: invalid initialiser: array initialiser for scalar",
            "*15: invalid initialiser: scalar initialiser for array",
            "*16: excess elements in array initialiser",
            "*17: array subscript is not an integer",
            "*18: array size missing",

            // functions
            "*19: attempt to reference a scalar/array as a function",

            // conditional expressions in if, for and while
            "*20: if conditional is not boolean",
            "*21: for conditional is not boolean",
            "*22: while conditional is not boolean",

            // break and continue
            "*23: break must be in a while/for",
            "*24: continue must be in a while/for",

            // parameters
            "*25: too many actual parameters",
            "*26: too few actual parameters",
            "*27: wrong type for actual parameter",

            // reserved for errors that I may have missed (J. Xue)
            "*28: misc 1",
            "*29: misc 2",

            // the following two checks are optional
            "*30: statement(s) not reached",
            "*31: missing return statement",
    };


    private ErrorReporter reporter;
    private SymbolTable idTable;
    private SourcePosition dummyPos;
    private Ident dummyI;
    private java.util.List<FuncDecl> returnValueFuncs;

    // Checks whether the source program, represented by its AST,
    // satisfies the language's scope rules and type rules.
    // Also decorates the AST as follows:
    //  (1) Each applied occurrence of an identifier is linked to
    //      the corresponding declaration of that identifier.
    //  (2) Each expression and variable is decorated by its type.
    public Checker(ErrorReporter reporter) {
        this.reporter = reporter;
        idTable = new SymbolTable();
        dummyPos = new SourcePosition();
        dummyI = new Ident("x", dummyPos);
        returnValueFuncs = new ArrayList<>();
        establishStdEnvironment();
    }


    // Creates small ASTs to represent "declarations" of all
    // build-in functions.
    // Inserts these "declarations" into the symbol table.
    private void establishStdEnvironment() {

        // Define four primitive types
        // errorType is assigned to ill-typed expressions

        StdEnvironment.booleanType = new BooleanType(dummyPos);
        StdEnvironment.intType = new IntType(dummyPos);
        StdEnvironment.floatType = new FloatType(dummyPos);
        StdEnvironment.stringType = new StringType(dummyPos);
        StdEnvironment.voidType = new VoidType(dummyPos);
        StdEnvironment.errorType = new ErrorType(dummyPos);

        // enter into the declarations for built-in functions into the table

        StdEnvironment.getIntDecl = declareStdFunc(StdEnvironment.intType,
                "getInt", new EmptyParaList(dummyPos));
        StdEnvironment.putIntDecl = declareStdFunc(StdEnvironment.voidType,
                "putInt", new ParaList(
                        new ParaDecl(StdEnvironment.intType, dummyI, dummyPos),
                        new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putIntLnDecl = declareStdFunc(StdEnvironment.voidType,
                "putIntLn", new ParaList(
                        new ParaDecl(StdEnvironment.intType, dummyI, dummyPos),
                        new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.getFloatDecl = declareStdFunc(StdEnvironment.floatType,
                "getFloat", new EmptyParaList(dummyPos));
        StdEnvironment.putFloatDecl = declareStdFunc(StdEnvironment.voidType,
                "putFloat", new ParaList(
                        new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos),
                        new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putFloatLnDecl = declareStdFunc(StdEnvironment.voidType,
                "putFloatLn", new ParaList(
                        new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos),
                        new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putBoolDecl = declareStdFunc(StdEnvironment.voidType,
                "putBool", new ParaList(
                        new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos),
                        new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putBoolLnDecl = declareStdFunc(StdEnvironment.voidType,
                "putBoolLn", new ParaList(
                        new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos),
                        new EmptyParaList(dummyPos), dummyPos));

        StdEnvironment.putStringLnDecl = declareStdFunc(StdEnvironment.voidType,
                "putStringLn", new ParaList(
                        new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos),
                        new EmptyParaList(dummyPos), dummyPos));

        StdEnvironment.putStringDecl = declareStdFunc(StdEnvironment.voidType,
                "putString", new ParaList(
                        new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos),
                        new EmptyParaList(dummyPos), dummyPos));

        StdEnvironment.putLnDecl = declareStdFunc(StdEnvironment.voidType,
                "putLn", new EmptyParaList(dummyPos));

    }

    // Creates a small AST to represent the "declaration" of each built-in
    // function, and enters it in the symbol table.

    private FuncDecl declareStdFunc(Type resultType, String id, List pl) {

        FuncDecl binding;

        binding = new FuncDecl(resultType, new Ident(id, dummyPos), pl,
                new EmptyStmt(dummyPos), dummyPos);
        idTable.insert(id, binding);
        return binding;
    }

    public void check(AST ast) {
        ast.visit(this, null);
    }

    // auxiliary methods
    private void declareVariable(Ident ident, Decl decl) {
        IdEntry entry = idTable.retrieveOneLevel(ident.spelling);
        if (entry == null) {
            ; // no problem
        } else {
            reporter.reportError(ERR_MESG[2] + ": %", ident.spelling, ident.position);
        }
        idTable.insert(ident.spelling, decl);
    }

    private Expr i2f(Expr intExpr) {
        Expr floatExpr = new UnaryExpr(new Operator("i2f", intExpr.position),
                intExpr, intExpr.position);
        floatExpr.type = StdEnvironment.floatType;
        floatExpr.parent = intExpr.parent;
        intExpr.parent = floatExpr;
        return floatExpr;
    }

    private boolean isInWhileOrFor(AST parent) {
        if (parent == null) {
            return false;
        }
        while (true) {
            if (parent instanceof WhileStmt
                    || parent instanceof ForStmt) {
                return true;
            } else {
                parent = parent.parent;
                if (parent == null) {
                    return false;
                }
            }
        }
    }

    @Override
    public Object visitProgram(Program ast, Object o) {
        ast.FL.visit(this, null);
        Decl mainDecl = idTable.retrieve("main");
        if (mainDecl == null
                || !mainDecl.isFuncDecl()) {
            reporter.reportError(ERR_MESG[0], "", ast.position);
        } else if (!mainDecl.T.isIntType()) {
            reporter.reportError(ERR_MESG[1], "", ast.position);
        }
        return null;
    }

    @Override
    public Object visitEmptyDeclList(EmptyDeclList ast, Object o) {
        return null;
    }

    @Override
    public Object visitEmptyStmtList(EmptyStmtList ast, Object o) {
        return null;
    }

    @Override
    public Object visitEmptyExprList(EmptyExprList ast, Object o) {
        return null;
    }

    @Override
    public Object visitEmptyParaList(EmptyParaList ast, Object o) {
        return null;
    }

    @Override
    public Object visitEmptyArgList(EmptyArgList ast, Object o) {
        List argList = (List) o;
        if (!argList.isEmptyParaList()) {
            reporter.reportError(ERR_MESG[26], "", ast.parent.position);
        }
        return null;
    }

    @Override
    public Object visitDeclList(DeclList ast, Object o) {
        ast.D.visit(this, null);
        ast.DL.visit(this, null);
        return null;
    }

    @Override
    public Object visitFuncDecl(FuncDecl ast, Object o) {
        IdEntry currentLevelFuncIdEntry = idTable.retrieveOneLevel(ast.I.spelling);
        if (currentLevelFuncIdEntry != null) {// same name func already exists
            reporter.reportError(ERR_MESG[2] + ": %", currentLevelFuncIdEntry.id, ast.I.position);
        }
        idTable.insert(ast.I.spelling, ast);
        ast.S.visit(this, ast);
        if (!ast.T.isVoidType() && !returnValueFuncs.contains(ast)) {
            if (!"main".equals(ast.I.spelling)) {
                reporter.reportError(ERR_MESG[31], "", ast.position);
            }
        }
        return null;
    }

    @Override
    public Object visitGlobalVarDecl(GlobalVarDecl ast, Object o) {
        declareVariable(ast.I, ast);
        if (ast.T.isVoidType()) {
            reporter.reportError(ERR_MESG[3] + ": %", ast.I.spelling, ast.I.position);
        }
        if (ast.T.isArrayType()) {
            ArrayType arrayType = (ArrayType) ast.T;
            if (arrayType.T.isVoidType()) {
                reporter.reportError(ERR_MESG[4] + ": %", ast.I.spelling, ast.position);
            }
            if (arrayType.E.isEmptyExpr()
                    && !(ast.E instanceof InitExpr)) {
                reporter.reportError(ERR_MESG[18] + ": %", ast.I.spelling, ast.I.position);
            }
        }
        Object object = ast.E.visit(this, ast.T);
        if (ast.T.isArrayType()) {
            if (ast.E instanceof InitExpr) {
                int initLength = (int) object;
                ArrayType arrayType = (ArrayType) (ast.T);
                if (arrayType.E.isEmptyExpr()) {
                    arrayType.E = new IntExpr(new IntLiteral(String.valueOf(initLength), dummyPos), dummyPos);
                } else {
                    int declLenght = Integer.parseInt(((IntExpr) (arrayType.E)).IL.spelling);
                    if (declLenght < initLength) {
                        reporter.reportError(ERR_MESG[16] + ": %", ast.I.spelling, ast.position);
                    }
                }
            } else if (!ast.E.isEmptyExpr()) {
                reporter.reportError(ERR_MESG[15] + ": %", ast.I.spelling, ast.position);
            }
        } else {
            if (ast.T.assignable(ast.E.type)) {
                if (!ast.T.equals(ast.E.type)) {
                    ast.E = i2f(ast.E);
                }
            } else {
                reporter.reportError(ERR_MESG[6], "", ast.position);
            }
        }
        return null;
    }

    @Override
    public Object visitLocalVarDecl(LocalVarDecl ast, Object o) {
        declareVariable(ast.I, ast);
        if (ast.T.isVoidType()) {
            reporter.reportError(ERR_MESG[3] + ": %", ast.I.spelling, ast.I.position);
        }
        if (ast.T.isArrayType()) {
            ArrayType arrayType = (ArrayType) ast.T;
            if (arrayType.T.isVoidType()) {
                reporter.reportError(ERR_MESG[4] + ": %", ast.I.spelling, ast.position);
            }
            if (arrayType.E.isEmptyExpr()
                    && !(ast.E instanceof InitExpr)) {
                reporter.reportError(ERR_MESG[18] + ": %", ast.I.spelling, ast.I.position);
            }
        }
        Object object = ast.E.visit(this, ast.T);
        if (ast.T.isArrayType()) {
            if (ast.E instanceof InitExpr) {
                int initLength = (int) object;
                ArrayType arrayType = (ArrayType) (ast.T);
                if (arrayType.E.isEmptyExpr()) {
                    arrayType.E = new IntExpr(new IntLiteral(String.valueOf(initLength), dummyPos), dummyPos);
                } else {
                    int declLenght = Integer.parseInt(((IntExpr) (arrayType.E)).IL.spelling);
                    if (declLenght < initLength) {
                        reporter.reportError(ERR_MESG[16] + ": %", ast.I.spelling, ast.position);
                    }
                }
            } else if (!ast.E.isEmptyExpr()) {
                reporter.reportError(ERR_MESG[15] + ": %", ast.I.spelling, ast.position);
            }
        } else {
            if (ast.T.assignable(ast.E.type)) {
                if (!ast.T.equals(ast.E.type)) {
                    ast.E = i2f(ast.E);
                }
            } else {
                reporter.reportError(ERR_MESG[6], "", ast.position);
            }
        }
        return null;
    }

    @Override
    public Object visitStmtList(StmtList ast, Object o) {
        ast.S.visit(this, o);
        if (ast.S instanceof ReturnStmt
                && !ast.SL.isEmptyStmtList()) {
            reporter.reportError(ERR_MESG[30], "", ast.SL.position);
        }
        ast.SL.visit(this, o);
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt ast, Object o) {
        Type type = (Type) ast.E.visit(this, null);
        if (type == null
                || !type.isBooleanType()) {
            reporter.reportError(ERR_MESG[20] + " (found: %)", type.toString(), ast.E.position);
        }
        ast.S1.visit(this, o);
        ast.S2.visit(this, o);
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt ast, Object o) {
        Type type = (Type) ast.E.visit(this, null);
        if (type == null
                || !type.isBooleanType()) {
            reporter.reportError(ERR_MESG[22] + " (found: %)", type.toString(), ast.E.position);
        }
        ast.S.visit(this, o);
        return null;
    }

    @Override
    public Object visitForStmt(ForStmt ast, Object o) {
        ast.E1.visit(this, null);
        Type type = (Type) ast.E2.visit(this, null);
        if (type == null
                || (!ast.E2.isEmptyExpr()
                && !type.isBooleanType())) {
            reporter.reportError(ERR_MESG[21] + " (found: %)", type.toString(), ast.E2.position);
        }
        ast.E3.visit(this, null);
        ast.S.visit(this, o);
        return null;
    }

    @Override
    public Object visitBreakStmt(BreakStmt ast, Object o) {
        if (!isInWhileOrFor(ast.parent)) {
            reporter.reportError(ERR_MESG[23], "", ast.position);
        }
        return null;
    }

    @Override
    public Object visitContinueStmt(ContinueStmt ast, Object o) {
        if (!isInWhileOrFor(ast.parent)) {
            reporter.reportError(ERR_MESG[24], "", ast.position);
        }
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt ast, Object o) {
        FuncDecl funcDecl = (FuncDecl) o;
        Type funcType = funcDecl.T;
        Type exprType = (Type) ast.E.visit(this, null);

        if (funcType.isVoidType() ^ ast.E.isEmptyExpr()) {
            reporter.reportError(ERR_MESG[8], "", ast.position);
        }
        if (!funcType.isVoidType()) {
            if (funcType.assignable(exprType)) {
                returnValueFuncs.add(funcDecl);
                if (!funcType.equals(exprType)) {
                    ast.E = i2f(ast.E);
                }
            } else {
                reporter.reportError(ERR_MESG[8], "", ast.position);
            }
        }
        return null;
    }

    @Override
    public Object visitCompoundStmt(CompoundStmt ast, Object o) {
        idTable.openScope();
        if (o instanceof FuncDecl) {
            FuncDecl funcDecl = (FuncDecl) o;
            funcDecl.PL.visit(this, null);
        }
        ast.DL.visit(this, null);
        ast.SL.visit(this, o);
        idTable.closeScope();
        return null;
    }

    @Override
    public Object visitExprStmt(ExprStmt ast, Object o) {
        ast.E.visit(this, o);
        return null;
    }

    @Override
    public Object visitEmptyCompStmt(EmptyCompStmt ast, Object o) {
        return null;
    }

    @Override
    public Object visitEmptyStmt(EmptyStmt ast, Object o) {
        return null;
    }

    @Override
    public Object visitIntExpr(IntExpr ast, Object o) {
        ast.type = StdEnvironment.intType;
        return ast.type;
    }

    @Override
    public Object visitFloatExpr(FloatExpr ast, Object o) {
        ast.type = StdEnvironment.floatType;
        return ast.type;
    }

    @Override
    public Object visitBooleanExpr(BooleanExpr ast, Object o) {
        ast.type = StdEnvironment.booleanType;
        return ast.type;
    }

    @Override
    public Object visitStringExpr(StringExpr ast, Object o) {
        ast.type = StdEnvironment.stringType;
        return ast.type;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr ast, Object o) {
        Type exprType = (Type) ast.E.visit(this, null);
        switch (ast.O.spelling) {
            case "+":
            case "-":
                if (exprType.isIntType() || exprType.isFloatType()) {
                    ast.type = exprType;
                } else {
                    reporter.reportError(ERR_MESG[10] + ": %", ast.O.spelling, ast.position);
                    ast.type = StdEnvironment.errorType;
                }
                break;
            case "!":
                if (exprType.isBooleanType()) {
                    ast.O.spelling += "i";
                    ast.type = exprType;
                } else {
                    reporter.reportError(ERR_MESG[10] + ": %", ast.O.spelling, ast.position);
                    ast.type = StdEnvironment.errorType;
                }
                break;
            default:
        }
        if (ast.type.isFloatType()) {
            ast.O.spelling = "f" + ast.O.spelling;
        } else {
            ast.O.spelling = "i" + ast.O.spelling;
        }
        return ast.type;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr ast, Object o) {
        Type expr1Type = (Type) ast.E1.visit(this, null);
        Type expr2Type = (Type) ast.E2.visit(this, null);
        if (expr1Type.isErrorType() || expr2Type.isErrorType()) {
            ast.type = StdEnvironment.errorType;
            reporter.reportError(ERR_MESG[9] + ": %", ast.O.spelling, ast.position);
        } else if (expr1Type.isFloatType() && expr2Type.isIntType()) {
            ast.type = StdEnvironment.floatType;
            ast.E2 = i2f(ast.E2);
        } else if (expr1Type.isIntType() && expr2Type.isFloatType()) {
            ast.type = StdEnvironment.floatType;
            ast.E1 = i2f(ast.E1);
        } else if (expr1Type.isFloatType() && expr2Type.isFloatType()) {
            ast.type = StdEnvironment.floatType;
        } else if (expr1Type.isIntType() && expr2Type.isIntType()) {
            ast.type = StdEnvironment.intType;
        } else if (expr1Type.isBooleanType() && expr2Type.isBooleanType()) {
            ast.type = StdEnvironment.booleanType;
        } else {
            ast.type = StdEnvironment.errorType;
            reporter.reportError(ERR_MESG[9] + ": %", ast.O.spelling, ast.position);
        }
        int status = 0;//1:convert to float option; 2:convert to int option; 3:error
        if (!ast.type.isErrorType()) {
            switch (ast.O.spelling) {
                case "+":
                case "-":
                case "*":
                case "/":
                    if (ast.type.isFloatType()) {
                        status = 1;
                    } else if (ast.type.isIntType()) {
                        status = 2;
                    } else {
                        status = 3;
                    }
                    break;
                case ">":
                case ">=":
                case "<":
                case "<=":
                    if (ast.type.isFloatType()) {
                        status = 1;
                        ast.type = StdEnvironment.booleanType;
                    } else if (ast.type.isIntType()) {
                        status = 2;
                        ast.type = StdEnvironment.booleanType;
                    } else {
                        status = 3;
                    }
                    break;
                case "&&":
                case "||":
                    if (ast.type.isBooleanType()) {
                        status = 2;
                    } else {
                        status = 3;
                    }
                    break;
                case "==":
                case "!=":
                    if (ast.type.isFloatType()) {
                        status = 1;
                        ast.type = StdEnvironment.booleanType;
                    } else if (ast.type.isIntType() || ast.type.isBooleanType()) {
                        status = 2;
                        ast.type = StdEnvironment.booleanType;
                    } else {
                        status = 3;
                    }
                    break;
                default:
            }
        }
        switch (status) {
            case 1:
                ast.O.spelling += "f";
                break;
            case 2:
                ast.O.spelling += "i";
                break;
            case 3:
                reporter.reportError(ERR_MESG[9] + ": %", ast.O.spelling, ast.position);
                ast.type = StdEnvironment.errorType;

        }
        return ast.type;
    }

    @Override
    public Object visitInitExpr(InitExpr ast, Object o) {
        Type type = (Type) o;
        if (!type.isArrayType()) {
            reporter.reportError(ERR_MESG[14], "", ast.position);
            ast.type = StdEnvironment.errorType;
            return ast.type;
        }
        ast.type = type;
        return ast.IL.visit(this, ((ArrayType) o).T);
    }

    @Override
    public Object visitExprList(ExprList ast, Object o) {
        ast.E.visit(this, null);
        Type type = (Type) o;
        if (type.assignable(ast.E.type)) {
            if (!type.equals(ast.E.type)) {
                ast.E = i2f(ast.E);
            }
        } else {
            reporter.reportError(ERR_MESG[13] + ": at position %", String.valueOf(ast.index), ast.E.position);
        }
        if (ast.EL.isEmpty()) {
            return ast.index + 1;
        } else {
            ((ExprList) ast.EL).index = ast.index + 1;
            return ast.EL.visit(this, o);
        }
    }

    @Override
    public Object visitArrayExpr(ArrayExpr ast, Object o) {
        Type type = (Type) ast.V.visit(this, null);
        if (type.isArrayType()) {
            ast.type = ((ArrayType) type).T;
        } else {
            ast.type = StdEnvironment.errorType;
            reporter.reportError(ERR_MESG[12], "", ast.position);
        }
        type = (Type) ast.E.visit(this, null);
        if (!type.isIntType()) {
            reporter.reportError(ERR_MESG[17], "", ast.position);
        }
        return ast.type;
    }

    @Override
    public Object visitVarExpr(VarExpr ast, Object o) {
        ast.type = (Type) ast.V.visit(this, o);
        return ast.type;
    }

    @Override
    public Object visitCallExpr(CallExpr ast, Object o) {
        Decl decl = idTable.retrieve(ast.I.spelling);
        if (decl == null) {
            reporter.reportError(ERR_MESG[5] + ": %", ast.I.spelling, ast.position);
            ast.type = StdEnvironment.errorType;
        } else if (decl.isFuncDecl()) {
            ast.AL.visit(this, ((FuncDecl) decl).PL);
            ast.type = decl.T;
        } else {
            reporter.reportError(ERR_MESG[19] + ": %", ast.I.spelling, ast.position);
            ast.type = StdEnvironment.errorType;
        }
        return ast.type;
    }

    @Override
    public Object visitAssignExpr(AssignExpr ast, Object o) {
        ast.E1.visit(this, o);
        ast.E2.visit(this, o);
        if (!(ast.E1 instanceof VarExpr || ast.E1 instanceof ArrayExpr)) {
            reporter.reportError(ERR_MESG[7], "", ast.position);
            ast.type = StdEnvironment.errorType;
        } else if (ast.E1 instanceof VarExpr) {
            String id = ((SimpleVar) (((VarExpr) ast.E1).V)).I.spelling;
            Decl decl = idTable.retrieve(id);
            if (decl instanceof FuncDecl) {
                reporter.reportError(ERR_MESG[7] + ": %", id, ast.position);
                ast.type = StdEnvironment.errorType;
            }
        }
        if (ast.E1.type.assignable(ast.E2.type)) {
            if (!ast.E1.type.equals(ast.E2.type)) {
                ast.E2 = i2f(ast.E2);
                ast.type = StdEnvironment.floatType;
            }
            ast.type = ast.E1.type;
        } else {
            reporter.reportError(ERR_MESG[6], "", ast.position);
            ast.type = StdEnvironment.errorType;

        }
        return ast.type;
    }

    @Override
    public Object visitEmptyExpr(EmptyExpr ast, Object o) {
        if (ast.parent instanceof ReturnStmt) {
            ast.type = StdEnvironment.voidType;
        } else {
            ast.type = StdEnvironment.errorType;
        }
        return ast.type;
    }

    @Override
    public Object visitIntLiteral(IntLiteral ast, Object o) {
        return StdEnvironment.intType;
    }

    @Override
    public Object visitFloatLiteral(FloatLiteral ast, Object o) {
        return StdEnvironment.floatType;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral ast, Object o) {
        return StdEnvironment.booleanType;
    }

    @Override
    public Object visitStringLiteral(StringLiteral ast, Object o) {
        return StdEnvironment.stringType;
    }

    @Override
    public Object visitIdent(Ident ast, Object o) {
        Decl decl = idTable.retrieve(ast.spelling);
        if (decl != null) {
            ast.decl = decl;
        }
        return decl;
    }

    @Override
    public Object visitOperator(Operator ast, Object o) {
        return null;
    }

    @Override
    public Object visitParaList(ParaList ast, Object o) {
        ast.P.visit(this, null);
        ast.PL.visit(this, null);
        return null;
    }

    @Override
    public Object visitParaDecl(ParaDecl ast, Object o) {
        declareVariable(ast.I, ast);
        if (ast.T.isVoidType()) {
            reporter.reportError(ERR_MESG[3] + ": %", ast.I.spelling, ast.I.position);
        } else if (ast.T.isArrayType()) {
            if (((ArrayType) ast.T).T.isVoidType()) {
                reporter.reportError(ERR_MESG[4] + ": %", ast.I.spelling, ast.I.position);
            }
        }
        return null;
    }

    @Override
    public Object visitArgList(ArgList ast, Object o) {
        List paraList = (List) o;
        if (paraList.isEmptyParaList()) {
            reporter.reportError(ERR_MESG[25], "", ast.position);
        } else {
            ast.A.visit(this, ((ParaList) o).P);
            ast.AL.visit(this, ((ParaList) o).PL);
        }
        return null;
    }

    @Override
    public Object visitArg(Arg ast, Object o) {
        Decl decl = (Decl) o;
        Type type = (Type) ast.E.visit(this, null);
        boolean isTypeMatched = false;
        if (decl.T.isArrayType()) {
            if (type.isArrayType()) {
                isTypeMatched = ((ArrayType) decl.T).T.assignable(((ArrayType) type).T);
            }
        } else {
            isTypeMatched = decl.T.assignable(type);
        }
        if (!isTypeMatched) {
            reporter.reportError(ERR_MESG[27] + ": %", decl.I.spelling, ast.E.position);
        } else if (isTypeMatched && decl.T.equals(type)) {
            ast.E = i2f(ast.E);
        }
        return null;
    }

    @Override
    public Object visitVoidType(VoidType ast, Object o) {
        return StdEnvironment.voidType;
    }

    @Override
    public Object visitBooleanType(BooleanType ast, Object o) {
        return StdEnvironment.booleanType;
    }

    @Override
    public Object visitIntType(IntType ast, Object o) {
        return StdEnvironment.intType;
    }

    @Override
    public Object visitFloatType(FloatType ast, Object o) {
        return StdEnvironment.floatType;
    }

    @Override
    public Object visitStringType(StringType ast, Object o) {
        return StdEnvironment.stringType;
    }

    @Override
    public Object visitArrayType(ArrayType ast, Object o) {
        return ast;
    }

    @Override
    public Object visitErrorType(ErrorType ast, Object o) {
        return StdEnvironment.errorType;
    }

    @Override
    public Object visitSimpleVar(SimpleVar ast, Object o) {
        Decl decl = idTable.retrieve(ast.I.spelling);
        if (decl == null) {
            ast.type = StdEnvironment.errorType;
            reporter.reportError(ERR_MESG[5] + ": %", ast.I.spelling, ast.position);
        } else if (decl instanceof FuncDecl) {
            ast.type = StdEnvironment.errorType;
            reporter.reportError(ERR_MESG[11] + ": %", ast.I.spelling, ast.position);
        } else {
            ast.type = decl.T;
        }
        if (ast.type.isArrayType()
                && ast.parent instanceof VarExpr
                && !(ast.parent.parent instanceof Arg)) {
            ast.type = StdEnvironment.errorType;
            reporter.reportError(ERR_MESG[11] + ": %", decl.I.spelling, ast.position);

        }
        return ast.type;
    }
}
