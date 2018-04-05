/***
 * *
 * * Recogniser.java            
 * *
 ***/

/* At this stage, this parser accepts a subset of VC defined	by
 * the following grammar. 
 *
 * You need to modify the supplied parsing methods (if necessary) and 
 * add the missing ones to obtain a parser for the VC language.
 *
 * (17---March---2017)

program       -> func-decl

// declaration

func-decl     -> void identifier "(" ")" compound-stmt

identifier    -> ID

// statements 
compound-stmt -> "{" stmt* "}" 
stmt          -> continue-stmt
    	      |  expr-stmt
continue-stmt -> continue ";"
expr-stmt     -> expr? ";"

// expressions 
expr                -> assignment-expr
assignment-expr     -> additive-expr
additive-expr       -> multiplicative-expr
                    |  additive-expr "+" multiplicative-expr
multiplicative-expr -> unary-expr
	            |  multiplicative-expr "*" unary-expr
unary-expr          -> "-" unary-expr
		    |  primary-expr

primary-expr        -> identifier
 		    |  INTLITERAL
		    | "(" expr ")"
*/

package VC.Recogniser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import VC.ErrorReporter;
import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;

public class Recogniser {

    private Scanner scanner;
    private ErrorReporter errorReporter;
    private Token currentToken;

    private Set<Integer> primitiveTypeFirstSet;
    private Set<Integer> primaryExprFirstSet;

    public Recogniser(Scanner lexer, ErrorReporter reporter) {
        scanner = lexer;
        errorReporter = reporter;

        currentToken = scanner.getToken();

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
            currentToken = scanner.getToken();
        } else {
            syntacticError("\"%\" expected here", Token.spell(tokenExpected));
        }
    }

    // accepts the current token and fetches the next
    private void accept() {
        currentToken = scanner.getToken();
    }

    private void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
        SourcePosition pos = currentToken.position;
        errorReporter.reportError(messageTemplate, tokenQuoted, pos);
        throw (new SyntaxError());
    }


// ========================== PROGRAMS ========================

    public void parseProgram() {
        try {
            while (Token.EOF != currentToken.kind) {
                parseType();
                parseIdent();
                if (Token.LPAREN == currentToken.kind) {
                    parseRemainingFuncDecl();
                } else {
                    parseRemainingVarDecl();
                }

            }
            if (Token.EOF != currentToken.kind) {
                syntacticError("\"%\" wrong result type for a function", currentToken.spelling);
            }
        } catch (SyntaxError s) {
        }
    }

// ========================== DECLARATIONS ========================

    private void parseRemainingFuncDecl() throws SyntaxError {
        parseParaList();
        parseCompoundStmt();
    }

    private void parseRemainingVarDecl() throws SyntaxError {
        if (Token.LBRACKET == currentToken.kind) {
            accept();
            if (Token.INTLITERAL == currentToken.kind) {
                parseIntLiteral();
            }
            match(Token.RBRACKET);
        }
        if (Token.EQ == currentToken.kind) {
            accept();
            parseInitialiser();
        }
        while (Token.COMMA == currentToken.kind) {
            accept();
            parseInitDeclarator();
        }
        match(Token.SEMICOLON);
    }

    private void parseVarDecl() throws SyntaxError {
        parseType();
        parseInitDeclaratorList();
        match(Token.SEMICOLON);
    }

    private void parseInitDeclaratorList() throws SyntaxError {
        parseInitDeclarator();
        while (Token.COMMA == currentToken.kind) {
            accept();
            parseInitDeclarator();
        }
    }

    private void parseInitDeclarator() throws SyntaxError {
        parseDeclarator();
        if (Token.EQ == currentToken.kind) {
            accept();
            parseInitialiser();
        }
    }

    private void parseDeclarator() throws SyntaxError {
        parseIdent();
        if (Token.LBRACKET == currentToken.kind) {
            accept();
            if (Token.INTLITERAL == currentToken.kind) {
                parseIntLiteral();
            }
            match(Token.RBRACKET);
        }
    }

    private void parseInitialiser() throws SyntaxError {
        if (Token.LCURLY == currentToken.kind) {
            accept();
            parseExpr();
            while (Token.COMMA == currentToken.kind) {
                accept();
                parseExpr();
            }
            match(Token.RCURLY);
        } else {
            parseExpr();
        }
    }

