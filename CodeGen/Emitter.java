/*
 * Emitter.java    15 -|- MAY -|- 2016
 * Jingling Xue, School of Computer Science, UNSW, Australia
 */

// A new frame object is created for every function just before the
// function is being translated in visitFuncDecl.
//
// All the information about the translation of a function should be
// placed in this Frame object and passed across the AST nodes as the
// 2nd argument of every visitor method in Emitter.java.

package VC.CodeGen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import VC.ASTs.*;
import VC.ErrorReporter;
import VC.StdEnvironment;

public final class Emitter implements Visitor {

    private ErrorReporter errorReporter;
    private String inputFilename;
    private String classname;
    private String outputFilename;
    private boolean isReturnMain;
    private Map<String, String> arithmeticOp;
    private Set<String> cmpOp;

    public Emitter(String inputFilename, ErrorReporter reporter) {
        this.inputFilename = inputFilename;
        errorReporter = reporter;

        int i = inputFilename.lastIndexOf('.');
        if (i > 0) {
            classname = inputFilename.substring(0, i);
        } else {
            classname = inputFilename;
        }
        isReturnMain = false;
        arithmeticOp = new HashMap<>();
        cmpOp = new HashSet<>();
        initOps();
    }

    private void initOps() {
        arithmeticOp.put("i+", JVM.IADD);
        arithmeticOp.put("i-", JVM.ISUB);
        arithmeticOp.put("i*", JVM.IMUL);
        arithmeticOp.put("i/", JVM.IDIV);
        arithmeticOp.put("f+", JVM.FADD);
        arithmeticOp.put("f-", JVM.FSUB);
        arithmeticOp.put("f*", JVM.FMUL);
        arithmeticOp.put("f/", JVM.FDIV);

        cmpOp.add("i>");
        cmpOp.add("i>=");
        cmpOp.add("i<");
        cmpOp.add("i<=");
        cmpOp.add("i!=");
        cmpOp.add("i==");
        cmpOp.add("f>");
        cmpOp.add("f>=");
        cmpOp.add("f<");
        cmpOp.add("f<=");
        cmpOp.add("f!=");
        cmpOp.add("f==");
    }

    // PRE: ast must be a Program node

    public final void gen(AST ast) {
        ast.visit(this, null);
        JVM.dump(classname + ".j");
    }


    // Auxiliary methods for byte code generation

    // The following method appends an instruction directly into the JVM
    // Code Store. It is called by all other overloaded emit methods.

    private void emit(String s) {
        JVM.append(new Instruction(s));
    }

    private void emit(String s1, String s2) {
        emit(s1 + " " + s2);
    }

    private void emit(String s1, int i) {
        emit(s1 + " " + i);
    }

    private void emit(String s1, float f) {
        emit(s1 + " " + f);
    }

    private void emit(String s1, String s2, int i) {
        emit(s1 + " " + s2 + " " + i);
    }

    private void emit(String s1, String s2, String s3) {
        emit(s1 + " " + s2 + " " + s3);
    }

    private void emitIF_ICMPCOND(String op, Frame frame) {
        String opcode;
        switch (op) {
            case "i!=":
                opcode = JVM.IF_ICMPNE;
                break;
            case "i==":
                opcode = JVM.IF_ICMPEQ;
                break;
            case "i<":
                opcode = JVM.IF_ICMPLT;
                break;
            case "i<=":
                opcode = JVM.IF_ICMPLE;
                break;
            case "i>":
                opcode = JVM.IF_ICMPGT;
                break;
            default://"i>="
                opcode = JVM.IF_ICMPGE;
        }

        String falseLabel = frame.getNewLabel();
        String nextLabel = frame.getNewLabel();

        emit(opcode, falseLabel);
        frame.pop(2);
        emit("iconst_0");
        emit("goto", nextLabel);
        emit(falseLabel + ":");
        emit(JVM.ICONST_1);
        frame.push();
        emit(nextLabel + ":");
    }

