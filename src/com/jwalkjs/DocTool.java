package com.jwalkjs;

import java.io.*;
import java.util.*;
import org.mozilla.javascript.*;

public class DocTool {

	private static Context cx;
	private static Scriptable scope;
	private static File appDir;

	public static void main(String[] args)
		throws Exception{
	
		appDir = new File(System.getProperty("java.class.path"));
		appDir = appDir.getParentFile().getCanonicalFile();

		if(args.length == 0 || args[0].equals("-help")){
			printUsage();
			return;
		}

		String parser = null;
		String tmpl = null;
		File output = null;
		ArrayList<String> sources = new ArrayList();

		// Parse arguments
		String arg = "";
		String argSet = "";
		for(int i = 0; i < args.length; i++){
			arg = args[i];

			if(arg.equals("-s")){
				argSet = arg;
			} else if(arg.equals("-o")){
				argSet = "";
				output = new File(arg);
				if(output.exists() && output.isDirectory()){
					System.out.println("'"+ arg +"' is not a directory");
					return;
				}
			} else if(argSet.equals("-s")){
				File source = new File(arg);
				if(source.exists()){
					sources.add(arg);
				}
				else{
					System.out.println("The file or directory '"+ arg +"' does not exist...SKIPPING");
				}
			}
		}

		if(sources.size() == 0){
			System.out.println("No source files were specified.");
			printUsage();
			return;
		}

		// Load doc parser
		File parserFile = new File(appDir.getPath() +"/doctool/parsers/default.js");
		loadJS(parserFile.getPath());
		Scriptable docparser = (Scriptable) scope.get("DocParser", scope);
		Function parserFunc = (Function) ScriptableObject.getProperty(docparser, "parseElement");
		
		// Parser all JS source files and run them through the doc parser
		ArrayList<Element> parsed = new ArrayList();
		ElementMeta globalDoc;
		for(int i = 0; i < sources.size(); i++){
			Element global = JWalkParser.parseFile(sources.get(i), true);
			globalDoc = new ElementMeta(cx, scope, global);
			Element[] elements = global.getAllChildren();
			
			// Call parser object for each JS element in the source
			Element elem;
			ElementMeta doc;
			for(int n = 0; n < elements.length; n++){
				elem = elements[n];
				doc = new ElementMeta(cx, scope, elem);  // Create a scriptable document object
				
				parserFunc.call(cx, scope, docparser, new Object[]{ doc, elem });
			}

			parsed.add(global);
		
			// Load template engine
			/*FileReader tmplFile = new FileReader(appDir.getPath() +"/doctool/templates/default/index.tmpl");
			Template tmplParser = new Template(tmplFile, "index.tmpl");
			Map<String, Object> tmplContext = new HashMap<String, Object>();
			tmplContext.put("global", globalDoc);
			tmplContext.put("Hello", "world");
			
			FileWriter tmplOut = new FileWriter( new File(appDir.getPath() +"/doctool/out/index.html") ); 
			tmplParser.expand(tmplContext, tmplOut);*/
			
			Map<String, Object> globals = new HashMap<String, Object>();
			globals.put("elements", globalDoc);;
			Template templates = new Template( appDir.getPath() +"/doctool/templates/default/" );
			templates.parse("index.tmpl", appDir.getPath() +"/doctool/out/index.html", globals);
		}
		
		Context.exit();
	}

	/**
	 * Print the command line usage for this class
	 */
	private static void printUsage(){
		StringBuffer out = new StringBuffer();

		out.append("Generates documentation from your JavaScript source files.\n\n");
		out.append("usage: jwalk doc [options] -o path -s path [path...path] [-e path...path]\n");
		out.append("    -list   : Lists all document parsers and templates that are\n");
		out.append("              loaded in the doctool directory\n");
		out.append("    -parser : The doc parser name or file.\n");
		out.append("    -tmpl   : The template set name or directory path.\n");
		out.append("    -o      : The output directory.\n");
		out.append("    -s      : The source files and directories.\n");
		out.append("    -e      : Files and directories to exclude.\n");

		System.out.println(out.toString());
	}
	
	
	/**
	 * Load a JS file and return the first object 
	 */
	private static void loadJS(String path)
		throws Exception {

		cx = (new ContextFactory()).enterContext();
		scope = cx.initStandardObjects();
		
		// Add helper JS methods
		ScriptableObject.defineClass(scope, ParserHelpers.class);
		Scriptable helpers = cx.newObject(scope, "ParserHelpers", new Object[0]);
		scope.put("jwalk", scope, helpers);
		
		cx.evaluateReader(scope, new FileReader(path), path, 1, null);
	}
	
}