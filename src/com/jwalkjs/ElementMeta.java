package com.jwalkjs;

import java.util.ArrayList;
import java.util.Hashtable;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class ElementMeta {
	
	static final int FUNCTION 	= 2;
	static final int VARIABLE	= 3;
	static final int OBJECT		= 4;
	static final int PROPERTY	= 5;
	static final int METHOD		= 6;
	
	public Element element;
	
	public String name = null;
	public int lineno = -1;
	public int linepos = -1;
	
	public Scriptable type;
	public String description = "";
	public String short_description = "";

	private Hashtable<String,Scriptable> params = null;
	private Scriptable scope;
	private Context cx;
	
	/** 
	 * Extract the meta from the JS element into this ElementMeta object
	 */
	public ElementMeta(Context cx, Scriptable scope, Element elem){
		this.cx = cx;
		this.scope = scope;
		this.element = elem;
		elem.doc = this;
		
		this.name = elem.name;
		this.lineno = elem.lineno;
		this.linepos = elem.start;
		
		// Datatype to JSON object: { type: "string", desc: "..." };
		String type = "unknown";
		switch(elem.getDatatype()){
		case Element.TYPE_NULL:
			type = "null";
			break;
		case Element.TYPE_STRING:
			type = "string";
			break;
		case Element.TYPE_NUMBER:
			type = "number";
			break;
		case Element.TYPE_BOOLEAN:
			type = "boolean";
			break;
		case Element.TYPE_REGEXP:
			type = "regexp";
			break;
		}
		
		this.type = cx.newObject(scope);
		this.type.put("name", this.type, type);
		this.type.put("desc", this.type, "");
		
		// Create Params
		params = new Hashtable<String,Scriptable>();
		FunctionParam param;
		Scriptable definition;
		Object name;
		for(int i = 0; i < element.params.length; i++){
			param = element.params[i];
			definition = cx.newObject(scope);
			name = Context.javaToJS(param.name, scope);
			definition.put("name", definition, name);
			
			this.setParam(definition);
		}
		
		
		// Return value
	}
	
	/**
	 * Returns the symbol type of element this is.
	 * i.e. function, variable, object, etc.
	 */
	public String getSymbol(){

		// Element type
		switch(this.element.type){
		case Element.FUNC:
			return "function";
		case Element.METHOD:
			return "method";
			
		case Element.VAR:
			return "variable";
		case Element.PROP:
			return "property";
		
		case Element.ROOT:
			return "ROOT";
		case Element.PARAM:
			return "parameter";
			
		case Element.OBJ:
		default:
			return "object";
		}
		
	}
	
	/**
	 * Sets a function parameter definition
	 * @param definition A JSON definition for this parameter.  
	 * 					Should include 'name', 'type', 'desc' and 'short_desc"
	 */
	public void setParam(Scriptable definition){
		String name = (String)definition.get("name", definition); 
			
		// Check if 'name' is set
		Object jsname = Context.javaToJS(name, scope);
		if( !definition.has(name, definition) ){
			definition.put("name", definition, jsname);
		}
		
		// Add
		params.put(name, definition);
	}
	
	/**
	 * Get the parameter definition for this name
	 * @param name The name of the parameter to get
	 */
	public Scriptable getParam(Object name){
		return params.get(name);
	}
	
	/**
	 * Gets all the parameters for this function/method
	 */
	public Object getParams(){
		Object[] list = params.values().toArray();
		return Context.javaToJS(list, this.scope);
	}
	
	/**
	 * Get all the elements defined under this one.
	 * This is really only appilcable for Objects, Functions and Methods
	 */
	public Object getChildren(){
		return getChildren(0);
	}
	
	/**
	 * Get all the children of a specific type (i.e. Funciton, Variable, etc) 
	 * defined under this one.
	 * This is really only appilcable for Objects, Functions and Methods
	 * @param type The element type FUNCTION, VARIABLE, OBJECT etc.
	 */
	public Object getChildren(int symbol){
		ArrayList<ElementMeta> children = new ArrayList<ElementMeta>();
		
		Element child;
		for( int i = 0; i < element.children.size(); i++ ){
			child = element.children.get(i);
			if( symbol == 0 || (symbol >= FUNCTION  && symbol <= METHOD && child.type == symbol) ){
				children.add(child.doc);
			}
		}
		
		return Context.javaToJS(children.toArray(), this.scope);
	}
	
	/**
	 * Get the parent element that this one is defined in. (Function, Method or Object).
	 * @return Parent element or NULL if this is the root object
	 */
	public Element getParent(){
		return this.element.parent;
	}
	
	/**
	 * Get the root element of the JavaScript file.
	 * The root element is a "virtual" element that is created to hold all the script file element.
	 */
	public Element getRoot(){
		return this.element.top;
	}

}
