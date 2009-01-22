package com.jwalkjs;

import java.io.*;
import java.util.ArrayList;

import org.mozilla.javascript.*;

/**
 * Reads and parses a JavaScript file.
 */
public final class JWalkParser {

	private final static int CONTINUE = 20;
	private final static int SKIP_CHILDREN = 21;

	/**
	 * Set the debugging level for development
	 * 0x0001 - Calls toStringTree on the root node (Token.printTrees needs to be TRUE)
	 * 0x0010 - Prints the description for each node
	 * 0x1000 - Debug where comments are
	 */
	private static int debug = 0x0000;
	private static int indent = 0;

	private static int sourceLength;
	private static ErrorReporter errorReporter;

	private static Element[] allElements;
	private static ArrayList<Comment> comments = null;

	public static void main(String[] args){
		try{
			debug = 0x0010;
			ScriptFile script = parseFile(args[0], true);
			//printTree(script.global, 0);
		} catch(Exception e){
			System.out.println(e.toString());
		}
	}

	/**
	 * Parse a JavaScript file and return an element tree
	 * @param path The file path to the JS file
	 * @param incComments Extract comments from the source
	 */
	public static ScriptFile parseFile(String path, boolean incComments)
		throws java.io.FileNotFoundException, java.io.IOException {

		comments = null;

		// Init JS playground
		Context cx = (new ContextFactory()).enterContext();
		CompilerEnvirons env = new CompilerEnvirons();

		// Read source code
		ScriptFile sourceFile = new ScriptFile(path);
		sourceLength = sourceFile.source.length();

		// Parse
		ErrorReporter errorReporter = env.getErrorReporter();
		Parser parser = new Parser(env, errorReporter);
		ScriptOrFnNode root = parser.parse(sourceFile.source, path, 1);

		Element global = new Element(root, null, Element.ROOT);
		global.script = sourceFile;
		global.name = "[global]";
		global.top = global;


		if((debug & 0x0001) != 0){
			System.out.println(root.toStringTree(root));
		}

		readTree(root, root, global, CONTINUE);

		// Parse comments
		if(incComments){
			comments = new ArrayList();
			allElements = global.getAllChildren();
			TokenStream ts = new TokenStream(parser, null, sourceFile.source, 1);
			readComments(global, ts, sourceFile.source, parser.getEncodedSource());

			sourceFile.comments = comments;
		}

		sourceFile.global = global;
		sourceFile.comments = comments;

		Context.exit();
		return sourceFile;
	}

	public static ScriptFile parseFile(String path)
		throws java.io.FileNotFoundException, java.io.IOException {
		return parseFile(path, false);
	}

	/**
	 * Print the tree to the screen -- used for debugging
	 * @param root The element to start from
	 */
	public static void printTree(Element elem){
		printTree(elem, 0);
	}

