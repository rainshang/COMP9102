/*
 * Parser.java            
 *
 * This parser for a subset of the VC language is intended to 
 *  demonstrate how to create the AST nodes, including (among others): 
 *  [1] a list (of statements)
 *  [2] a function
 *  [3] a statement (which is an expression statement), 
 *  [4] a unary expression
 *  [5] a binary expression
 *  [6] terminals (identifiers, integer literals and operators)
 *
 * In addition, it also demonstrates how to use the two methods start 
 * and finish to determine the position information for the start and 
 * end of a construct (known as a phrase) corresponding an AST node.
 *
 * NOTE THAT THE POSITION INFORMATION WILL NOT BE MARKED. HOWEVER, IT CAN BE
 * USEFUL TO DEBUG YOUR IMPLEMENTATION.
 *
 * (04-||-April-||-2018)


program       -> func-decl
func-decl     -> type identifier "(" ")" compound-stmt
type          -> void
identifier    -> ID
// statements
compound-stmt -> "{" stmt* "}" 
stmt          -> expr-stmt
expr-stmt     -> expr? ";"
// expressions 
expr                -> additive-expr
additive-expr       -> multiplicative-expr
                    |  additive-expr "+" multiplicative-expr
                    |  additive-expr "-" multiplicative-expr
multiplicative-expr -> unary-expr
	            |  multiplicative-expr "*" unary-expr
	            |  multiplicative-expr "/" unary-expr
unary-expr          -> "-" unary-expr
		    |  primary-expr

primary-expr        -> identifier
 		    |  INTLITERAL
		    | "(" expr ")"
 */

package VC.Parser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;
import VC.ErrorReporter;
import VC.ASTs.*;

public class Parser {

    private Scanner scanner;
    private ErrorReporter errorReporter;
    private Token currentToken;

    private SourcePosition previousTokenPosition;
    private SourcePosition dummyPos = new SourcePosition();

    private Set<Integer> primitiveTypeFirstSet;
    private Set<Integer> primaryExprFirstSet;


    public Parser(Scanner lexer, ErrorReporter reporter) {
        scanner = lexer;
        errorReporter = reporter;

        currentToken = scanner.getToken();

        previousTokenPosition = new SourcePosition();

        primitiveTypeFirstSet = new HashSet<>(Arrays.asList(Token.VOID, Token.BOOLEAN, Token.INT, Token.FLOAT));
        primaryExprFirstSet = new HashSet<>(Arrays.asList(Token.ID,
                Token.LPAREN, Token.PLUS, Token.MINUS, Token.NOT,
                Token.INTLITERAL, Token.FLOATLITERAL, Token.BOOLEANLITERAL, Token.STRINGLITERAL));
    }

    // match checks to see f the current token matches tokenExpected.
    // If so, fetches the next token.
    // If not, reports a syntactic error.
    private void match(int tokenExpected) throws SyntaxError {
        if (tokenExpected == currentToken.kind) {
            previousTokenPosition = currentToken.position;
            currentToken = scanner.getToken();
        } else {
            syntacticError("\"%\" expected here", Token.spell(tokenExpected));
        }
    }

    private void accept() {
        previousTokenPosition = currentToken.position;
        currentToken = scanner.getToken();
    }

    private void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
        SourcePosition pos = currentToken.position;
        errorReporter.reportError(messageTemplate, tokenQuoted, pos);
        throw (new SyntaxError());
    }

    // start records the position of the start of a phrase.
    // This is defined to be the position of the first
    // character of the first token of the phrase.
    private void start(SourcePosition position) {
        position.lineStart = currentToken.position.lineStart;
        position.charStart = currentToken.position.charStart;
    }

    // finish records the position of the end of a phrase.
    // This is defined to be the position of the last
    // character of the last token of the phrase.
    private void finish(SourcePosition position) {
        position.lineFinish = previousTokenPosition.lineFinish;
        position.charFinish = previousTokenPosition.charFinish;
    }

    private void copyStart(SourcePosition from, SourcePosition to) {
        to.lineStart = from.lineStart;
        to.charStart = from.charStart;
    }

    private SourcePosition newAndStart() {
        SourcePosition position = new SourcePosition();
        start(position);
        return position;
    }

