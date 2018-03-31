/**
 * *	Scanner.java
 **/

package VC.Scanner;

import VC.ErrorReporter;

public final class Scanner {

    private SourceFile sourceFile;
    private boolean debug;

    private ErrorReporter errorReporter;
    private StringBuffer currentSpelling;
    private char currentChar;
    private SourcePosition currentPosition;

    // =========================================================

    public Scanner(SourceFile source, ErrorReporter reporter) {
        sourceFile = source;
        errorReporter = reporter;
        currentChar = sourceFile.getNextChar();
        debug = false;

        // you may initialise your counters for line and column numbers here
        currentPosition = new SourcePosition(1, 1, 0);
    }

    private void countLineAndColumn() {
        switch (currentChar) {
            case '\n':
                currentPosition.lineFinish++;
                currentPosition.charFinish = 0;
                break;
            case '\t':
                currentPosition.charFinish = (currentPosition.charFinish - currentPosition.charFinish % 8) + 8;
                break;
            default:
                currentPosition.charFinish++;
        }
    }

    public void enableDebugging() {
        debug = true;
    }

    /**
     * purely read next char and count line and column
     */
    private void readNextChar() {
        countLineAndColumn();// current char has been handled (accept or skip)
        currentChar = sourceFile.getNextChar();
    }

    // accept gets the next character from the source program.

    private void accept() {
        // you may save the lexeme of the current token incrementally here
        currentSpelling.append(currentChar);
        // you may also increment your line and column counters here
        readNextChar();

    }

    // inspectChar returns the n-th character after currentChar
    // in the input stream.
    //
    // If there are fewer than nthChar characters between currentChar
    // and the end of file marker, SourceFile.eof is returned.
    //
    // Both currentChar and the current position in the input stream
    // are *not* changed. Therefore, a subsequent call to accept()
    // will always return the next char after currentChar.

    private char inspectChar(int nthChar) {
        return sourceFile.inspectChar(nthChar);
    }

