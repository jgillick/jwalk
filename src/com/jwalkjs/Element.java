package com.jwalkjs;

import java.util.Arrays;
import java.util.ArrayList;
import org.mozilla.javascript.*;

public class Element implements Comparable {

	// Node types
	protected static final int ROOT 	= 0;
	protected static final int TOKEN 	= 1;
	protected static final int FUNC 	= 2;
	protected static final int VAR 		= 3;
	protected static final int OBJ 		= 4;
	protected static final int PROP 	= 5;
	protected static final int METHOD	= 6;
	protected static final int BLOCK 	= 7;
	protected static final int PARAM 	= 8;
	protected static final int COMMENT	= 9;

	// Data types
	static final int TYPE_NULL		= 10;
	static final int TYPE_THIS		= 11;
	static final int TYPE_STRING	= 12;
	static final int TYPE_NUMBER	= 13;
	static final int TYPE_BOOLEAN	= 14;
	static final int TYPE_REGEXP	= 15;
	//static final int TYPE_OBJLIT	= 16;
	static final int TYPE_UNKNOWN	= -100;

	// Global properties
	protected int type;
	public String name = null;

	public int lineno = -1;
	public int start = -1;
	protected Node node = null;

	public Element top;
	public Element scope;
	public Element parent;
	public ScriptFile script;

	public Element previousSibling;
	public Element nextSibling;

	public Comment previousComment;
	public Comment nextComment;

	protected ArrayList<Element> children = new ArrayList();
	public Element firstChild;
	public Element lastChild;

	public boolean is_private = false;
	public ElementDoc doc = null;

	// Variable properties
	public boolean read_only = false;
	public boolean write_only = false;
	public boolean implicit_global = false;
	public ArrayList<Integer> datatypes = new ArrayList();  // Token types

	// Function properties
	public boolean constructor = false;
	public boolean anonymous = false;
	public FunctionParam[] params = new FunctionParam[0];
	public ArrayList<Object> objReturns = new ArrayList();  // Object Literal nodes, return types go to the datatypes ArrayList

	// Object properties
	public boolean implicit_obj = false; // not defined explicitly

	public Element(){ }

	/**
	 * Create new element
	 * @param ts
	 * @param parent The physical parent element.  Often this will be the same as scope
	 * @param type The type of element this is
	 */
	public Element(Node node, Element parent, int type){
		this.node = node;
		this.type = type;
		this.lineno = node.getLineno();
		this.start = node.getOffset();
		this.parent = parent;

		if(parent != null){
			this.top = parent.top;
		}

		if(this.top != null){
			this.script = this.top.script;
		}
	}

	/**
	 * Returns all the comments in the current source file
	 * @return
	 */
	public Comment[] getAllComments(){
		if(this.top != this){
			return this.top.getAllComments();
		}

		return script.comments.toArray( new Comment[script.comments.size()] );
	}

	/**
	 * Add an element as a child of this element
	 * @param child
	 */
	public void addChild(Element child){

		// Already exists, override
		removeChild(child.name);

		if(children.size() == 0){
			this.firstChild = child;
		}

		// Set siblings
		if(this.lastChild != null){
			this.lastChild.nextSibling = child;
			child.previousSibling = this.lastChild;
			this.lastChild = child;
		}

		// Add as child of block
		child.scope = this;
		children.add(child);
	}

	/**
	 * Add a list of children.  This will automatically call rebuildChildList() and might change
	 * the order and sibling associations/
	 * @param chilren
	 */
	public void addChildren(ArrayList<Element> children){
		addChildren(children, true);
	}

	/**
	 * Add a list of children.
	 * @param chilren
	 * @param rebuild Will automatically resort and rebuild the children list
	 */
	public void addChildren(ArrayList<Element> children, boolean rebuild){
		this.children.addAll(children);

		if(rebuild){
			rebuildChildList();
		}
	}

	/**
	 * Remove a child by name
	 * @param name
	 */
	public void removeChild(String name){
		Element child = this.findByName(name, false, false);
		if(child != null && child.type != PARAM){

			// Adjust sibling mappings
			if(child.previousSibling != null && child.nextSibling != null){  // Surrounded
				child.previousSibling.nextSibling = child.nextSibling;
				child.nextSibling.previousSibling = child.previousSibling;
			}
			else if(child.nextSibling != null){  // Only has next, so we must be first
				child.nextSibling.previousSibling = null;
				firstChild = child.nextSibling;
			}
			else if(child.previousSibling != null){ // Only has previous, so we must be last
				child.previousSibling.nextSibling = null;
				lastChild = child.previousSibling;
			}

			// Remove
			children.remove(child);

		}
	}

	/**
	 * Return an array of child elements.
	 * @return
	 */
	public Element[] getChildren(){
		Element[] elems = new Element[children.size()];
		return children.toArray(elems);
	}