// ========================== PROGRAMS ========================

    /**
     * program             ->  ( func-decl | var-decl )*
     */
    public Program parseProgram() {
        Program program = null;
        SourcePosition position = newAndStart();

        try {
            List declList = parseDeclList();
            if (currentToken.kind != Token.EOF) {
                syntacticError("\"%\" unknown type", currentToken.spelling);
            }
            program = new Program(declList, position);
        } catch (SyntaxError s) {
        }

        finish(position);
        return program;
    }

// ========================== DECLARATIONS ========================

    private List parseDeclList() throws SyntaxError {
        List declList = null;
        SourcePosition position = newAndStart();

        // first hit
        if (primitiveTypeFirstSet.contains(currentToken.kind)) {
            Type type = parseType();
            Ident ident = parseIdent();

            Decl funDecl = null;
            List varDecl = null;
            if (Token.LPAREN == currentToken.kind) {
                funDecl = parseRemainingFuncDecl(type, ident);
            } else {
                varDecl = parseRemainingVarDecl(type, ident);
            }

            // recursive hit
            List subDeclList = null;
            if (primitiveTypeFirstSet.contains(currentToken.kind)) {
                subDeclList = parseDeclList();
            } else {
                subDeclList = new EmptyDeclList(dummyPos);
            }

            // generate DeclList
            if (funDecl != null) {
                declList = new DeclList(funDecl, subDeclList, position);
            } else {
                declList = varDecl;

                DeclList rightNodeList = (DeclList) varDecl;
                while (!(rightNodeList.DL instanceof EmptyDeclList)) {
                    rightNodeList = (DeclList) rightNodeList.DL;
                }
                rightNodeList.DL = subDeclList;
            }
        } else {
            declList = new EmptyDeclList(dummyPos);
        }

        finish(position);
        return declList;
    }

    /**
     * func-decl           -> type identifier para-list compound-stmt
     */
    private Decl parseRemainingFuncDecl(Type type, Ident ident) throws SyntaxError {
        SourcePosition position = newAndStart();

        List paraList = parseParaList();
        Stmt compoundStmt = parseCompoundStmt();


        finish(position);
        return new FuncDecl(type, ident, paraList, compoundStmt, position);
    }

    /**
     * down-top parse
     * <p>
     * var-decl            -> type init-declarator-list ";"
     * init-declarator-list-> init-declarator ( "," init-declarator )*
     * init-declarator     -> declarator ( "=" initialiser )?
     * declarator          -> identifier
     * <pre>               |  identifier "[" INTLITERAL? "]"
     */
    private List parseRemainingVarDecl(Type type, Ident ident) throws SyntaxError {
        List varDeclList = null;
        SourcePosition position = newAndStart();

        Type declType = null;
        if (Token.LBRACKET == currentToken.kind) {// declarator          -> identifier "[" INTLITERAL? "]"
            accept();

            Expr indexExpr = null;
            if (Token.INTLITERAL == currentToken.kind) {
                IntLiteral index = parseIntLiteral();
                indexExpr = new IntExpr(index, previousTokenPosition);
            } else {
                indexExpr = new EmptyExpr(dummyPos);
            }
            match(Token.RBRACKET);
            declType = new ArrayType(type, indexExpr, position);
        } else {// declarator          -> identifier
            declType = type;
        }

        // init-declarator     -> declarator ( "=" initialiser )?
        SourcePosition initDeclPosition = newAndStart();

        Decl varDecl = null;
        if (Token.EQ == currentToken.kind) {// init-declarator     -> declarator  "=" initialiser
            accept();
            Expr initExpr = parseInitialiser();
            varDecl = new GlobalVarDecl(declType, ident, initExpr, initDeclPosition);
        } else {// init-declarator     -> declarator
            varDecl = new GlobalVarDecl(declType, ident, new EmptyExpr(dummyPos), initDeclPosition);
        }

        finish(initDeclPosition);

        // init-declarator-list-> init-declarator ( "," init-declarator )*
        if (Token.COMMA == currentToken.kind) {
            accept();
            boolean isGlobal = true;
            List subDeclAST = parseInitDeclaratorList(type, isGlobal);
            varDeclList = new DeclList(varDecl, subDeclAST, position);
        } else {
            varDeclList = new DeclList(varDecl, new EmptyDeclList(dummyPos), position);
        }
        match(Token.SEMICOLON);

        finish(position);
        return varDeclList;
    }

    /**
     * var-decl            -> type init-declarator-list ";"
     */
    private List parseVarDecl() throws SyntaxError {
        Type type = parseType();
        boolean isGlobal = false;
        List initDeclaratorList = parseInitDeclaratorList(type, isGlobal);
        match(Token.SEMICOLON);
        return initDeclaratorList;
    }

    /**
     * init-declarator-list-> init-declarator ( "," init-declarator )*
     */
    private List parseInitDeclaratorList(Type type, boolean isGlobal) throws SyntaxError {
        List initDeclaratorList = null;
        SourcePosition position = newAndStart();

        // first hit
        Decl initDeclarator = parseInitDeclarator(type, isGlobal);

        // recursive hit
        if (Token.COMMA == currentToken.kind) {
            accept();
            List subInitDeclaratorList = parseInitDeclaratorList(type, isGlobal);
            initDeclaratorList = new DeclList(initDeclarator, subInitDeclaratorList, position);
        } else {
            initDeclaratorList = new DeclList(initDeclarator, new EmptyDeclList(dummyPos), position);
        }

        finish(position);
        return initDeclaratorList;
    }

    /**
     * init-declarator     -> declarator ( "=" initialiser )?
     */
    private Decl parseInitDeclarator(Type type, boolean isGlobal) throws SyntaxError {
        Decl initDeclarator = null;
        SourcePosition position = newAndStart();

        Decl declarator = parseDeclarator(type);
        if (Token.EQ == currentToken.kind) {
            accept();
            Expr initialiser = parseInitialiser();
            if (isGlobal) {
                initDeclarator = new GlobalVarDecl(declarator.T, declarator.I, initialiser, position);
            } else {
                initDeclarator = new LocalVarDecl(declarator.T, declarator.I, initialiser, position);
            }
        } else {
            if (isGlobal) {
                initDeclarator = new GlobalVarDecl(declarator.T, declarator.I, new EmptyExpr(dummyPos), position);
            } else {
                initDeclarator = new LocalVarDecl(declarator.T, declarator.I, new EmptyExpr(dummyPos), position);
            }
        }

        finish(position);
        return initDeclarator;
    }

    /**
     * declarator          -> identifier
     */
    private Decl parseDeclarator(Type type) throws SyntaxError {
        SourcePosition position = newAndStart();
        Decl declarator = new Decl(position) {
            @Override
            public Object visit(Visitor v, Object o) {
                return null;
            }
        };

        Ident ident = parseIdent();
        declarator.I = ident;

        if (Token.LBRACKET == currentToken.kind) {// declarator          -> identifier "[" INTLITERAL? "]"
            accept();
            Expr indexExpr = null;
            if (Token.INTLITERAL == currentToken.kind) {
                IntLiteral index = parseIntLiteral();
                indexExpr = new IntExpr(index, previousTokenPosition);
            } else {
                indexExpr = new EmptyExpr(dummyPos);
            }
            match(Token.RBRACKET);

            declarator.T = new ArrayType(type, indexExpr, position);
        } else {// declarator          -> identifier
            declarator.T = type;
        }

        finish(position);
        return declarator;
    }

    /**
     * initialiser         -> expr
     * <pre>               |  "{" expr ( "," expr )* "}"
     */
    private Expr parseInitialiser() throws SyntaxError {
        SourcePosition position = newAndStart();
        Expr expr = null;

        if (Token.LCURLY == currentToken.kind) {
            accept();
            List exprList = parseInitExprList();
            expr = new InitExpr(exprList, position);
            match(Token.RCURLY);
        } else {
            expr = parseExpr();
        }

        finish(position);
        return expr;
    }

    private List parseInitExprList() throws SyntaxError {
        List exprList = null;
        SourcePosition position = newAndStart();

        Expr expr = parseExpr();

        if (Token.COMMA == currentToken.kind) {
            accept();
            List subList = parseInitExprList();
            exprList = new ExprList(expr, subList, position);
        } else {
            exprList = new ExprList(expr, new EmptyExprList(dummyPos), position);
        }

        finish(position);
        return exprList;
    }