	/**
	 * Print the tree to the screen -- used for debugging
	 * @param root The element to print the children of
	 * @param level The level of the heirarchy we're in
	 */
	public static void printTree(Element elem, int level){

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

			// Comments
			if(child.previousComment != null){
				Comment comment = child.previousComment;
				while(comment != null && comment.nextSibling == child){
					System.out.println(comment.getBody());
					comment = comment.previousComment;
				}
			}

			out += "["+ child.lineno +"] "+ child.name +" ";

			switch(child.type){
				case Element.VAR:
				case Element.PROP:
					if(child.implicit_global){
						out += "- IMPLIED GLOBAL";
					}

					type = child.getDatatype();
					if(type > -100){
						out += " <"+ Token.name(type).toLowerCase() +">";
					}
					break;

				case Element.FUNC:
				case Element.METHOD:
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
					if(type > -100){
						out += " <"+ Token.name(type).toLowerCase() +">";
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
	 * Read the source tree and create a tree of Element objects
	 */
	public static void readTree(ScriptOrFnNode tree, Node parent, Element scope, int action){
		Node node = null;
		int propertyIndex = 0;

		// Generate indent padding
		String padding = "";
		for(int i = 0; i < indent; i++){
			padding += "\t";
		}

		// Loop through sibling nodes
		for (;;) {

			// Get node
			Node previous = null;
			if (node == null) {
				node = parent.getFirstChild();
			} else{
				previous = node;
				node = node.getNext();
			}
			if (node == null) {
				break;
			}

			String val = "";
			int type = node.getType();
			int parentType = parent.getType();
			int prevType = (previous != null) ? previous.getType() : -1;

			Object[] properties = new Object[0];
			if(parentType == Token.OBJECTLIT){
				properties = (Object[])parent.getProp(Node.OBJECT_IDS_PROP);
			}

			// Print node description
			if((debug & 0x0010) != 0){

				if(type == Token.STRING || type == Token.NAME || type == Token.BINDNAME || type == Token.FUNCTION){
					val = node.getString();
				}

				if(parentType == Token.OBJECTLIT && properties.length > propertyIndex){
					System.out.println(padding +"["+ node.getLineno() +":"+ node.getOffset() +"] "+ (String)properties[propertyIndex++] +" : "+ Token.name(type) +" "+ val);
				}
				else{
					System.out.println(padding +"["+ node.getLineno() +":"+ node.getOffset() +"] "+ Token.name(type) +" "+ val);
				}
			}

			int ret = action;
			if(action == CONTINUE){
				switch(type){
					case Token.VAR:
					case Token.CONST:
					case Token.EXPR_VOID:
					case Token.EXPR_RESULT:
						ret = parseVariable(node, scope);
						break;
					case Token.FUNCTION:
						ret = parseFunction(node, scope);
						break;
					case Token.RETURN:
						ret = parseReturn(node, scope);
						break;
				}
			}

			// Traverse into function node
			if(type == Token.FUNCTION){
				int funcIndex = node.getExistingIntProp(Node.FUNCTION_PROP);
				ScriptOrFnNode func = tree.getFunctionNode(funcIndex);

				indent++;
				readTree(func, func, scope, ret);
				indent--;
			}


			// Get children
			indent++;
			readTree(tree, node, scope, ret);
			indent--;

			// Reset properties
			if(type == Token.OBJECTLIT){
				properties = new Object[0];
			}
		}
	}

	/**
	 * Read all the comments in the source file
	 * @param global
	 * @param ts
	 */
	public static void readComments(Element global, TokenStream ts, String source, String encSource)
		throws java.io.IOException {

		int tt = ts.getToken();
		int tokenEnd = 0;
		int tokenStart = 0;
		int commentStart = -1;
		int lastToken = tt;
		int lastOffset = -1;

		// source without space, as node width reference
		int refIndex = 1;
		String ref = encSource;

		// Read through TokenStream and find comments
		String message = "";
		Comment comment = null;
		Comment lastComment = null;
		while(tt > Token.EOF){
			message = "";
			lastComment = comment;
			tokenStart = ts.getTokenStart();
			tokenEnd = ts.getCursor();
			commentStart = ts.getCommentStart();

			if((debug & 0x1000) != 0){
				System.out.format("%d %9s %4d, %4d -> %4d\n", ts.getLineno(), Token.name(tt), tokenStart, tokenEnd, commentStart);
			}

			// Get string in between
			if(commentStart > -1){
				if(commentStart < tokenStart){
					commentStart--;
					message = source.substring(commentStart, tokenStart - 1).trim();
				}
				else if(commentStart < tokenEnd){
					commentStart--;
					message = source.substring(commentStart, tokenEnd - 1).trim();
				}

				if(message.length() > 0){
					if((debug & 0x1000) != 0){
						System.out.println(message);
					}
					comment = addComment(commentStart, ts.getCommentLineno(), message);

					if(lastComment != null){
						lastComment.nextComment = comment;
						comment.previousComment = lastComment;
					}
				}
			}

			// Might be regex
			if(tt == Token.DIV && lastToken != Token.NUMBER && lastToken != Token.NAME && lastToken != Token.RP){
				ts.readRegExp(tt);
				tt = ts.getToken();
			}
			lastToken = tt;
			lastOffset = tokenEnd;
			tt = ts.getToken();
		}

	}

	/**
	 * Get the index for the next node type in the encoded source
	 * @param encSource
	 * @param nodeType
	 * @param cursor The current location in the encoded source
	 * @return
	 */
	private static int getNextNode(String encSource, int nodeType, int cursor){
		int len = encSource.length();
		for(int i = cursor; i < len; i++){
			if(encSource.charAt(i) == nodeType){
				return i;
			}
		}
		return -1;
	}

	/**
	 * Add a comment to the correct elements
	 * @param offset
	 * @param lineno
	 * @param msg
	 */
	private static Comment addComment(int offset, int lineno, String msg){
		Comment comment = new Comment(lineno, offset, msg);

		// Find the element right above the comment
		// Use the offset and source length to guess where in the allElements array
		// the element is.
		int elemNum = allElements.length;
		int index = (int)Math.round((float)offset/(float)sourceLength * (float)elemNum);
		int direction = 1;

		Element elem = allElements[index];
		Element nextElem = (index + 1 < allElements.length) ? allElements[index + 1] : null;
		while(elem != null){

			// Found
			if(elem.start < offset && (nextElem == null || nextElem.start >= offset)){
				comment.previousSibling = elem;
				comment.nextSibling = nextElem;
				break;
			}
			// Overshot, change directions
			else if(elem.start > offset){
				direction = -1;
			}

			// Get next element
			index += direction;
			if(index > 0 && index < allElements.length){
				elem = allElements[index];
				nextElem = (index + 1 < allElements.length) ? allElements[index + 1] : null;
			}
			else{
				elem = null;
				index = 0;
				break;
			}
		}

		// Must be at the start of the file
		if(index == 0){
			nextElem = allElements[0];
			comment.nextSibling = nextElem;
		}

		// Add to Element node
		if(elem != null){
			elem.nextComment = comment;
		}
		if(nextElem != null){
			nextElem.previousComment = comment;
		}

		comments.add(comment);
		return comment;
	}

	/**
	 * Parse variable node
	 * @param node
	 * @param scope The function scope this node is in.
	 */
	private static int parseVariable(Node node, Element scope){
		return parseVariable(node, node, null, scope);
	}

	/**
	 * Parse variable node
	 * @param node
	 * @param origNode The original node that started this statement
	 * @param scope The function scope this node is in.
	 */
	private static int parseVariable(Node node, Node origNode, Element scope){
		return parseVariable(node, origNode, null, scope);
	}

	/**
	 * Parse variable node
	 * @param node
	 * @param origNode The original node that started this statement
	 * @param attachTo Build the variable on this element instead of creating a new one.
	 * @param scope The function scope this node is in.
	 */
	private static int parseVariable(Node node, Node origNode, Element attachTo, Element scope){
		int type = node.getType();

		Element elem;
		if(attachTo != null){
			elem = attachTo;
		}
		else{
			elem = new Element(origNode, scope, Element.VAR);
			elem.read_only = true;
		}

		if(type == Token.NAME){
			elem.name = node.getString();
		}

		// Loop through child siblings
		Node child = node.getFirstChild();
		while(child != null){
			Node sibling = child.getNext();
			Node grandChild = child.getFirstChild();

			switch(child.getType()){

				// Variable name
				case Token.NAME:
					if(attachTo == null){
						elem.name = child.getString();
						parseVariable(child, origNode, elem, scope);
					}

					// The next sibling is NAME in a multi variable declaration ("var one, two, three;")
					if(sibling != null && sibling.getType() == Token.NAME){
						scope.addChild(elem);
						elem = new Element(origNode, scope, Element.VAR);
					}
					break;

				// Value type
				case Token.STRING:
				case Token.NUMBER:
				case Token.TRUE:
				case Token.FALSE:
				case Token.NULL:
				case Token.THIS:
				case Token.REGEXP:
					elem.addDatatype(child.getType());
					break;

				// Multiple variables declared -- dig further
				case Token.COMMA:
					parseVariable(child, origNode, scope);
					break;

				// Starting a new variable
				case Token.SETNAME:
					parseVariable(child, origNode, scope);
					break;

				// Setting an object property
				case Token.SETPROP:
					parseProperty(child, origNode, scope);
					break;

				// Bind the variable name to new variable
				case Token.BINDNAME:
					elem.name = child.getString();

					// Set line number, if it wasn't set before
					if(elem.lineno == -1 && child.getLineno() > -1){
						elem.lineno = child.getLineno();
						elem.start = child.getOffset();
					}

					break;

				// Function assigned to the variable
				case Token.FUNCTION:
					elem.type = Element.FUNC;
					parseFunction(child, elem, scope);

					if(scope != scope.top){
						elem.is_private = true;
					}
					break;

				// The return value of a function call: var foo = helloWorld();
				case Token.CALL:
					if(grandChild.getType() == Token.FUNCTION){

						// Process and set variable value
						Element callFunc = new Element(origNode, scope, Element.TOKEN);
						parseFunction(grandChild, callFunc, scope);

						// Merge properties and functions variables into this variable and mark it as an object
						if(callFunc.objReturns.size() == 1){
							Element objLit = (Element)callFunc.objReturns.get(0);

							elem.type = Element.OBJ;
							elem.addChildren(callFunc.children, false);
							elem.addChildren(objLit.children, true);

							objLit = null;
							callFunc = null;
						}
					}
					break;

				// Object literal
				case Token.OBJECTLIT:
					elem.type = Element.OBJ;
					parseObjectLiteral(child, elem, scope);

					if(scope != scope.top){
						elem.is_private = true;
					}
					break;
			}

			child = sibling;
		}

		// Add element
		if(elem != null && elem.name != null){

			// Missing var, so it's a redefined variable or implicit global
			if(origNode.getType() != Token.VAR && origNode.getType() != Token.CONST){
				Element find = scope.findByName(elem.name, true, true);

				// New variable
				if(find == null){
					elem.implicit_global = true;
					scope.top.addChild(elem);
				}

				// Existing variable redefined -- update datatype
				else{
					if(elem.getDatatype() != Element.TYPE_UNKNOWN){
						find.addDatatype(elem.getDatatype());
					}
					elem = null;
				}
			}

			// Add to scope
			if(elem != null){
				scope.addChild(elem);
			}
		}
		else{
			elem = null;
		}

		return SKIP_CHILDREN;
	}

	/**
	 * Parse the function node
	 * @param node
	 * @param scope The function scope this node is in.
	 * @return
	 */
	private static int parseFunction(Node node, Element scope){
		return parseFunction(node, null, scope);
	}

	/**
	 * Parse the function node
	 * @param node
	 * @param attachTo The variable or property that this function should be assigned to
	 * @param scope The function scope this node is in.
	 * @return
	 */
	private static int parseFunction(Node node, Element attachTo, Element scope){
		int type = node.getType();
		String name = node.getString();

		if(scope.name == null){
			return CONTINUE;
		}

		// Get function block node
		int funcIndex = node.getExistingIntProp(Node.FUNCTION_PROP);
		ScriptOrFnNode block = ((ScriptOrFnNode)scope.node).getFunctionNode(funcIndex);

		if(block == null){
			return CONTINUE;
		}
		block.flattenSymbolTable(false);

		Element elem;
		if(attachTo != null){
			elem = attachTo;
			elem.type = Element.FUNC;
			elem.node = block;
			name = elem.name;
		}
		else{
			elem = new Element(block, scope, Element.FUNC);
		}

		// Anonymous
		if(name == null || name.equals("")){
			elem.anonymous = true;
		}
		else{
			elem.name = name;
		}

		// Get params
		if(block.getParamCount() > 0){
			int length = block.getParamCount();
			String[] names = block.getParamAndVarNames();
			elem.params = new FunctionParam[length];

			for(int i = 0; i < length; i++){
				elem.params[i] = new FunctionParam(names[i]);
			}
		}

		// Parse function block
		if(block != null){
			if(attachTo == null){
				scope.addChild(elem);
			}

			// Process function block
			readTree(block, block, elem, CONTINUE);
		}
		else{
			return CONTINUE;
		}


		return SKIP_CHILDREN;
	}

	/**
	 * Parse a return statment for a scope.
	 * @param node
	 * @param scope The function scope this node is in.
	 */
	private static int parseReturn(Node node, Element scope){

		// Loop through child siblings
		Node sibling = node.getFirstChild();
		while(sibling != null){

			switch(sibling.getType()){
				case Token.STRING:
				case Token.NUMBER:
				case Token.TRUE:
				case Token.FALSE:
				case Token.NULL:
				case Token.THIS:
				case Token.REGEXP:
					scope.addDatatype(sibling.getType());
					break;
				case Token.OBJECTLIT:
					scope.addDatatype(sibling.getType());

					Element retElem = new Element(node, scope, Element.OBJ);
					parseObjectLiteral(sibling, retElem, scope);
					scope.objReturns.add(retElem);
					break;
			}

			sibling = sibling.getNext();
		}

		return SKIP_CHILDREN;
	}

	/**
	 * Parse an object property or method
	 * @param node
	 * @param origNode The original node that started this statement
	 * @param scope The function scope this node is in.
	 * @return
	 */
	private static int parseProperty(Node node, Node origNode, Element scope){
		int type = node.getType();
		Element elem = new Element(origNode, scope, Element.PROP);
		Node child = node.getFirstChild();
		int childType = child.getType();

		/*
		 *  A single property name value pair
		 */
		if(node.getType() == Token.SETPROP && (childType == Token.THIS || childType == Token.NAME) ){

			// If not THIS, it needs to be at the global scope
			if(childType != Token.THIS && scope != scope.top){
				return CONTINUE;
			}

			// Scope
			Element parent = scope;
			if(childType == Token.NAME){
				parent = scope.findByName(child.getString());

				// Create the scope if it doesn't exist
				if(parent == null){
					Element objScope = new Element(origNode, scope, Element.OBJ);
					objScope.name = child.getString();
					objScope.implicit_obj = true;
					scope.addChild(objScope);
					parent = objScope;
					elem.parent = parent;
				}
			}

			// Property name
			Node sibling = child.getNext();
			if(sibling != null && sibling.getType() == Token.STRING){
				elem.name = sibling.getString();
			}
			else{
				return CONTINUE;
			}

			// Prototype?
			if(elem.name.equals("prototype")){
				elem = parent;
				parent = parent.scope;
			}

			// Type
			sibling = sibling.getNext();
			if(sibling != null){
				switch(sibling.getType()){
					case Token.FUNCTION:
						parseFunction(sibling, elem, scope);
						elem.type = Element.METHOD;
						break;
					case Token.OBJECTLIT:
						parseObjectLiteral(sibling, elem, scope);
						if(elem.constructor){
							elem.type = Element.FUNC;
						}
						break;
					default:
						elem.addDatatype(sibling.getType());
				}
			}

			// Add to scope
			if(elem != null){
				if(scope.type == Element.FUNC){
					scope.constructor = true;
				}
				parent.addChild(elem);

				return SKIP_CHILDREN;
			}

			return CONTINUE;
		}

		/*
		 *  Object chain ("one.two.thee = 'four'")
		 */
		if(childType == Token.GETPROP){

			// Get object chain, it goes in reverse order
			Node sibling;
			ArrayList<String> chain = new ArrayList();

			while(child != null && (child.getType() == Token.GETPROP || child.getType() == Token.NAME)){

				sibling = child.getNext();

				// If we're at NAME we need to get get STRING first, since that's the correct chain order
				if(child.getType() == Token.NAME){

					if(sibling != null && sibling.getType() == Token.STRING){
						chain.add(0, sibling.getString());
					}

					sibling = child;
				}

				// Get chain element name
				if(sibling != null && (sibling.getType() == Token.NAME || sibling.getType() == Token.STRING)){

					// Name of the property
					if(elem.name == null){
						elem.name = sibling.getString();
					}
					// Chain item name
					else{
						chain.add(0, sibling.getString());
					}
				}
				else{
					break;
				}

				// Get element value
				sibling = sibling.getNext();
				if(chain.size() <= 1 && sibling != null){

					switch(sibling.getType()){
						case Token.FUNCTION:
							parseFunction(sibling, elem, scope);
							elem.type = Element.METHOD;
							break;
						case Token.OBJECTLIT:
							parseObjectLiteral(sibling, elem, scope);
							if(elem.constructor){
								elem.type = Element.FUNC;
							}
							break;
						default:
							elem.addDatatype(sibling.getType());
					}

				}

				child = child.getFirstChild();
			}

			// Construct object heirarchy
			String name;
			Element objScope = scope;
			Element prevScope = scope.top;
			for(int i = 0; i < chain.size(); i++){
				name = chain.get(i);

				// Skip prototype
				if(name.equals("prototype")){
					prevScope.type = Element.FUNC;
					prevScope.constructor = true;
					continue;
				}

				objScope = objScope.findByName(chain.get(i), (prevScope == scope.top));

				// Create an empty object
				if(objScope == null){
					objScope = new Element(origNode, prevScope, Element.OBJ);
					objScope.name = name;
					objScope.implicit_obj = true;
					prevScope.addChild(objScope);
				}
				else if(objScope.constructor == false){
					objScope.type = Element.OBJ;
				}

				prevScope = objScope;
			}

			// If the element is "prototype", then it's scope is the constructor
			if(elem.name.equals("prototype")){
				objScope.constructor = true;
				objScope.addChildren(elem.children);
				elem = null;
			}

			// Add to object
			if(objScope.type == Element.FUNC){
				objScope.constructor = true;
			}

			if(elem != null){
				objScope.addChild(elem);
			}

			return SKIP_CHILDREN;
		}

		return CONTINUE;
	}

	/**
	 * Parse an object literal block
	 * @param node
	 * @param attachTo The variable or property that this object should be assigned to
	 * @param scope The function scope this node is in.
	 * @return
	 */
	private static int parseObjectLiteral(Node node, Element attachTo, Element scope){
		Element elem;
		if(attachTo != null){
			elem = attachTo;
			elem.node = node;

			if(elem.constructor == false){
				elem.type = Element.OBJ;
			}
		}
		else{
			return CONTINUE;
		}

		// Get properties
		String name;
		Element prop;
		Node child = null;
		Object[] props = (Object[])node.getProp(Node.OBJECT_IDS_PROP);

		for(int i = 0; i < props.length; i++){
			name = (String)props[i];

			// Get node
			if(child == null){
				child = node.getFirstChild();
			}
			else{
				child = child.getNext();
			}

			// Create element
			prop = new Element(child, elem, Element.PROP);
			prop.name = name;
			switch(child.getType()){
				case Token.FUNCTION:
					parseFunction(child, prop, scope);
					prop.type = Element.METHOD;
					break;
				case Token.OBJECTLIT:
					prop.type = Element.OBJ;
					parseObjectLiteral(child, prop, scope);
					break;
				case Token.STRING:
				case Token.NUMBER:
				case Token.TRUE:
				case Token.FALSE:
				case Token.NULL:
				case Token.THIS:
				case Token.REGEXP:
					prop.addDatatype(child.getType());
					break;
				default:
					prop = null;
			}

			if(prop != null){
				elem.addChild(prop);
			}
		}

		return SKIP_CHILDREN;
	}
}
