package com.jwalkjs;

import java.util.ArrayList;

public class Comment extends Element {

	private String body = "";

	Comment(int lineno, int start, String body){
		this.lineno = lineno;
		this.start = start;
		this.body = body;
		this.type = Element.COMMENT;
	}

	public void addChild(Element child){}
	public void addChildren(ArrayList<Element> children){}
	public void addChildren(ArrayList<Element> children, boolean rebuild){}
	public void removeChild(String name){}

	public String getBody(){
		return body;
	}

}