// ======================= PRIMITIVE TYPES ==============================

    /**
     * type                -> void | boolean | int | float
     */
    private Type parseType() throws SyntaxError {
        Type type = null;

        switch (currentToken.kind) {
            case Token.VOID:
                type = new VoidType(currentToken.position);
                break;
            case Token.BOOLEAN:
                type = new BooleanType(currentToken.position);
                break;
            case Token.INT:
                type = new IntType(currentToken.position);
                break;
            case Token.FLOAT:
                type = new FloatType(currentToken.position);
                break;
            default:
                syntacticError("primitive types expected here", "");
        }

        accept();
        return type;
    }

// ======================= IDENTIFIERS ======================

    /**
     * identifier          -> ID
     */
    private Ident parseIdent() throws SyntaxError {
        Ident ident = null;
        if (Token.ID == currentToken.kind) {
            ident = new Ident(currentToken.spelling, currentToken.position);
        } else {
            syntacticError("identifier expected here", "");
        }

        accept();
        return ident;
    }

// ======================= STATEMENTS ==============================

    /**
     * compound-stmt       -> "{" var-decl* stmt* "}"
     */
    private Stmt parseCompoundStmt() throws SyntaxError {
        Stmt compoundStmt = null;
        SourcePosition position = newAndStart();

        match(Token.LCURLY);
        // Insert code here to build a DeclList node for variable declarations
        List varDeclList = parseVarDeclList();
        List stmtList = parseStmtList();
        match(Token.RCURLY);

        /* In the subset of the VC grammar, no variable declarations are
         * allowed. Therefore, a block is empty iff it has no statements.
         */
        if (varDeclList instanceof EmptyDeclList
                && stmtList instanceof EmptyStmtList) {
            compoundStmt = new EmptyCompStmt(position);
        } else {
            compoundStmt = new CompoundStmt(varDeclList, stmtList, position);
        }

        finish(position);
        return compoundStmt;
    }

    /**
     * var-decl            -> type init-declarator-list ";"
     */
    private List parseVarDeclList() throws SyntaxError {
        List varDeclList = null;

        if (primitiveTypeFirstSet.contains(currentToken.kind)) {
            varDeclList = parseVarDecl();

            DeclList rightNodeList = (DeclList) varDeclList;
            while (!(rightNodeList.DL instanceof EmptyDeclList)) {
                rightNodeList = (DeclList) rightNodeList.DL;
            }
            rightNodeList.DL = parseVarDeclList();
        } else {
            varDeclList = new EmptyDeclList(dummyPos);
        }

        return varDeclList;
    }

    private List parseStmtList() throws SyntaxError {
        List stmtList = null;
        SourcePosition position = newAndStart();

        if (Token.RCURLY != currentToken.kind) {
            Stmt stmt = parseStmt();
            List subStmtList = null;
            if (Token.RCURLY != currentToken.kind) {
                subStmtList = parseStmtList();
                stmtList = new StmtList(stmt, subStmtList, position);
            } else {
                stmtList = new StmtList(stmt, new EmptyStmtList(dummyPos), position);
            }
        } else {
            stmtList = new EmptyStmtList(dummyPos);
        }

        finish(position);
        return stmtList;
    }

    /**
     * stmt                -> compound-stmt
     * <pre>               |  if-stmt
     * <pre>               |  for-stmt
     * <pre>               |  while-stmt
     * <pre>               |  break-stmt
     * <pre>               |  continue-stmt
     * <pre>               |  return-stmt
     * <pre>               |  expr-stmt
     */
    private Stmt parseStmt() throws SyntaxError {
        Stmt stmt = null;

        switch (currentToken.kind) {
            case Token.LCURLY:
                stmt = parseCompoundStmt();
                break;
            case Token.IF:
                stmt = parseIfStmt();
                break;
            case Token.FOR:
                stmt = parseForStmt();
                break;
            case Token.WHILE:
                stmt = parseWhileStmt();
                break;
            case Token.BREAK:
                stmt = parseBreakStmt();
                break;
            case Token.CONTINUE:
                stmt = parseContinueStmt();
                break;
            case Token.RETURN:
                stmt = parseReturnStmt();
                break;
            default:
                stmt = parseExprStmt();
        }

        return stmt;
    }

    /**
     * if-stmt             -> if "(" expr ")" stmt ( else stmt )?
     */
    private Stmt parseIfStmt() throws SyntaxError {
        Stmt ifStmt = null;
        SourcePosition position = newAndStart();

        accept();
        match(Token.LPAREN);
        Expr expr = parseExpr();
        match(Token.RPAREN);
        Stmt ifStmtStmt = parseStmt();
        if (Token.ELSE == currentToken.kind) {
            accept();
            Stmt elseStmt = parseStmt();
            ifStmt = new IfStmt(expr, ifStmtStmt, elseStmt, position);
        } else {
            ifStmt = new IfStmt(expr, ifStmtStmt, position);
        }

        finish(position);
        return ifStmt;
    }

    /**
     * for-stmt            -> for "(" expr? ";" expr? ";" expr? ")" stmt
     */
    private Stmt parseForStmt() throws SyntaxError {
        SourcePosition position = newAndStart();

        accept();
        match(Token.LPAREN);
        Expr[] exprs = new Expr[3];
        for (int i = 0; i < 3; i++) {
            if (primaryExprFirstSet.contains(currentToken.kind)) {
                exprs[i] = parseExpr();
            } else {
                exprs[i] = new EmptyExpr(dummyPos);
            }
            if (i < 2) {
                match(Token.SEMICOLON);
            }
        }
        match(Token.RPAREN);

        Stmt stmt = parseStmt();

        finish(position);
        return new ForStmt(exprs[0], exprs[1], exprs[2], stmt, position);
    }

    /**
     * while-stmt          -> while "(" expr ")" stmt
     */
    private Stmt parseWhileStmt() throws SyntaxError {
        SourcePosition position = newAndStart();

        accept();
        match(Token.LPAREN);
        Expr expr = parseExpr();
        match(Token.RPAREN);
        Stmt stmt = parseStmt();

        finish(position);
        return new WhileStmt(expr, stmt, position);
    }

    /**
     * break-stmt          -> break ";"
     */
    private Stmt parseBreakStmt() throws SyntaxError {
        SourcePosition position = newAndStart();

        accept();
        match(Token.SEMICOLON);

        finish(position);
        return new BreakStmt(position);
    }

    /**
     * continue-stmt       -> continue ";"
     */
    private Stmt parseContinueStmt() throws SyntaxError {
        SourcePosition position = newAndStart();

        accept();
        match(Token.SEMICOLON);

        finish(position);
        return new ContinueStmt(position);
    }

    /**
     * return-stmt         -> return expr? ";"
     */
    private Stmt parseReturnStmt() throws SyntaxError {
        SourcePosition position = newAndStart();

        accept();
        Expr expr = null;
        if (primaryExprFirstSet.contains(currentToken.kind)) {
            expr = parseExpr();
        } else {
            expr = new EmptyExpr(dummyPos);
        }
        match(Token.SEMICOLON);

        finish(position);
        return new ReturnStmt(expr, position);
    }

    /**
     * expr-stmt           -> expr? ";"
     */
    private Stmt parseExprStmt() throws SyntaxError {
        SourcePosition position = newAndStart();

        Expr expr = null;
        if (primaryExprFirstSet.contains(currentToken.kind)) {
            expr = parseExpr();
        }
        match(Token.SEMICOLON);
        finish(position);
        return new ExprStmt((expr != null ?
                expr :
                new EmptyExpr(dummyPos)), position);
    }

