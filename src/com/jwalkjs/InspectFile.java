package com.jwalkjs;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;

public class InspectFile implements FilenameFilter {

	private static String format = "raw";
	private static File currentFile = null;
	private static String currentType = "";
	private static FilenameFilter fileFilter = (FilenameFilter)new InspectFile();
	private static ArrayList<File> excludeList = new ArrayList();

	public static void main(String[] args)
		throws java.io.FileNotFoundException, java.io.IOException {

		if(args.length == 0 || args[0].equals("-help")){
			printUsage();
			return;
		}

		format = "raw";
		boolean recursive = false;

		// Get all files
		ArrayList<File> fileList = new ArrayList();
		ArrayList<File> addTo = fileList;
		for(int index = 0; index < args.length; index++){

			if(args[index].equals("-r")){
				recursive = true;
				continue;
			}

			if(args[index].equals("-csv")){
				format = "csv";
				continue;
			}

			// The rest are excludes
			if(args[index].equals("-e")){
				addTo = excludeList;
				continue;
			}

			// Add file
			String path = args[index];
			File file = new File(path);
			if(file.exists()){

				if(file.isDirectory() && addTo != excludeList){
					addTo.addAll(Arrays.asList(file.listFiles(fileFilter)));
				}
				else{
					addTo.add(file);
				}
			}
			else{
				System.out.println("WARNING: The file or directory '"+ args[index] +"' does not exist. SKIPPING FILE");
			}
		}

		if(fileList.size() == 0){
			printUsage();
		}
		else{
			printCsvHeader();
			inspect( fileList.toArray(new File[fileList.size()]), recursive);
		}
	}

	/**
	 * FilenameFilter for directories
	 */
	public boolean accept(File dir, String name){
		File file = new File(dir + File.separator + name);
		return (file.isDirectory() || name.toLowerCase().endsWith(".js"));
	}

	/**
	 * Print the command line usage for this class
	 */
	private static void printUsage(){
		StringBuffer out = new StringBuffer();
		out.append("\nPrints all functions, variables and objects in a JavaScript file.\n\n");
		out.append("usage: jwalk inspect [-r] [-csv] path [path path] [-e path path]\n");
		out.append("    -r       : Parse directories recursively\n");
		out.append("    -csv     : Output data in CSV format\n");
		out.append("    path     : The file or directory to inspect.\n");
		out.append("               Passing a directory will parse all the JavaScript files contained within it.\n");
		out.append("    -e path  : Files and directories to exclude.\n");
		out.append("\nIn the results:\n");
		out.append("   * \"cloaked\" is used to describe an inner function that is not accessible from the global scope.\n");
		out.append("   * \"implicit global\" is a variable which was not defined with 'var', which means it\n");
		out.append("                         will be assigned to the global scope.\n");

		System.out.println(out.toString());
	}

	/**
	 * Inspect JavaScript files and print the results.
	 * @param files The list of files to inspect
	 * @param recursive Parse directories recursively
	 */
	public static void inspect(File[] files, boolean recursive)
		throws java.io.FileNotFoundException, java.io.IOException {
		inspect(files, recursive, null);
	}

	/**
	 * Inspect JavaScript files and print the results.
	 * @param files The list of files to inspect
	 * @param recursive Parse directories recursively
	 * @param root Root path of all the files
	 */
	public static void inspect(File[] files, boolean recursive, String root)
		throws java.io.FileNotFoundException, java.io.IOException {


		String path;
		File file;
		for(int i = 0; i < files.length; i++){
			file = files[i];
			path = file.getPath();

			// Exclude this file
			if(excludeList.contains(file)){
				continue;
			}

			// Directory
			if(file.isDirectory()){
				if(recursive){
					inspect(file.listFiles(fileFilter), recursive);
				}
				continue;
			}

			if(format != "csv"){
				System.out.println("\nInspecting: "+ path);
			}

			currentFile = file;
			ScriptFile script = JWalkParser.parseFile(path);
			Element elem = script.global.element;
			Element[] children = elem.getChildren();

			if(format != "csv"){
				System.out.println(elem.children.size() +" global elements");
			}

			inspectElementList(children);
		}

	}

	/**
	 * Print the details for each element in the list
	 * @param elems
	 */
	public static void inspectElementList(Element[] elems){
		printVariables(elems);
		printObjects(elems);
		printFunctions(elems);
	}

	/**
	 * Print the global variables in the element list.
	 * @param elem
	 */
	public static void printVariables(Element[] elems){
		printColumnHeaders("Variables");
		currentType = "variable";

		Element elem;
		String notes;
		for(int i = 0; i < elems.length; i++){
			elem = elems[i];
			notes = "";

			if(elem.type == Element.VAR){

				if(elem.implicit_global){
					notes += "!!! Implicit global !!!";
				}

				printRow(elem.name, elem.lineno, notes);
			}

		}
		printRowSeparator();
	}