// ======================= PRIMITIVE TYPES ==============================

    private void parseType() throws SyntaxError {
        if (primitiveTypeFirstSet.contains(currentToken.kind)) {
            accept();
        } else {
            syntacticError("primitive types expected here", "");
        }

    }
// ======================= IDENTIFIERS ======================

    // Call parseIdent rather than match(Token.ID).
    // In Assignment 3, an Identifier node will be constructed in here.
    private void parseIdent() throws SyntaxError {
        if (Token.ID == currentToken.kind) {
            currentToken = scanner.getToken();
        } else
            syntacticError("identifier expected here", "");
    }

// ======================= STATEMENTS ==============================

    private void parseCompoundStmt() throws SyntaxError {
        match(Token.LCURLY);
        parseStmtList();
        match(Token.RCURLY);
    }

    // Here, a new nontermial has been introduced to define { stmt } *
    private void parseStmtList() throws SyntaxError {
        while (Token.RCURLY != currentToken.kind) {
            if (primitiveTypeFirstSet.contains(currentToken.kind)) {// fits var-decl
                parseVarDecl();
            } else {
                break;
            }
        }
        while (currentToken.kind != Token.RCURLY) {
            parseStmt();
        }
    }

    private void parseStmt() throws SyntaxError {
        switch (currentToken.kind) {
            case Token.LCURLY:
                parseCompoundStmt();
                break;
            case Token.IF:
                parseIfStmt();
                break;
            case Token.FOR:
                parseForStmt();
                break;
            case Token.WHILE:
                parseWhileStmt();
                break;
            case Token.BREAK:
                parseBreakStmt();
                break;
            case Token.CONTINUE:
                parseContinueStmt();
                break;
            case Token.RETURN:
                parseReturnStmt();
                break;
            default:
                parseExprStmt();
                break;

        }
    }

    private void parseIfStmt() throws SyntaxError {
        accept();
        match(Token.LPAREN);
        parseExpr();
        match(Token.RPAREN);
        parseStmt();
        if (Token.ELSE == currentToken.kind) {
            accept();
            parseStmt();
        }
    }

    private void parseForStmt() throws SyntaxError {
        accept();
        match(Token.LPAREN);
        for (int i = 0; i < 3; i++) {
            if (primaryExprFirstSet.contains(currentToken.kind)) {
                parseExpr();
            }
            if (i < 2) {
                match(Token.SEMICOLON);
            }
        }
        match(Token.RPAREN);
        parseStmt();
    }

    private void parseWhileStmt() throws SyntaxError {
        accept();
        match(Token.LPAREN);
        parseExpr();
        match(Token.RPAREN);
        parseStmt();
    }

    private void parseBreakStmt() throws SyntaxError {
        accept();
        match(Token.SEMICOLON);
    }

    private void parseContinueStmt() throws SyntaxError {
        accept();
        match(Token.SEMICOLON);
    }

    private void parseReturnStmt() throws SyntaxError {
        accept();
        if (primaryExprFirstSet.contains(currentToken.kind)) {
            parseExpr();
        }
        match(Token.SEMICOLON);
    }

    private void parseExprStmt() throws SyntaxError {
        if (primaryExprFirstSet.contains(currentToken.kind)) {
            parseExpr();
        }
        match(Token.SEMICOLON);
    }


// ======================= OPERATORS ======================

    // Call acceptOperator rather than accept().
    // In Assignment 3, an Operator Node will be constructed in here.

    private void acceptOperator() throws SyntaxError {
        currentToken = scanner.getToken();
    }