    private void emitFCMP(String op, Frame frame) {
        String opcode;
        switch (op) {
            case "f!=":
                opcode = JVM.IFNE;
                break;
            case "f==":
                opcode = JVM.IFEQ;
                break;
            case "f<":
                opcode = JVM.IFLT;
                break;
            case "f<=":
                opcode = JVM.IFLE;
                break;
            case "f>":
                opcode = JVM.IFGT;
                break;
            default://"f>="
                opcode = JVM.IFGE;
        }

        String falseLabel = frame.getNewLabel();
        String nextLabel = frame.getNewLabel();

        emit(JVM.FCMPG);
        frame.pop(2);
        emit(opcode, falseLabel);
        emit(JVM.ICONST_0);
        emit("goto", nextLabel);
        emit(falseLabel + ":");
        emit(JVM.ICONST_1);
        frame.push();
        emit(nextLabel + ":");
    }

    private void emitALOAD(int index) {
        if (index >= 0 && index <= 3) {
            emit(JVM.ALOAD + "_" + index);
        } else {
            emit(JVM.ALOAD, index);
        }
    }

    private void emitILOAD(int index) {
        if (index >= 0 && index <= 3)
            emit(JVM.ILOAD + "_" + index);
        else
            emit(JVM.ILOAD, index);
    }

    private void emitFLOAD(int index) {
        if (index >= 0 && index <= 3)
            emit(JVM.FLOAD + "_" + index);
        else
            emit(JVM.FLOAD, index);
    }

    private void emitGETSTATIC(String T, String I) {
        emit(JVM.GETSTATIC, classname + "/" + I, T);
    }

    private void emitISTORE(Ident ast) {
        int index;
        if (ast.decl instanceof ParaDecl) {
            index = ((ParaDecl) ast.decl).index;
        } else {
            index = ((LocalVarDecl) ast.decl).index;
        }

        if (index >= 0 && index <= 3) {
            emit(JVM.ISTORE + "_" + index);
        } else {
            emit(JVM.ISTORE, index);
        }
    }

    private void emitFSTORE(Ident ast) {
        int index;
        if (ast.decl instanceof ParaDecl) {
            index = ((ParaDecl) ast.decl).index;
        } else {
            index = ((LocalVarDecl) ast.decl).index;
        }
        if (index >= 0 && index <= 3) {
            emit(JVM.FSTORE + "_" + index);
        } else {
            emit(JVM.FSTORE, index);
        }
    }

    private void emitASTORE(int index) {
        if (index >= 0 && index <= 3) {
            emit(JVM.ASTORE + "_" + index);
        } else {
            emit(JVM.ASTORE, index);
        }
    }

    private void emitPUTSTATIC(String T, String I) {
        emit(JVM.PUTSTATIC, classname + "/" + I, T);
    }

    private void emitICONST(int value) {
        if (value == -1) {
            emit(JVM.ICONST_M1);
        } else if (value >= 0 && value <= 5) {
            emit(JVM.ICONST + "_" + value);
        } else if (value >= -128 && value <= 127) {
            emit(JVM.BIPUSH, value);
        } else if (value >= -32768 && value <= 32767) {
            emit(JVM.SIPUSH, value);
        } else {
            emit(JVM.LDC, value);
        }
    }

    private void emitFCONST(float value) {
        if (value == 0.0f) {
            emit(JVM.FCONST_0);
        } else if (value == 1.0f) {
            emit(JVM.FCONST_1);
        } else if (value == 2.0f) {
            emit(JVM.FCONST_2);
        } else {
            emit(JVM.LDC, value);
        }
    }

    private void emitBCONST(boolean value) {
        emit(value ? JVM.ICONST_1 : JVM.ICONST_0);
    }

    private String getJavaType(Type t) {
        if (t.equals(StdEnvironment.booleanType)) {
            return "Z";
        } else if (t.equals(StdEnvironment.intType)) {
            return "I";
        } else if (t.equals(StdEnvironment.floatType)) {
            return "F";
        } else {
            // StdEnvironment.voidType
            return "V";
        }
    }

