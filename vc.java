/*
 * vc.java
 *
 * F O R     A S S I G N M E N T    2
 *
 *
 * Jingling Xue, CSE, UNSW, Sydney NSW 2052, Australia.
 *
 */

package VC;

import VC.Scanner.Scanner;
import VC.Scanner.SourceFile;
import VC.Recogniser.Recogniser;
import VC.Scanner.Token;

public class vc {

    private static Scanner scanner;
    private static ErrorReporter reporter;
    private static Recogniser recogniser;

    private static String inputFilename;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.print("Please type the file you want to process:");
            java.util.Scanner consoleScanner = new java.util.Scanner(System.in);
            inputFilename = "Recogniser/test_data/" + consoleScanner.nextLine() + ".vc";
        } else {
            inputFilename = args[0];
        }
        System.out.println("======= The VC compiler =======");

        SourceFile source = new SourceFile(inputFilename);

        reporter = new ErrorReporter();
        scanner = new Scanner(source, reporter);

        recogniser = new Recogniser(scanner, reporter);

        recogniser.parseProgram();

        if (reporter.numErrors == 0)
            System.out.println("Compilation was successful.");
        else
            System.out.println("Compilation was unsuccessful.");
    }
}

