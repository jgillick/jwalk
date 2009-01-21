package com.jwalkjs;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import org.mozilla.javascript.Token;

public class PrintTree implements FilenameFilter {

	public static void main(String[] args)
		throws java.io.FileNotFoundException, java.io.IOException {

		if(args.length == 0 || args[0].equals("-help")){
			printUsage();
			return;
		}

		String path = args[0];
		if(!(new File(path)).exists()){
			System.out.println("The file '"+ path +"' doesn't exist.");
			return;
		}

		Element elem = JWalkParser.parseFile(path);
		printTree(elem);
	}

	/**
	 * FilenameFilter for directories
	 */
	public boolean accept(File dir, String name){
		return name.toLowerCase().endsWith(".js");
	}

	/**
	 * Print the command line usage for this class
	 */
	private static void printUsage(){
		StringBuffer out = new StringBuffer();
		out.append("Prints a tree representing the elements in your JavaScript file.\n\n");
		out.append("usage: jwalk tree path\n");
		out.append("    path     : The JavaScript file to read.\n");

		System.out.println(out.toString());
	}

	/**
	 * Print the tree to the screen
	 * @param root The element to print the children of
	 */
	private static void printTree(Element elem){
		printTree(elem, 0);
	}

	/**
	 * Print the tree to the screen
	 * @param root The element to print the children of
	 * @param level The level of the heirarchy we're in
	 */
	private static void printTree(Element elem, int level){

		// Generate indents
		String padding = "";
		for(int i = 0; i < level; i++){
			padding += "\t";
		}

		// Print children
		for(int i = 0; i < elem.children.size(); i++){
			Element child = elem.children.get(i);
			String out = padding;
			int type;

			out += "["+ child.lineno +":"+ child.start+"] ";

			switch(child.type){
				case Element.VAR:
				case Element.PROP:
				case Element.OBJ:
					out += child.name +" ";
					if(child.implicit_global){
						break;
					}

					type = child.getDatatype();
					if(type != Element.TYPE_UNKNOWN){
						out += " <"+ getTypeName(type) +">";
					}
					break;

				case Element.FUNC:
				case Element.METHOD:

					if(child.name == null){
						out += "[anonymous] ";
					}
					else{
						out += child.name +" ";
					}

					out += "(";
					FunctionParam[] params = child.params;
					for(int n = 0; n < params.length; n++){
						if(n > 0){
							out += ", ";
						}
						out += params[n].name;
					}
					out += ")";

					type = child.getDatatype();
					if(type != Element.TYPE_UNKNOWN){
						out += " <"+ getTypeName(type) +">";
					}

					if(child.constructor){
						out += " - constructor";
					}

					break;
			}


			System.out.println(out);

			printTree(child, level + 1);
		}

	}

	/**
	 * Get the name of the datatype
	 * @param type
	 * @return
	 */
	public static String getTypeName(int type){
		switch(type){
			case Element.TYPE_NULL:
				return "NULL";
			case Element.TYPE_THIS:
				return "THIS";
			case Element.TYPE_STRING:
				return "string";
			case Element.TYPE_NUMBER:
				return "number";
			case Element.TYPE_BOOLEAN:
				return "boolean";
			case Element.TYPE_REGEXP:
				return "regexp";
			case Element.TYPE_UNKNOWN:
			default:
				return "unknown";
		}
	}

}