// ======================= EXPRESSIONS ======================

    private void parseExpr() throws SyntaxError {
        parseAssignExpr();
    }


    /**
     * A -> (B op )* B is A -> B, A -> B op B, A -> B op B op B, ... equals
     * A -> B (op B)*
     */
    private void parseAssignExpr() throws SyntaxError {
        parseCondOrExpr();
        while (Token.EQ == currentToken.kind) {
            acceptOperator();
            parseCondOrExpr();
        }
    }

    /**
     * A -> B | A op B is left recursion
     * let A -> B A', A' -> e | op B, thus,
     * A -> B (Op B)*
     */
    private void parseCondOrExpr() throws SyntaxError {
        parseCondAndExpr();
        while (Token.OROR == currentToken.kind) {
            acceptOperator();
            parseCondAndExpr();
        }
    }

    private void parseCondAndExpr() throws SyntaxError {
        parseEqualityExpr();
        while (Token.ANDAND == currentToken.kind) {
            acceptOperator();
            parseEqualityExpr();
        }
    }

    private void parseEqualityExpr() throws SyntaxError {
        parseRelExpr();
        while (Token.EQEQ == currentToken.kind
                || Token.NOTEQ == currentToken.kind) {
            acceptOperator();
            parseRelExpr();
        }
    }

    private void parseRelExpr() throws SyntaxError {
        parseAdditiveExpr();
        while (Token.LT == currentToken.kind
                || Token.LTEQ == currentToken.kind
                || Token.GT == currentToken.kind
                || Token.GTEQ == currentToken.kind) {
            acceptOperator();
            parseAdditiveExpr();
        }
    }

    private void parseAdditiveExpr() throws SyntaxError {
        parseMultiplicativeExpr();
        while (Token.PLUS == currentToken.kind
                || Token.MINUS == currentToken.kind) {
            acceptOperator();
            parseMultiplicativeExpr();
        }
    }

    private void parseMultiplicativeExpr() throws SyntaxError {
        parseUnaryExpr();
        while (Token.MULT == currentToken.kind
                || Token.DIV == currentToken.kind) {
            acceptOperator();
            parseUnaryExpr();
        }
    }

    private void parseUnaryExpr() throws SyntaxError {
        switch (currentToken.kind) {
            case Token.PLUS:
            case Token.MINUS:
            case Token.NOT:
                acceptOperator();
                parseUnaryExpr();
                break;

            default:
                parsePrimaryExpr();
        }
    }

    private void parsePrimaryExpr() throws SyntaxError {
        switch (currentToken.kind) {
            case Token.ID:
                parseIdent();
                if (Token.LPAREN == currentToken.kind) {
                    parseArgList();
                } else if (Token.LBRACKET == currentToken.kind) {
                    accept();
                    parseExpr();
                    match(Token.RBRACKET);
                } else {
                    break;
                }
                break;
            case Token.LPAREN:
                accept();
                parseExpr();
                match(Token.RPAREN);
                break;
            case Token.INTLITERAL:
                parseIntLiteral();
                break;
            case Token.FLOATLITERAL:
                parseFloatLiteral();
                break;
            case Token.BOOLEANLITERAL:
                parseBooleanLiteral();
                break;
            case Token.STRINGLITERAL:
                accept();
                break;

            default:
                syntacticError("illegal parimary expression", currentToken.spelling);

        }
    }

// ========================== LITERALS ========================

    // Call these methods rather than accept().  In Assignment 3,
    // literal AST nodes will be constructed inside these methods.

    private void parseIntLiteral() throws SyntaxError {
        if (Token.INTLITERAL == currentToken.kind) {
            currentToken = scanner.getToken();
        } else {
            syntacticError("integer literal expected here", "");
        }
    }

    private void parseFloatLiteral() throws SyntaxError {
        if (Token.FLOATLITERAL == currentToken.kind) {
            currentToken = scanner.getToken();
        } else {
            syntacticError("float literal expected here", "");
        }
    }

    void parseBooleanLiteral() throws SyntaxError {
        if (Token.BOOLEANLITERAL == currentToken.kind) {
            currentToken = scanner.getToken();
        } else {
            syntacticError("boolean literal expected here", "");
        }
    }

// ========================== PARAMETERS ========================

    private void parseParaList() throws SyntaxError {
        match(Token.LPAREN);
        if (primitiveTypeFirstSet.contains(currentToken.kind)) {
            parseProperParaList();
        }
        match(Token.RPAREN);
    }

    private void parseProperParaList() throws SyntaxError {
        parseParaDecl();
        while (Token.COMMA == currentToken.kind) {
            accept();
            parseParaDecl();
        }
    }

    private void parseParaDecl() throws SyntaxError {
        parseType();
        parseDeclarator();
    }

    private void parseArgList() throws SyntaxError {
        match(Token.LPAREN);
        if (primaryExprFirstSet.contains(currentToken.kind)) {
            parseProperArgList();
        }
        match(Token.RPAREN);
    }

    private void parseProperArgList() throws SyntaxError {
        parseExpr();
        while (currentToken.kind == Token.COMMA) {
            accept();
            parseExpr();
        }
    }

}