    private void framePop(Expr expr, Frame frame) {
        if (!(expr instanceof CallExpr
                && expr.type.isVoidType()
                || expr instanceof EmptyExpr
                || expr instanceof AssignExpr)) {
            emit(JVM.POP);
            frame.pop();
        }
    }

    @Override
    public Object visitProgram(Program ast, Object o) {
        // Generates the default constructor initialiser
        emit(JVM.CLASS, "public", classname);
        emit(JVM.SUPER, "java/lang/Object");
        emit("");

        // Three subpasses:

        // (1) Generate .field definition statements since
        //     these are required to appear before method definitions
        List list = ast.FL;
        while (!list.isEmpty()) {
            DeclList dlAST = (DeclList) list;
            if (dlAST.D instanceof GlobalVarDecl) {
                GlobalVarDecl vAST = (GlobalVarDecl) dlAST.D;
                emit(JVM.STATIC_FIELD, vAST.I.spelling, getJavaType(vAST.T));
            }
            list = dlAST.DL;
        }
        emit("");

        // (2) Generate <clinit> for global variables (assumed to be static)
        emit("; standard class static initializer ");
        emit(JVM.METHOD_START, "static <clinit>()V");
        emit("");

        // create a Frame for <clinit>

        Frame frame = new Frame(false);
        list = ast.FL;
        while (!list.isEmpty()) {
            DeclList dlAST = (DeclList) list;
            if (dlAST.D instanceof GlobalVarDecl) {
                GlobalVarDecl vAST = (GlobalVarDecl) dlAST.D;
                // modified
                if (vAST.T.isArrayType()) {
                    ArrayType arrayType = (ArrayType) vAST.T;
                    arrayType.visit(this, frame);
                    if (!vAST.E.isEmptyExpr()) {
                        vAST.E.visit(this, frame);
                    }
                    emitPUTSTATIC(getJavaType(arrayType), vAST.I.spelling);
                    frame.pop();
                    list = dlAST.DL;
                    continue;
                }
                if (!vAST.E.isEmptyExpr()) {
                    vAST.E.visit(this, frame);
                } else {
                    if (vAST.T.equals(StdEnvironment.floatType))
                        emit(JVM.FCONST_0);
                    else
                        emit(JVM.ICONST_0);
                    frame.push();
                }
                emitPUTSTATIC(getJavaType(vAST.T), vAST.I.spelling);
                frame.pop();
            }
            list = dlAST.DL;
        }

        emit("");
        emit("; set limits used by this method");
        emit(JVM.LIMIT, "locals", frame.getNewIndex());

        emit(JVM.LIMIT, "stack", frame.getMaximumStackSize());
        emit(JVM.RETURN);
        emit(JVM.METHOD_END, "method");
        emit("");

        // (3) Generate Java bytecode for the VC program
        emit("; standard constructor initializer ");
        emit(JVM.METHOD_START, "public <init>()V");
        emit(JVM.LIMIT, "stack 1");
        emit(JVM.LIMIT, "locals 1");
        emit(JVM.ALOAD_0);
        emit(JVM.INVOKESPECIAL, "java/lang/Object/<init>()V");
        emit(JVM.RETURN);
        emit(JVM.METHOD_END, "method");

        return ast.FL.visit(this, o);
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
        return null;
    }

    @Override
    public Object visitDeclList(DeclList ast, Object o) {
        ast.D.visit(this, o);
        ast.DL.visit(this, o);
        return null;
    }

