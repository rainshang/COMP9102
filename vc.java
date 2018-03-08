/*+*
 *
 * vc.java           67/2/2017
 * 
 * Jingling Xue, CSE, UNSW, Sydney NSW 2052, Australia.
 *
 *+*/

package VC;

import VC.Scanner.Scanner;
import VC.Scanner.SourceFile;
import VC.Scanner.Token;

public class vc {

    private static Scanner scanner;
    private static ErrorReporter reporter;
    private static Token currentToken;
    private static String inputFilename;


    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.print("\nPlease type the vc file you want to scan:");
            java.util.Scanner consoleScanner = new java.util.Scanner(System.in);
            inputFilename = "Scanner/" + consoleScanner.nextLine() + ".vc";
        } else {
            inputFilename = args[0];
        }
        System.out.println("======= The VC compiler =======");

        SourceFile source = new SourceFile(inputFilename);

        reporter = new ErrorReporter();
        scanner = new Scanner(source, reporter);
        scanner.enableDebugging();

        do
            currentToken = scanner.getToken();
        while (currentToken.kind != Token.EOF);
    }
}
