package com.jwalkjs;

import org.mozilla.javascript.ScriptableObject;

public class JWalk {

	public static String version = "0.1a";

	public static void main(String[] args)
		throws Exception {

		if(args.length == 0 || args[0].equals("--help")){
			printUsage();
			return;
		}

		// Get command and remove from args
		String command = args[0];
		String[] cmdArgs = new String[args.length - 1];
		System.arraycopy(args, 1, cmdArgs, 0, cmdArgs.length);

		// Supported command?
		if(command.equals("inspect")){
			InspectFile.main(cmdArgs);
			return;
		} else if(command.equals("tree")){
			PrintTree.main(cmdArgs);
			return;
		} else if(command.equals("doc")){
			DocTool.main(cmdArgs);
			return;
		} else if(command.equals("main")){
			JWalkParser.main(cmdArgs);
			return;
		} else if(command.equals("version")){
			printVersion();
			return;
		} else if(command.equals("help")){
			printUsage();
			return;
		}

		System.out.println("\nUnknown command: '"+ command +"'\n");
		printUsage();
	}

	/**
	 * Print the command line usage for this class
	 */
	private static void printUsage(){
		StringBuffer out = new StringBuffer();

		out.append("usage: jwalk <command> [args]\n");
		out.append("JWalk JavaScript parsing tool, version "+ version +"\n");
		out.append("Type 'jwalk <command> -help' to get help on a command.\n\n");
		out.append("Available commands:\n");
		out.append("   help      : Shows this help text.\n");
		out.append("   inspect   : Prints a table of global elements for each file in the argument list.\n");
		out.append("   tree      : Prints a hierarchy tree of JS elements in a file.\n");
		out.append("   doc       : Runs the JavaScript files through the JWalk documentation engine.\n");
		out.append("   version   : Prints the JWalk version.\n");

		System.out.println(out.toString());
	}

	/**
	 * Print the version information for JWalk
	 */
	private static void printVersion(){
		System.out.println("JWalk JavaScript inspection tool\n\tversion "+ version);
	}
}