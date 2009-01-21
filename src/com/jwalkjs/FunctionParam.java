package com.jwalkjs;

import java.util.ArrayList;

public class FunctionParam extends Element {

	FunctionParam(String name){
		this.lineno = -1;
		this.start = -1;
		this.name = name;
		this.type = Element.PARAM;
	}

	public void addChild(Element child){}
	public void addChildren(ArrayList<Element> children){}
	public void addChildren(ArrayList<Element> children, boolean rebuild){}
	public void removeChild(String name){}

}