    @Override
    public Object visitFuncDecl(FuncDecl ast, Object o) {
        Frame frame;
        if ("main".equals(ast.I.spelling)) {
            frame = new Frame(true);
            frame.getNewIndex();
            emit(JVM.METHOD_START, "public static main([Ljava/lang/String;)V");
            frame.getNewIndex();
        } else {
            frame = new Frame(false);
            frame.getNewIndex();

            String retType = getJavaType(ast.T);

            StringBuffer argsTypes = new StringBuffer();
            List fpl = ast.PL;
            while (!fpl.isEmpty()) {
                if (((ParaList) fpl).P.T.equals(StdEnvironment.booleanType))
                    argsTypes.append("Z");
                else if (((ParaList) fpl).P.T.equals(StdEnvironment.intType))
                    argsTypes.append("I");
                else if (((ParaList) fpl).P.T.equals(StdEnvironment.floatType))
                    argsTypes.append("F");
                else if (((ParaList) fpl).P.T.isArrayType()) {
                    ArrayType type = (ArrayType) ((ParaList) fpl).P.T;
                    if (type.T.isIntType()) {
                        argsTypes.append("[I");
                    } else if (type.T.isFloatType()) {
                        argsTypes.append("[F");
                    } else if (type.T.isBooleanType()) {
                        argsTypes.append("[Z");
                    }
                }
                fpl = ((ParaList) fpl).PL;
            }

            emit(JVM.METHOD_START, ast.I.spelling + "(" + argsTypes + ")" + retType);
        }

        ast.S.visit(this, frame);


        if (StdEnvironment.voidType.equals(ast.T)) {
            emit("");
            emit("; return may not be present in a VC function returning void");
            emit("; The following return inserted by the VC compiler");
            emit(JVM.RETURN);
        } else if (ast.I.spelling.equals("main") && !isReturnMain) {
            emit(JVM.RETURN);
            isReturnMain = true;
        } else {
            emit(JVM.NOP);
        }

        emit("");
        emit("; set limits used by this method");
        emit(JVM.LIMIT, "locals", frame.getNewIndex());
        emit(JVM.LIMIT, "stack", frame.getMaximumStackSize());
        emit(".end method");
        return null;
    }

    @Override
    public Object visitGlobalVarDecl(GlobalVarDecl ast, Object o) {
        return null;
    }

    @Override
    public Object visitLocalVarDecl(LocalVarDecl ast, Object o) {
        Frame frame = (Frame) o;
        ast.index = frame.getNewIndex();
        emit(JVM.VAR + " " + ast.index + " is " + ast.I.spelling + " " + getJavaType(ast.T) + " from " + frame.scopeStart.peek() + " to " + frame.scopeEnd.peek());

        if (ast.T.isArrayType()) {
            ArrayType arrayType = (ArrayType) ast.T;
            arrayType.visit(this, o);
            if (!ast.E.isEmptyExpr()) {
                ast.E.visit(this, o);
            }
            emitASTORE(ast.index);
            frame.pop();
            return null;
        }

        if (!ast.E.isEmptyExpr()) {
            ast.E.visit(this, o);

            if (ast.T.equals(StdEnvironment.floatType)) {
                if (ast.index >= 0 && ast.index <= 3) {
                    emit(JVM.FSTORE + "_" + ast.index);
                } else {
                    emit(JVM.FSTORE, ast.index);
                }
                frame.pop();
            } else {
                if (ast.index >= 0 && ast.index <= 3) {
                    emit(JVM.ISTORE + "_" + ast.index);
                } else {
                    emit(JVM.ISTORE, ast.index);
                }
                frame.pop();
            }
        }
        return null;
    }

    @Override
    public Object visitStmtList(StmtList ast, Object o) {
        ast.S.visit(this, o);
        ast.SL.visit(this, o);
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt ast, Object o) {
        Frame frame = (Frame) o;
        String L1 = frame.getNewLabel();
        String L2 = frame.getNewLabel();
        ast.E.visit(this, o);
        emit(JVM.IFEQ, L1);
        ast.S1.visit(this, o);
        emit(JVM.GOTO, L2);
        emit(L1 + ":");
        ast.S2.visit(this, o);
        emit(L2 + ":");
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt ast, Object o) {
        Frame frame = (Frame) o;
        String L1 = frame.getNewLabel();
        String L2 = frame.getNewLabel();
        frame.conStack.push(L1);
        frame.brkStack.push(L2);
        emit(L1 + ":");
        ast.E.visit(this, o);
        emit(JVM.IFEQ, L2);
        ast.S.visit(this, o);
        emit(JVM.GOTO, L1);
        emit(L2 + ":");
        return null;
    }