	/**
	 * Gets all the children recursively and returns a flat Array sorted by position in the source.
	 */
	public Element[] getAllChildren(){
		ArrayList<Element> list = getAllChildrenList();
		Element[] all = list.toArray(new Element[list.size()]);
		Arrays.sort(all);

		return all;
	}

	/**
	 * Same as getAllChildren() but returns an Array list.
	 * @return
	 */
	private ArrayList getAllChildrenList(){
		ArrayList<Element> all = new ArrayList();

		for(int i = 0; i < children.size(); i++){
			all.add(children.get(i));
			all.addAll(children.get(i).getAllChildrenList());
		}

		return all;
	}

	/**
	 * Find an element by name in this and parent scopes scope.
	 * @param name The name of the element to search for
	 * @param recurse Weahter to recurse up the scopes to find the element
	 * @param searchParams Search function parameter names
	 */
	public Element findByName(String name, boolean recurse, boolean searchParams){

		// Search through children for this variable name
		for(int i = 0; i < children.size(); i++){
			Element child = children.get(i);

			if(child.name == name){
				return child;
			}
		}

		// Look in function parameters
		if(type == FUNC){
			for(int i = 0; i < params.length; i++){
				if(params[i].name.equals(name)){
					return params[i];
				}
			}
		}

		if(recurse && parent != null){
			return ((Element)parent).findByName(name, recurse, searchParams);
		}

		return null;
	}

	public Element findByName(String name){
		return findByName(name, true, false);
	}

	public Element findByName(String name, boolean recurse){
		return findByName(name, recurse, false);
	}

	/**
	 * Add a datatype to the list
	 * @param type The datatype to add to the list for this variable.
	 */
	public void addDatatype(int type){

		// Convert to JWalk type map
		switch(type){
			case Token.STRING:
				type = TYPE_STRING;
				break;
			case Token.NUMBER:
				type = TYPE_NUMBER;
				break;
			case Token.TRUE:
			case Token.FALSE:
				type = TYPE_BOOLEAN;
				break;
			case Token.NULL:
				type = TYPE_NULL;
				break;
			case Token.THIS:
				type = TYPE_THIS;
				break;
			case Token.REGEXP:
				type = TYPE_REGEXP;
				break;
			/*case Token.OBJECTLIT:
				type = TYPE_OBJLIT;
				break;*/
			default:
				type = TYPE_UNKNOWN;
		}

		addDatatype(new Integer(type));
	}

	/**
	 * Add a datatype to the list
	 * @param type The datatype to add to the list for this variable.
	 */
	private void addDatatype(Integer type){

		if(!datatypes.contains(type)){
			datatypes.add(type);
		}
	}

	/**
	 * Get the datatype code (TYPE_*) for this element or TYPE_UNKNOWN if there are multiple types defined or none at all.
	 */
	public int getDatatype(){

		if(datatypes.size() == 1){
			return datatypes.get(0).intValue();
		}
		return TYPE_UNKNOWN;
	}

	/**
	 * Get the datatypes which were assigned to this element
	 */
	public int[] getDatatypes(){
		int[] types = new int[datatypes.size()];
		for(int i = 0; i < datatypes.size(); i++){
			types[i] = datatypes.get(0).intValue();
		}
		return types;
	}

	/**
	 * Orders the children by line number and set the siblings
	 */
	public void rebuildChildList(){
		Element childScope = (type == Element.OBJ || type == Element.FUNC) ? this : scope;

		// No children
		if(children == null || children.size() == 0){
			return;
		}

		// Only one child, no need to sort
		if(children.size() == 1){
			Element child = children.get(0);
			firstChild = lastChild = child;
			firstChild.nextSibling = null;
			firstChild.previousSibling = null;

			child.parent = this;
			child.scope = childScope;

			return;
		}

		// Sort
		Element[] list = new Element[children.size()];
		list = children.toArray(list);
		Arrays.sort(list);

		ArrayList<Element> newList = new ArrayList(Arrays.asList(list));
		children = newList;

		// Fix the child and sibling associations
		Element child;
		Element previous = null;
		firstChild = children.get(0);
		lastChild = children.get(children.size() - 1);
		for(int i = 0; i < children.size(); i++){
			child = children.get(i);
			child.scope = childScope;
			child.parent = this;

			if(previous != null){
				child.previousSibling = previous;
				previous.nextSibling = child;
			}
		}
	}

	/**
	 * Compares this Element with another, sorting it by line number
	 */
	public int compareTo(Object obj) {
		if(obj instanceof Element){
			Element elem = (Element)obj;

			if(elem.start == this.start){
				return 0;
			}
			return (elem.start > this.start) ? -1 : 1;
		}
		return 1;
	}
}