	/**
	 * Print the functions in the element list.
	 * @param elem
	 */
	public static void printFunctions(Element[] elems){
		printColumnHeaders("Functions");
		currentType = "function";

		Element elem;
		String notes;
		for(int i = 0; i < elems.length; i++){
			elem = elems[i];

			if(elem.type == Element.FUNC || elem.type == Element.METHOD){
				printFunction(elem);
			}
			printNestedFunctions(elem);
		}
		printRowSeparator();

	}

	/**
	 * Print the details of a single function
	 * @param elem
	 */
	public static void printFunction(Element elem){
		String notes = "";

		if(elem.type == Element.FUNC || elem.type == Element.METHOD){

			if(elem.anonymous){
				return;
			}

			if(elem.constructor){
				notes += "constructor";

				if(elem.scope != elem.top && elem.type != Element.METHOD){
					notes += " in '"+ getObjectChain(elem.scope) +"'";
				}
			}

			if(elem.type == Element.METHOD){
				if(!notes.equals("")){
					notes += ", ";
				}
				notes += "method of '"+ getObjectChain(elem.scope) +"'";
			}

			if(elem.scope.anonymous || elem.is_private || (elem.scope.type == Element.FUNC && !elem.scope.constructor)){
				if(!notes.equals("")){
					notes += ", ";
				}
				notes += "cloaked";

				if(elem.scope.type == Element.FUNC && !elem.scope.constructor){
					notes += " in '"+ getObjectChain(elem.scope) +"'";
				}
			}

			printRow(elem.name, elem.lineno, notes);
		}
	}

	/**
	 * Print any nested functions of this element
	 * @param elem
	 */
	public static void printNestedFunctions(Element elem){

		// Only go into anonymous functions at the global scope
		if(elem.anonymous && elem.scope != elem.top){
			return;
		}

		// Read functions
		Element[] elems = elem.getChildren();
		String notes;
		for(int i = 0; i < elems.length; i++){
			elem = elems[i];
			notes = "";

			if(elem.type == Element.FUNC || elem.type == Element.METHOD){
				printFunction(elem);
			}
			else if(elem.constructor || elem.type == Element.OBJ){
				printNestedFunctions(elem);
			}

		}
	}

	/**
	 * Print the Objects in the element list.
	 * @param elem
	 */
	public static void printObjects(Element[] elems){
		printColumnHeaders("Objects");
		currentType = "object";

		Element elem;
		String notes;
		for(int i = 0; i < elems.length; i++){
			elem = elems[i];

			if(elem.type == Element.OBJ){
				printObject(elem);
			}
		}
		printRowSeparator();
	}

	/**
	 * Print the details of all the child objects of this object
	 * @param elem
	 */
	public static void printObject(Element elem){
		int objCount = 0;
		String notes = "";

		// Find any child objects
		Element child;
		Element[] elems = elem.getChildren();
		for(int i = 0; i < elems.length; i++){
			child = elems[i];

			if(child.type == Element.OBJ){
				objCount++;
				printObject(child);
			}
		}

		if(objCount == 0){

			if(elem.is_private || (elem.scope.type == Element.FUNC && !elem.scope.constructor)){
				if(!notes.equals("")){
					notes += ", ";
				}
				notes += "cloaked";
			}

			printRow(getObjectChain(elem), elem.lineno, notes);
		}

	}

	/**
	 * Returns the object chain as a string
	 * @param elem The Object element
	 */
	public static String getObjectChain(Element elem){
		String chain = elem.name;

		while((elem = elem.scope) != null && elem != elem.top){
			chain = elem.name +"."+ chain;
		}

		return chain;
	}

	/**
	 * Print header for CSV file
	 */
	public static void printCsvHeader(){
		if(format == "csv"){
			System.out.println("Type,Name,Notes,Line,File");
		}
	}

	/**
	 * Print the column headers
	 */
	public static void printColumnHeaders(String name){

		if(format == "csv"){
			return;
		}

		int width = 129;

		// Create header title
		String title = "\n\t+--------------- "+ name +" ";
		int remainin = width - title.length();
		for(int i = 0; i < remainin; i++){
			title += "-";
		}
		title += "+";

		System.out.println(title);
		System.out.format("\t| %-50s|%-8s|%-65s|\n", "Name", "  Line  ", " Notes");
		printRowSeparator();
	}

	/**
	 * Print the table row separator line
	 */
	public static void printRowSeparator(){
		if(format != "csv"){
			System.out.format("\t+%s+%s+%s+\n", "---------------------------------------------------", "--------", "-----------------------------------------------------------------");
		}
	}

	/**
	 * Print a row of information
	 * @param name
	 * @param lineno
	 */
	public static void printRow(String name, int lineno){
		printRow(name, lineno, "");
	}

	/**
	 * Print a row of information
	 * @param name
	 * @param lineno
	 * @param notes
	 */
	public static void printRow(String name, int lineno, String notes){
		if(format == "csv"){
			System.out.format("%s,%s,\"%s\",%d,%s\n", currentType, name, notes, lineno, currentFile.getPath());
		}
		else{
			System.out.format("\t| %-50s|%7d | %-64s|\n", name, lineno, notes);
		}
	}

}