    @Override
    public Object visitForStmt(ForStmt ast, Object o) {
        Frame frame = (Frame) o;
        String L1 = frame.getNewLabel();
        String L2 = frame.getNewLabel();
        String L3 = frame.getNewLabel();
        frame.conStack.push(L3);
        frame.brkStack.push(L2);
        ast.E1.visit(this, o);
        framePop(ast.E1, frame);
        emit(L1 + ":");
        ast.E2.visit(this, o);
        if (!ast.E2.isEmptyExpr()) {
            emit(JVM.IFEQ, L2);
        }
        ast.S.visit(this, o);
        emit(L3 + ":");
        ast.E3.visit(this, o);
        framePop(ast.E3, frame);
        emit(JVM.GOTO, L1);
        emit(L2 + ":");
        frame.conStack.pop();
        frame.brkStack.pop();
        return null;
    }

    @Override
    public Object visitBreakStmt(BreakStmt ast, Object o) {
        Frame frame = (Frame) o;
        emit(JVM.GOTO, frame.brkStack.peek());
        return null;
    }

    @Override
    public Object visitContinueStmt(ContinueStmt ast, Object o) {
        Frame frame = (Frame) o;
        emit(JVM.GOTO, frame.conStack.peek());
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt ast, Object o) {
        Frame frame = (Frame) o;
        if (frame.isMain()) {
            emit(JVM.RETURN);
            isReturnMain = true;
            return null;
        }
        if (ast.E.isEmptyExpr()) {
            emit(JVM.RETURN);
        } else {
            ast.E.visit(this, o);
            if (ast.E.type.isFloatType()) {
                emit(JVM.FRETURN);
            } else {
                emit(JVM.IRETURN);
            }
        }
        return null;
    }

    @Override
    public Object visitCompoundStmt(CompoundStmt ast, Object o) {
        Frame frame = (Frame) o;

        String scopeStart = frame.getNewLabel();
        String scopeEnd = frame.getNewLabel();
        frame.scopeStart.push(scopeStart);
        frame.scopeEnd.push(scopeEnd);

        emit(scopeStart + ":");
        if (ast.parent instanceof FuncDecl) {
            if (((FuncDecl) ast.parent).I.spelling.equals("main")) {
                emit(JVM.VAR, "0 is argv [Ljava/lang/String; from " + frame.scopeStart.peek() + " to " + frame.scopeEnd.peek());
                emit(JVM.VAR, "1 is vc$ L" + classname + "; from " + frame.scopeStart.peek() + " to " + frame.scopeEnd.peek());
                // Generate code for the initialiser vc$ = new classname();
                emit(JVM.NEW, classname);
                emit(JVM.DUP);
                frame.push(2);
                emit("invokenonvirtual", classname + "/<init>()V");
                frame.pop();
                emit(JVM.ASTORE_1);
                frame.pop();
            } else {
                emit(JVM.VAR, "0 is this L" + classname + "; from " + frame.scopeStart.peek() + " to " + frame.scopeEnd.peek());
                ((FuncDecl) ast.parent).PL.visit(this, o);
            }
        }
        ast.DL.visit(this, o);
        ast.SL.visit(this, o);
        emit(scopeEnd + ":");

        frame.scopeStart.pop();
        frame.scopeEnd.pop();
        return null;
    }

    @Override
    public Object visitExprStmt(ExprStmt ast, Object o) {
        Frame frame = (Frame) o;
        ast.E.visit(this, o);
        framePop(ast.E, frame);
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
        ast.IL.visit(this, o);
        return null;
    }

    @Override
    public Object visitFloatExpr(FloatExpr ast, Object o) {
        ast.FL.visit(this, o);
        return null;
    }

    @Override
    public Object visitBooleanExpr(BooleanExpr ast, Object o) {
        ast.BL.visit(this, o);
        return null;
    }

