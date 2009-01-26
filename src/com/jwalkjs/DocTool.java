package com.jwalkjs;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class DocTool implements FileFilter {

	private static Context cx;
	private static ScriptableObject scope;
	private static File appDir;

	private static File doctoolDir;
	private static File commentParser;
	private static File templateSet;
	private static File outDir;
	private static File sourceJS;

	private static Scriptable commentParserObj;
	private static Function commentParserFunc;

	public static void main(String[] args) {
		try {

			appDir = new File(System.getProperty("java.class.path"));
			appDir = appDir.getParentFile().getCanonicalFile();
			doctoolDir = new File(appDir, "doctool");

			// Parse arguments
			if( !parseArguments(args) ){
				return;
			}

			String parser = null;
			String tmpl = null;
			File output = null;

			// Load doc parser
			loadDocParser();

			// Parse all files
			if( sourceJS.isDirectory() ){
				System.out.println("Parsing source files in '"+ sourceJS.getPath() +"/' ...");
			} else {
				System.out.println("Parsing '"+ sourceJS.getName() +"' ...");
			}
			ArrayList<ScriptFile> scripts = parseSourceFiles(sourceJS);

			// Run comment parser on all files
			System.out.println("Reading comments...");
			runCommentParser(scripts);

			// Parse templates and output
			System.out.println("Running templates...");
			Template templates = new Template( templateSet );
			templates.dispatch(outDir, scripts.toArray( new ScriptFile[ scripts.size() ] ) );
			templates.close();

			Context.exit();

			System.out.println("Done!");

		} catch( Exception ex ){
			System.err.println("\nAn error occurred!");
			System.err.println(ex.getMessage());
		}
	}

	/**
	 * Parse a JavaScript source file or directory
	 * @param source A source file or directory
	 */
	private static ArrayList<ScriptFile> parseSourceFiles(File source) throws Exception {
		ArrayList<ScriptFile> parsed = new ArrayList<ScriptFile>();

		// Get sources
		File[] sources;
		if( source.isDirectory() ){
			sources = source.listFiles( new DocTool() );
		}
		else{
			sources = new File[1];
			sources[0] = source;
		}

		// Parse source files
		File file;
		for( int i = 0; i < sources.length; i++ ){
			file = sources[i];

			if( file.isDirectory() ){
				ArrayList<ScriptFile> scripts = parseSourceFiles( file );
				parsed.addAll( scripts );
			}
			else {
				try {
					ScriptFile script = JWalkParser.parseFile(file, true);
					parsed.add(script);
				} catch( IOException ex ){
					throw new Exception("Could not read or process the file '"+ file.getAbsolutePath() +"'");
				}
			}

		}

		return parsed;
	}

	/**
	 * Run the comment parser on all parsed scripts
	 * @param scripts
	 */
	private static void runCommentParser( ArrayList<ScriptFile> scripts ){
		ScriptFile script;
		for ( int i = 0; i < scripts.size(); i++ ){
			script = scripts.get(i);

			Element global = script.global.element;
			Element[] elements = global.getAllChildren();

			// Call comment parser for each JS element in the source
			ElementDoc doc;
			for(int n = 0; n < elements.length; n++){
				doc = elements[n].generateDocElement(cx, scope);
				commentParserFunc.call(cx, scope, commentParserObj, new Object[]{ doc, elements[n] });
			}
		}
	}

	/**
	 * Parse the command line arguments
	 * @returns FALSE if program execution should exit after this method call.
	 */
	private static boolean parseArguments(String[] args) {

		File parserDir = new File(doctoolDir, "parsers");
		File tmplDir = new File(doctoolDir, "templates");

		String arg = "";
		String next;
		File file;
		for(int i = 0; i < args.length; i++){
			arg = args[i];

			// Get next argument in the command
			next = null;
			if( i + 1 < args.length ){
				next = args[i + 1];
			}

			// Parse through command line arguments
			if( arg.equals("--list") ){

				System.out.println("== Available Template sets ==");
				listTemplates();

				System.out.println("== Available Comment Parsers ==");
				listParsers();

				return false;

			} else if( arg.equals("--help") ){
				printUsage();
				return false;

			}else if( arg.equals("--parser") || arg.equals("-p") ){

				if( next == null ){
					System.err.println("You must define a parser file when you use the '"+ arg +"' flag.");
					return false;
				}

				// Check existence of the file
				if( (file = new File(next)).exists() ){
					System.out.println("Using parser: "+ file.getAbsolutePath() );
				} else if ( (file = new File(parserDir, next)).exists() ){
					System.out.println("Using parser: "+ next );
				} else{
					System.err.println("Could not find the parser file '"+ next +"'." +
							"Try putting it in the doctool parser directory: '"+ parserDir.getAbsolutePath() +"'");
					return false;
				}

				i++;
				commentParser = file;

			} else if( arg.equals("--tmpl") || arg.equals("-t") ){
				if( next == null ){
					System.err.println("You must define a template directory when you use the '"+ arg +"' flag.");
					return false;
				}

				// Check existence of the file
				if( (file = new File(next)).exists() ){
					System.out.println("Using templates: "+ file.getAbsolutePath() );
				} else if ( (file = new File(tmplDir, next)).exists() ){
					System.out.println("Using templates: "+ next );
				} else{
					System.err.println("Could not find the template set directory '"+ next +"'." +
							"Try putting it in the doctool templates directory: '"+ tmplDir.getAbsolutePath() +"'");
					return false;
				}

				i++;
				templateSet = file;

			} else if( arg.equals("--out") || arg.equals("-o") ){
				if( next == null ){
					System.err.println("You must define a out directory when you use the '"+ arg +"' flag.");
					return false;
				}

				// Not a directory?
				file = new File( next );
				if( file.exists() && !file.isDirectory() ){
					System.err.println("'"+ next +"' is not a directory.");
					return false;
				}

				// Create directory
				if( !file.exists() ){
					file.mkdirs();
				}

				i++;
				outDir = file;

			} else { // Source File

				file = new File(arg);
				if( !file.exists() ){
					System.err.println("'"+ arg +"' is not a file or directory.");
				}

				sourceJS = file;

				break;
			}
		}

		// If sourceJS is not set
		if( sourceJS == null ){
			System.err.println("You did not specify a source file or directory.");
			return false;
		}

		// If output directory is not set
		if( outDir == null ){
			System.err.println("You did not specify an output directory.");
			return false;
		}

		// Default Comment Parser
		if( commentParser == null ){
			commentParser = new File(parserDir, "default.js");

			if( !commentParser.exists() ){
				System.err.println("The default comment parser 'default.js' does not exist in the " +
						"doctool parsers directory '"+ parserDir.getAbsolutePath() +"'");
				return false;
			}
		}

		// Default Template set
		if( templateSet == null ){
			templateSet = new File(tmplDir, "default");

			if( !templateSet.exists() ){
				System.err.println("The default template set 'default' does not exist in the " +
						"doctool templates directory '"+ tmplDir.getAbsolutePath() +"'");
				return false;
			}
		}

		return true;
	}

	/**
	 * List all the comment parsers and their description in the doctool directory
	 */
	public static void listParsers(){
		System.out.println("Not implemented yet");
	}

	/**
	 * List all the Template sets and their description in the doctool directory
	 */
	public static void listTemplates(){
		System.out.println("Not implemented yet");
	}

	/**
	 * Print the command line usage for this class
	 */
	private static void printUsage(){
		StringBuffer out = new StringBuffer();

		out.append("Generates documentation from your JavaScript source files.\n\n");
		out.append("usage: jwalk doc [options] -o path path [path...path]\n");
		out.append("    --help       : Display this help and exits \n");
		out.append("    --list       : Lists all document parsers and templates that are\n");
		out.append("                   loaded in the doctool directory\n");
		out.append("    -p, --parser : The doc parser name or file.\n");
		out.append("    -t, --tmpl   : The template set name or directory path.\n");
		out.append("    -o, --out    : The output directory.\n");
		out.append("    <path>       : The source file or directory.\n");

		System.out.println(out.toString());
	}


	/**
	 * Load a JS file and return the first object
	 */
	private static void loadDocParser()
		throws Exception {

		cx = (new ContextFactory()).enterContext();
		scope = cx.initStandardObjects();

		// Add helper JS methods
		JSHelpers.load(scope);
		scope.associateValue("output_writer", System.out);

		cx.evaluateReader(scope, new FileReader(commentParser), commentParser.getAbsolutePath(), 1, null);
		commentParserObj = (Scriptable) scope.get("DocParser", scope);
		commentParserFunc = (Function) ScriptableObject.getProperty(commentParserObj, "parseElement");
	}

	/**
	 * Filter that only shows *.js files and directories
	 * @param pathname The file to be tested
	 * @return
	 */
	public boolean accept(File pathname) {
		return ( pathname.isDirectory() || pathname.getName().toLowerCase().matches("\\.js$") );
	}

}