// ======================= OPERATORS ======================

    private Operator acceptOperator() throws SyntaxError {
        Operator operator = new Operator(currentToken.spelling, currentToken.position);
        accept();
        return operator;
    }

// ======================= EXPRESSIONS ======================

    /**
     * expr                -> assignment-expr
     */
    private Expr parseExpr() throws SyntaxError {
        return parseAssignExpr();
    }

    // A -> (B op )* B is A -> B, A -> B op B, A -> B op B op B, ... equals
    // A -> B (op B)*

    /**
     * assignment-expr     -> ( cond-or-expr "=" )* cond-or-expr
     */
    private Expr parseAssignExpr() throws SyntaxError {
        Expr expr = null;
        SourcePosition position = newAndStart();

        expr = parseCondOrExpr();

        if (Token.EQ == currentToken.kind) {
            accept();
            Expr subExpr = parseAssignExpr();
            expr = new AssignExpr(expr, subExpr, position);
        }

        finish(position);
        return expr;
    }

    // A -> B | A op B is left recursion
    // let A -> B A', A' -> e | op B, thus,
    // A -> B (Op B)*

    /**
     * cond-or-expr        -> cond-and-expr
     * <pre/>              |  cond-or-expr "||" cond-and-expr
     */
    private Expr parseCondOrExpr() throws SyntaxError {
        Expr expr = null;

        expr = parseCondAndExpr();

        while (Token.OROR == currentToken.kind) {
            Operator operator = acceptOperator();
            Expr subExpr = parseCondAndExpr();
            SourcePosition subPosition = newAndStart();
            finish(subPosition);
            expr = new BinaryExpr(expr, operator, subExpr, subPosition);
        }

        return expr;
    }

    /**
     * cond-and-expr       -> equality-expr
     * <pre>               |  cond-and-expr "&&" equality-expr
     */
    private Expr parseCondAndExpr() throws SyntaxError {
        Expr expr = null;

        expr = parseEqualityExpr();

        while (Token.ANDAND == currentToken.kind) {
            Operator operator = acceptOperator();
            Expr subExpr = parseEqualityExpr();
            SourcePosition subPosition = newAndStart();
            finish(subPosition);
            expr = new BinaryExpr(expr, operator, subExpr, subPosition);
        }

        return expr;
    }

    /**
     * equality-expr       -> rel-expr
     * <pre>               |  equality-expr "==" rel-expr
     * <pre>               |  equality-expr "!=" rel-expr
     */
    private Expr parseEqualityExpr() throws SyntaxError {
        Expr expr = null;

        expr = parseRelExpr();

        while (Token.EQEQ == currentToken.kind
                || Token.NOTEQ == currentToken.kind) {
            Operator operator = acceptOperator();
            Expr subExpr = parseRelExpr();
            SourcePosition subPosition = newAndStart();
            finish(subPosition);
            expr = new BinaryExpr(expr, operator, subExpr, subPosition);
        }

        return expr;
    }

    /**
     * rel-expr            -> additive-expr
     * <pre>               |  rel-expr "<" additive-expr
     * <pre>               |  rel-expr "<=" additive-expr
     * <pre>               |  rel-expr ">" additive-expr
     * <pre>               |  rel-expr ">=" additive-expr
     */
    private Expr parseRelExpr() throws SyntaxError {
        Expr expr = null;

        expr = parseAdditiveExpr();

        while (Token.LT == currentToken.kind
                || Token.LTEQ == currentToken.kind
                || Token.GT == currentToken.kind
                || Token.GTEQ == currentToken.kind) {
            Operator operator = acceptOperator();
            Expr subExpr = parseAdditiveExpr();
            SourcePosition subPosition = newAndStart();
            finish(subPosition);
            expr = new BinaryExpr(expr, operator, subExpr, subPosition);
        }

        return expr;
    }

    /**
     * additive-expr       -> multiplicative-expr
     * <pre>               |  additive-expr "+" multiplicative-expr
     * <pre>               |  additive-expr "-" multiplicative-expr
     */
    private Expr parseAdditiveExpr() throws SyntaxError {
        Expr expr = null;

        expr = parseMultiplicativeExpr();

        while (Token.PLUS == currentToken.kind
                || Token.MINUS == currentToken.kind) {
            Operator operator = acceptOperator();
            Expr subExpr = parseMultiplicativeExpr();
            SourcePosition subPosition = newAndStart();
            finish(subPosition);
            expr = new BinaryExpr(expr, operator, subExpr, subPosition);
        }

        return expr;
    }

    /**
     * multiplicative-expr -> unary-expr
     * <pre>               |  multiplicative-expr "*" unary-expr
     * <pre>               |  multiplicative-expr "/" unary-expr
     */
    private Expr parseMultiplicativeExpr() throws SyntaxError {
        Expr expr = null;

        expr = parseUnaryExpr();

        while (Token.MULT == currentToken.kind
                || Token.DIV == currentToken.kind) {
            Operator operator = acceptOperator();
            Expr subExpr = parseUnaryExpr();
            SourcePosition subPosition = newAndStart();
            finish(subPosition);
            expr = new BinaryExpr(expr, operator, subExpr, subPosition);
        }

        return expr;
    }

    /**
     * unary-expr          -> "+" unary-expr
     * <pre>               |  "-" unary-expr
     * <pre>               |  "!" unary-expr
     * <pre>               |  primary-expr
     */
    private Expr parseUnaryExpr() throws SyntaxError {
        Expr expr = null;
        SourcePosition position = newAndStart();

        switch (currentToken.kind) {
            case Token.PLUS:
            case Token.MINUS:
            case Token.NOT:
                Operator operator = acceptOperator();
                Expr subExpr = parseUnaryExpr();
                expr = new UnaryExpr(operator, subExpr, position);
                break;

            default:
                expr = parsePrimaryExpr();
        }

        finish(position);
        return expr;
    }

    /**
     * primary-expr        -> identifier arg-list?
     * <pre>               | identifier "[" expr "]"
     * <pre>               | "(" expr ")"
     * <pre>               | INTLITERAL
     * <pre>               | FLOATLITERAL
     * <pre>               | BOOLLITERAL
     * <pre>               | STRINGLITERAL
     */
    private Expr parsePrimaryExpr() throws SyntaxError {
        Expr expr = null;
        SourcePosition position = newAndStart();

        switch (currentToken.kind) {
            case Token.ID:
                Ident ident = parseIdent();
                if (Token.LPAREN == currentToken.kind) {
                    SourcePosition callPosition = newAndStart();
                    List argList = parseArgList();
                    finish(callPosition);
                    expr = new CallExpr(ident, argList, callPosition);
                } else if (Token.LBRACKET == currentToken.kind) {
                    Var arrayVar = new SimpleVar(ident, previousTokenPosition);
                    accept();
                    SourcePosition arrayPosition = newAndStart();
                    Expr indexExpr = parseExpr();
                    match(Token.RBRACKET);
                    finish(arrayPosition);
                    expr = new ArrayExpr(arrayVar, indexExpr, arrayPosition);
                } else {
                    Var var = new SimpleVar(ident, position);
                    expr = new VarExpr(var, position);
                }
                break;
            case Token.LPAREN:
                accept();
                expr = parseExpr();
                match(Token.RPAREN);
                break;
            case Token.INTLITERAL:
                IntLiteral intLiteral = parseIntLiteral();
                expr = new IntExpr(intLiteral, position);
                break;
            case Token.FLOATLITERAL:
                FloatLiteral floatLiteral = parseFloatLiteral();
                expr = new FloatExpr(floatLiteral, position);
                break;
            case Token.BOOLEANLITERAL:
                BooleanLiteral booleanLiteral = parseBooleanLiteral();
                expr = new BooleanExpr(booleanLiteral, position);
                break;
            case Token.STRINGLITERAL:
                StringLiteral stringLiteral = parseStringLiteral();
                expr = new StringExpr(stringLiteral, position);
                break;

            default:
                syntacticError("illegal parimary expression", currentToken.spelling);
                expr = new EmptyExpr(dummyPos);
        }

        finish(position);
        return expr;
    }