    private int nextToken() {
        // Tokens: separators, operators, literals, identifiers and keyworods
        switch (currentChar) {
            // separators
            case '{':
                accept();
                return Token.LCURLY;
            case '}':
                accept();
                return Token.RCURLY;
            case '(':
                accept();
                return Token.LPAREN;
            case ')':
                accept();
                return Token.RPAREN;
            case '[':
                accept();
                return Token.LBRACKET;
            case ']':
                accept();
                return Token.RBRACKET;
            case ';':
                accept();
                return Token.SEMICOLON;
            case ',':
                accept();
                return Token.COMMA;
            // operators
            case '+':
                accept();
                return Token.PLUS;
            case '-':
                accept();
                return Token.MINUS;
            case '*':
                accept();
                return Token.MULT;
            case '/':
                accept();
                return Token.DIV;
            case '!':
                accept();
                if (currentChar != '=') {
                    return Token.NOT;
                } else {
                    accept();
                    return Token.NOTEQ;
                }
            case '=':
                accept();
                if (currentChar != '=') {
                    return Token.EQ;
                } else {
                    accept();
                    return Token.EQEQ;
                }
            case '<':
                accept();
                if (currentChar != '=') {
                    return Token.LT;
                } else {
                    accept();
                    return Token.LTEQ;
                }
            case '>':
                accept();
                if (currentChar != '=') {
                    return Token.GT;
                } else {
                    accept();
                    return Token.GTEQ;
                }
            case '&':
                accept();
                if (currentChar == '&') {
                    accept();
                    return Token.ANDAND;
                } else {
                    return Token.ERROR;
                }
            case '|':
                accept();
                if (currentChar == '|') {
                    accept();
                    return Token.OROR;
                } else {
                    return Token.ERROR;
                }
                // literals

                // numbers
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                accept();
                while (Character.isDigit(currentChar)) {
                    accept();
                }// 1
                switch (currentChar) {
                    case '.':
                        accept();// 1.
                        switch (currentChar) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':// 1.1
                                accept();
                                while (Character.isDigit(currentChar)) {
                                    accept();
                                }// 1.111
                                switch (currentChar) {
                                    case 'e':
                                    case 'E':// 1.1e
                                        return processExponent(Token.FLOATLITERAL);
                                    default:
                                        return Token.FLOATLITERAL;// 1.1
                                }
                            case 'e':
                            case 'E':
                                return processExponent(Token.FLOATLITERAL);// 1.e
                            default:
                                return Token.FLOATLITERAL;// 1.
                        }
                    case 'e':
                    case 'E':
                        return processExponent(Token.INTLITERAL);// 1e
                    default:// pure integer
                        return Token.INTLITERAL;
                }
                // numbers-float_literal
            case '.':
                accept();// .
                switch (currentChar) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':// .1
                        accept();
                        while (Character.isDigit(currentChar)) {
                            accept();
                        }// .111
                        switch (currentChar) {
                            case 'e':
                            case 'E':// .1e
                                return processExponent(Token.FLOATLITERAL);
                            default:
                                return Token.FLOATLITERAL;// .1
                        }
                    default:
                        return Token.ERROR;// .e is illegal
                }
                // strings
            case '"':
                readNextChar();
                while (currentChar != SourceFile.eof) {
                    switch (currentChar) {
                        case '\\':// \
                            /**
                             * @see <a href="https://docs.oracle.com/javase/tutorial/java/data/characters.html">Escape Sequences</a>
                             */
                            switch (inspectChar(1)) {
                                case 't':// \t
                                    readNextChar();
                                    readNextChar();
                                    currentSpelling.append('\t');
                                    break;
                                case 'b':
                                    readNextChar();
                                    readNextChar();
//                                    currentSpelling.append('\b');// according to the given solution, ignore \b
                                    break;
                                case 'n':
                                    readNextChar();
                                    readNextChar();
                                    currentSpelling.append('\n');
                                    break;
                                case 'r':
                                    readNextChar();
                                    readNextChar();
                                    currentSpelling.append('\n');// according to the given solution, \r == \n
                                    break;
                                case 'f':
                                    readNextChar();
                                    readNextChar();
                                    currentSpelling.append('\f');
                                    break;
                                case '\'':
                                    readNextChar();
                                    readNextChar();
                                    currentSpelling.append('\'');
                                    break;
                                case '\"':
                                    readNextChar();
                                    readNextChar();
                                    currentSpelling.append('\"');
                                    break;
                                case '\\':
                                    readNextChar();
                                    readNextChar();
                                    currentSpelling.append('\\');
                                    break;
                                default:
                                    // illegal escape char
                                    // according to the given solution, just keep the string and report the error
                                    String illegalEscapeChar = "\\";// \
                                    accept();
                                    illegalEscapeChar += currentChar;//e.g. \p
                                    errorReporter.reportError("%: illegal escape character", illegalEscapeChar.toString(), currentPosition);
                                    accept();
                            }
                            break;
                        case '\n':// string must be ONE line, otherwise they are TWO strings
                            errorReporter.reportError("%: unterminated string", currentSpelling.toString(), new SourcePosition(currentPosition.lineFinish, currentPosition.charStart, currentPosition.charStart));
                            // accept the string, do as coming to end and a \n coming with the string
                            return Token.STRINGLITERAL;
                        case SourceFile.eof:
                            countLineAndColumn();
                            errorReporter.reportError("%: unterminated string", currentSpelling.toString(), new SourcePosition(currentPosition.lineFinish, currentPosition.charStart, currentPosition.charStart));
                            break;
                        case '"':// end of string
                            readNextChar();
                            return Token.STRINGLITERAL;
                        default:
                            accept();

                    }
                }
            case SourceFile.eof:
                currentSpelling.append(Token.spell(Token.EOF));
                countLineAndColumn();// accept, but not adding anymore
                return Token.EOF;
            default:
                if (Character.isLetter(currentChar) || currentChar == '_') {// first char must be letter or '_'
                    accept();
                    while (Character.isLetterOrDigit(currentChar) || currentChar == '_') {
                        accept();
                    }
                    Token keywords = new Token(Token.ID, currentSpelling.toString(), currentPosition);
                    if (keywords.kind != Token.ID) {// is keywords
                        return keywords.kind;
                    } else if ("true".equals(currentSpelling.toString()) || "false".equals(currentSpelling.toString())) {
                        return Token.BOOLEANLITERAL;
                    } else {
                        return Token.ID;
                    }
                }
                break;
        }

        accept();
        return Token.ERROR;
    }

    /**
     * when comes to 'e', recognise the coming "e" sequence
     */
    private int processExponent(int intOrFloatBeforeE) {
        switch (inspectChar(1)) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                accept();// accept 'e'
                while (Character.isDigit(currentChar)) {
                    accept();
                }
                return Token.FLOATLITERAL;// e1
            case '+':
            case '-':
                if (Character.isDigit(inspectChar(2))) {
                    accept();// accept 'e'
                    accept();// accept '+' or '-'
                    while (Character.isDigit(currentChar)) {
                        accept();
                    }
                    return Token.FLOATLITERAL;// e+1
                }
            default:// 'e' is not an Exponent, just a letter
                return intOrFloatBeforeE;
        }
    }

    void skipSpaceAndComments() {
        while (currentChar != SourceFile.eof) {
            switch (currentChar) {
                case '/':
                    switch (inspectChar(1)) {
                        case '/':// a "//" comment
                            do {
                                readNextChar();
                            } while (currentChar != '\n' && currentChar != SourceFile.eof);
                            break;
                        case '*':// a "/*" comment
                            readNextChar();// read '*' in "/*"
                            boolean isMultiLineCommentEndNormally = false;
                            do {
                                readNextChar();
                                switch (currentChar) {
                                    case '*':
                                        switch (inspectChar(1)) {
                                            case '/':// ends with "*/"
                                                readNextChar();
                                                isMultiLineCommentEndNormally = true;
                                                break;
                                            default:
                                                break;
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }
                            while (!isMultiLineCommentEndNormally && currentChar != SourceFile.eof);
                            if (!isMultiLineCommentEndNormally) {
                                errorReporter.reportError("% unterminated comment", ":", new SourcePosition(currentPosition.lineStart, currentPosition.charStart, currentPosition.charStart));
                                return;
                            }
                            break;
                        default:// is a div token
                            return;
                    }
                case ' ':
                case '\t':
                case '\n':// space out of a comment
                    readNextChar();
                    setPositionCurrent();
                    break;
                default:// not a comment nor space char
                    return;
            }
        }
    }

    /**
     * set currentPosition to currentChar (to be handled)
     */
    private void setPositionCurrent() {
        currentPosition.lineStart = currentPosition.lineFinish;
        currentPosition.charStart = currentPosition.charFinish;
        currentPosition.charStart++;
    }

    public Token getToken() {
        skipSpaceAndComments();// skip white space and comments
        currentSpelling = new StringBuffer();// clear to store next token
        setPositionCurrent();
        int kind = nextToken();
        SourcePosition tokenPosition = new SourcePosition(currentPosition.lineFinish, currentPosition.charStart, currentPosition.charFinish);
        Token token = new Token(kind, currentSpelling.toString(), tokenPosition);

        // * do not remove these three lines
        if (debug)
            System.out.println(token);
        return token;
    }

}