    @Override
    public Object visitStringExpr(StringExpr ast, Object o) {
        ast.SL.visit(this, o);
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr ast, Object o) {
        Frame frame = (Frame) o;
        String L1 = frame.getNewLabel();
        String L2 = frame.getNewLabel();
        ast.E.visit(this, o);
        String op = ast.O.spelling;
        switch (op) {
            case "i!":
                emit(JVM.IFNE, L1);
                emitBCONST(true);
                emit(JVM.GOTO, L2);
                emit(L1);
                emitBCONST(false);
                emit(L2);
                break;
            case "i-":
                emit(JVM.INEG);
                break;
            case "f-":
                emit(JVM.FNEG);
                break;
            case "i2f":
                emit(JVM.I2F);
                break;
        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr ast, Object o) {
        Frame frame = (Frame) o;
        String op = ast.O.spelling;
        if (arithmeticOp.containsKey(op)) {
            ast.E1.visit(this, o);
            ast.E2.visit(this, o);
            emit(arithmeticOp.get(op));
            // two operands are popped and result is pushed into operand stack, shrink the stack
            frame.pop();
        } else if (cmpOp.contains(op)) {
            ast.E1.visit(this, o);
            ast.E2.visit(this, o);
            if (op.contains("f")) {
                emitFCMP(op, frame);
            } else if (op.contains("i")) {
                emitIF_ICMPCOND(op, frame);
            }
            frame.pop();
        } else if (op.equals("i&&")) {
            String L1 = frame.getNewLabel();
            String L2 = frame.getNewLabel();
            ast.E1.visit(this, o);
            emit(JVM.IFEQ, L1);
            ast.E2.visit(this, o);
            emit(JVM.IFEQ, L1);
            emitICONST(1);
            emit(JVM.GOTO, L2);
            emit(L1 + ":");
            emitICONST(0);
            emit(L2 + ":");
            frame.push();
        } else if (op.equals("i||")) {
            String L1 = frame.getNewLabel();
            String L2 = frame.getNewLabel();
            ast.E1.visit(this, o);
            emit(JVM.IFNE, L1);
            ast.E2.visit(this, o);
            emit(JVM.IFNE, L1);
            emitICONST(0);
            emit(JVM.GOTO, L2);
            emit(L1 + ":");
            emitICONST(1);
            emit(L2 + ":");
            frame.push();
        }
        return null;
    }

    @Override
    public Object visitInitExpr(InitExpr ast, Object o) {
        Frame frame = (Frame) o;
        List list = ast.IL;
        int index = 0;
        while (!list.isEmpty()) {
            ExprList exprList = (ExprList) list;
            emit(JVM.DUP);
            frame.push();
            emitICONST(index);
            frame.push();
            exprList.E.visit(this, o);
            if (exprList.E.type.isFloatType()) {
                emit(JVM.FASTORE);
            } else if (exprList.E.type.isBooleanType()) {
                emit(JVM.BASTORE);
            } else {
                emit(JVM.IASTORE);
            }
            frame.pop(3);
            index++;
            list = exprList.EL;
        }
        return null;
    }

    @Override
    public Object visitExprList(ExprList ast, Object o) {
        return null;
    }

    @Override
    public Object visitArrayExpr(ArrayExpr ast, Object o) {
        Frame frame = (Frame) o;
        ast.V.visit(this, o);
        ast.E.visit(this, o);
        if (ast.type.isFloatType()) {
            emit(JVM.FALOAD);
        } else if (ast.type.isBooleanType()) {
            emit(JVM.BALOAD);
        } else {
            emit(JVM.IALOAD);
        }
        frame.pop();
        return null;
    }

    @Override
    public Object visitVarExpr(VarExpr ast, Object o) {
        ast.V.visit(this, o);
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr ast, Object o) {
        Frame frame = (Frame) o;
        switch (ast.I.spelling) {
            case "getInt":
                ast.AL.visit(this, o);
                emit("invokestatic VC/lang/System.getInt()I");
                frame.push();
                break;
            case "putInt":
                ast.AL.visit(this, o);
                emit("invokestatic VC/lang/System.putInt(I)V");
                frame.pop();
                break;
            case "putIntLn":
                ast.AL.visit(this, o);
                emit("invokestatic VC/lang/System/putIntLn(I)V");
                frame.pop();
                break;
            case "getFloat":
                ast.AL.visit(this, o);
                emit("invokestatic VC/lang/System/getFloat()F");
                frame.push();
                break;
            case "putFloat":
                ast.AL.visit(this, o);
                emit("invokestatic VC/lang/System/putFloat(F)V");
                frame.pop();
                break;
            case "putFloatLn":
                ast.AL.visit(this, o);
                emit("invokestatic VC/lang/System/putFloatLn(F)V");
                frame.pop();
                break;
            case "putBool":
                ast.AL.visit(this, o);
                emit("invokestatic VC/lang/System/putBool(Z)V");
                frame.pop();
                break;
            case "putBoolLn":
                ast.AL.visit(this, o);
                emit("invokestatic VC/lang/System/putBoolLn(Z)V");
                frame.pop();
                break;
            case "putString":
                ast.AL.visit(this, o);
                emit(JVM.INVOKESTATIC, "VC/lang/System/putString(Ljava/lang/String;)V");
                frame.pop();
                break;
            case "putStringLn":
                ast.AL.visit(this, o);
                emit(JVM.INVOKESTATIC, "VC/lang/System/putStringLn(Ljava/lang/String;)V");
                frame.pop();
                break;
            case "putLn":
                ast.AL.visit(this, o);
                emit("invokestatic VC/lang/System/putLn()V");
                break;
            default:
                FuncDecl fAST = (FuncDecl) ast.I.decl;

                // all functions except main are assumed to be instance methods
                if (frame.isMain()) {
                    // vc.funcname(...)
                    emit("aload_1");
                } else {
                    // this.funcname(...)
                    emit("aload_0");
                }
                frame.push();

                ast.AL.visit(this, o);

                String retType = getJavaType(fAST.T);

                // The types of the parameters of the called function are not
                // directly available in the FuncDecl node but can be gathered
                // by traversing its field PL.

                StringBuffer argsTypes = new StringBuffer();
                int paraCount = 0;
                List fpl = fAST.PL;
                while (!fpl.isEmpty()) {
                    if (((ParaList) fpl).P.T.equals(StdEnvironment.booleanType)) {
                        argsTypes.append("Z");
                    } else if (((ParaList) fpl).P.T.equals(StdEnvironment.intType)) {
                        argsTypes.append("I");
                    } else if (((ParaList) fpl).P.T.equals(StdEnvironment.floatType)) {
                        argsTypes.append("F");
                    } else if (((ParaList) fpl).P.T.isArrayType()) {
                        ArrayType type = (ArrayType) ((ParaList) fpl).P.T;
                        if (type.T.isIntType()) {
                            argsTypes.append("[I");
                        } else if (type.T.isFloatType()) {
                            argsTypes.append("[F");
                        } else if (type.T.isBooleanType()) {
                            argsTypes.append("[Z");
                        }
                    }
                    paraCount++;
                    fpl = ((ParaList) fpl).PL;
                }

                emit("invokevirtual", classname + "/" + ast.I.spelling + "(" + argsTypes + ")" + retType);
                frame.pop(paraCount);

                if (!retType.equals("V")) {
                    frame.push();
                }
        }
        return null;
    }

    @Override
    public Object visitAssignExpr(AssignExpr ast, Object o) {
        Frame frame = (Frame) o;
        if (ast.E1 instanceof ArrayExpr) {
            ArrayExpr arrayExpr = (ArrayExpr) ast.E1;
            arrayExpr.V.visit(this, o);
            arrayExpr.E.visit(this, o);
            ast.E2.visit(this, o);
            // Java has different array store instruction for integer, boolean and float
            if (ast.E2.type.isFloatType()) {
                emit(JVM.FASTORE);
            } else if (ast.E2.type.isBooleanType()) {
                emit(JVM.BASTORE);
            } else {
                emit(JVM.IASTORE);
            }
            frame.pop(3);
        } else if (ast.E1 instanceof VarExpr) {
            SimpleVar var = (SimpleVar) ((VarExpr) ast.E1).V;
            ast.E2.visit(this, o);
            if (ast.parent instanceof AssignExpr) {
                emit(JVM.DUP);
            }
            if (var.I.decl instanceof GlobalVarDecl) {
                emitPUTSTATIC(getJavaType(var.type), var.I.spelling);
            } else {
                if (ast.type.isFloatType()) {
                    emitFSTORE(var.I);
                } else {
                    emitISTORE(var.I);
                }
            }
        }
        return null;
    }

    @Override
    public Object visitEmptyExpr(EmptyExpr ast, Object o) {
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral ast, Object o) {
        Frame frame = (Frame) o;
        emitICONST(Integer.parseInt(ast.spelling));
        frame.push();
        return null;
    }

    @Override
    public Object visitFloatLiteral(FloatLiteral ast, Object o) {
        Frame frame = (Frame) o;
        emitFCONST(Float.parseFloat(ast.spelling));
        frame.push();
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral ast, Object o) {
        Frame frame = (Frame) o;
        emitBCONST("true".equals(ast.spelling));
        frame.push();
        return null;
    }

    @Override
    public Object visitStringLiteral(StringLiteral ast, Object o) {
        Frame frame = (Frame) o;
        emit(JVM.LDC, "\"" + ast.spelling + "\"");
        frame.push();
        return null;
    }

    @Override
    public Object visitIdent(Ident ast, Object o) {
        return null;
    }

    @Override
    public Object visitOperator(Operator ast, Object o) {
        return null;
    }

    @Override
    public Object visitParaList(ParaList ast, Object o) {
        ast.P.visit(this, o);
        ast.PL.visit(this, o);
        return null;
    }

    @Override
    public Object visitParaDecl(ParaDecl ast, Object o) {
        Frame frame = (Frame) o;
        ast.index = frame.getNewIndex();
        emit(JVM.VAR + " " + ast.index + " is " + ast.I.spelling + " " + getJavaType(ast.T) + " from " + frame.scopeStart.peek() + " to " + frame.scopeEnd.peek());
        return null;
    }

    @Override
    public Object visitArgList(ArgList ast, Object o) {
        ast.A.visit(this, o);
        ast.AL.visit(this, o);
        return null;
    }

    @Override
    public Object visitArg(Arg ast, Object o) {
        ast.E.visit(this, o);
        return null;
    }

    @Override
    public Object visitVoidType(VoidType ast, Object o) {
        return null;
    }

    @Override
    public Object visitBooleanType(BooleanType ast, Object o) {
        return null;
    }

    @Override
    public Object visitIntType(IntType ast, Object o) {
        return null;
    }

    @Override
    public Object visitFloatType(FloatType ast, Object o) {
        return null;
    }

    @Override
    public Object visitStringType(StringType ast, Object o) {
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType ast, Object o) {
        Frame frame = (Frame) o;
        int length = Integer.parseInt(((IntExpr) ast.E).IL.spelling);
        emitICONST(length);
        frame.push();
        emit(JVM.NEWARRAY, ast.T.toString());
        return null;
    }

    @Override
    public Object visitErrorType(ErrorType ast, Object o) {
        return null;
    }

    @Override
    public Object visitSimpleVar(SimpleVar ast, Object o) {
        Frame frame = (Frame) o;
        if (ast.I.decl instanceof GlobalVarDecl) {
            emitGETSTATIC(getJavaType(ast.type), ast.I.spelling);
        } else {
            int index = ((Decl) ast.I.decl).index;
            if (ast.I.decl instanceof GlobalVarDecl) {
                emitGETSTATIC(getJavaType(ast.type), ast.I.spelling);
            } else {
                if (ast.type.isArrayType()) {
                    emitALOAD(index);
                } else if (ast.type.isFloatType()) {
                    emitFLOAD(index);
                } else {
                    emitILOAD(index);
                }
            }
        }
        frame.push();
        return null;
    }
}