// ========================== LITERALS ========================

    private IntLiteral parseIntLiteral() throws SyntaxError {
        IntLiteral intLiteral = null;

        if (Token.INTLITERAL == currentToken.kind) {
            intLiteral = new IntLiteral(currentToken.spelling, currentToken.position);
        } else {
            syntacticError("integer literal expected here", "");
        }

        accept();
        return intLiteral;
    }

    private FloatLiteral parseFloatLiteral() throws SyntaxError {
        FloatLiteral floatLiteral = null;

        if (Token.FLOATLITERAL == currentToken.kind) {
            floatLiteral = new FloatLiteral(currentToken.spelling, currentToken.position);
        } else {
            syntacticError("integer literal expected here", "");
        }

        accept();
        return floatLiteral;
    }

    private BooleanLiteral parseBooleanLiteral() throws SyntaxError {
        BooleanLiteral booleanLiteral = null;

        if (Token.BOOLEANLITERAL == currentToken.kind) {
            booleanLiteral = new BooleanLiteral(currentToken.spelling, currentToken.position);
        } else {
            syntacticError("boolean literal expected here", "");
        }

        accept();
        return booleanLiteral;
    }

    private StringLiteral parseStringLiteral() throws SyntaxError {
        StringLiteral stringLiteral = null;

        if (Token.STRINGLITERAL == currentToken.kind) {
            stringLiteral = new StringLiteral(currentToken.spelling, currentToken.position);
        } else {
            syntacticError("string literal expected here", "");
        }

        accept();
        return stringLiteral;
    }

// ======================= PARAMETERS =======================

    /**
     * para-list           -> "(" proper-para-list? ")"
     */
    private List parseParaList() throws SyntaxError {
        List paraList = null;

        match(Token.LPAREN);
        if (primitiveTypeFirstSet.contains(currentToken.kind)) {
            paraList = parseProperParaList();
        } else {
            paraList = new EmptyParaList(dummyPos);
        }
        match(Token.RPAREN);

        return paraList;
    }

    /**
     * proper-para-list    -> para-decl ( "," para-decl )*
     */
    private List parseProperParaList() throws SyntaxError {
        List properParaList = null;
        SourcePosition position = newAndStart();

        ParaDecl paraDecl = parseParaDecl();
        if (Token.COMMA == currentToken.kind) {
            accept();
            List subProperParaList = parseProperParaList();
            properParaList = new ParaList(paraDecl, subProperParaList, position);
        } else {
            properParaList = new ParaList(paraDecl, new EmptyParaList(dummyPos), position);
        }

        finish(position);
        return properParaList;
    }

    /**
     * para-decl           -> type declarator
     */
    private ParaDecl parseParaDecl() throws SyntaxError {
        SourcePosition position = newAndStart();

        Type typeAST = parseType();
        Decl declAST = parseDeclarator(typeAST);

        finish(position);
        return new ParaDecl(declAST.T, declAST.I, position);
    }

    /**
     * arg-list            -> "(" proper-arg-list? ")"
     */
    private List parseArgList() throws SyntaxError {
        List argList = null;

        match(Token.LPAREN);
        if (primaryExprFirstSet.contains(currentToken.kind)) {
            argList = parseProperArgList();
        } else {
            argList = new EmptyArgList(dummyPos);
        }
        match(Token.RPAREN);

        return argList;
    }

    /**
     * proper-arg-list     -> arg ( "," arg )*
     */
    private List parseProperArgList() throws SyntaxError {
        List properArgList = null;
        SourcePosition position = newAndStart();

        Arg arg = parseArg();
        if (Token.COMMA == currentToken.kind) {
            accept();
            List subProperArgList = parseProperArgList();
            properArgList = new ArgList(arg, subProperArgList, position);
        } else {
            properArgList = new ArgList(arg, new EmptyArgList(dummyPos), position);
        }

        finish(position);
        return properArgList;
    }

    /**
     * arg                 -> expr
     */
    private Arg parseArg() throws SyntaxError {
        SourcePosition position = newAndStart();
        Expr expr = parseExpr();
        finish(position);
        return new Arg(expr, position);
    }

}

