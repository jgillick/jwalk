
var /* String */ globVar;
const GLOB_CONST = 0/* int */;

function plainFunc(attr1, attr2){
	var localVar = "local";
	const CONST_VAR = "constant";
}

globVar = 1;
var globVar2 = 3; // same line comment
globVar4 = "global string"/*inline*/;

/* multi line comment
abcd
efgh
*/

var math = num/5;
var reg = /((^\s*)|(\s*$))/; // space stripping regex

/* Three */ /* Comments */ // On the same line

function /* in between */ objFunc(){
	var one, two, three = "chain", four = "4";
	five, two = "two", six = "seven", three = "chaining";

	// Redefine global var
	globVar = "global";	// hello

	// Implied global variable
	globalVar5 = "move me to global scope";

	// local var
	var local = "foo";

	var page = document.getElementById("page");

	// Properties
	this.propOne = false;
	this.propTwo = "hello";
	this.propThree = "world";
	this.propFour;
	this.propFive.subOne = "test";

	otherObj.someProperty = "w00t";

	for(i = 0; i < 10; i++){
		alert(i +" times");
	}

	// Method referencing global function
	// (should appear as a method named hello)
	this.hello = helloWorld;

	// Methods
	this.methodFuncOne = function(attrX){

	}

	this.methodFuncTwo = function(){

	}

	this.clickHandler = function(){
		this.className = "clicked"; // 'this' will be the elment that was clicked
	}

	// Inner function
	function innerFunc(){

	}

}
objFunc.prototype = {
	mFoo : "Foo Bar",

	protoVar : "",
	protoFunc : function(){

	},

	get fooProp(){
		return this.mFoo;
	},
	set fooProp(f){
		this.mFoo = f;
		return this.mFoo;
	}
}

var objClosure = {
	objVar : "",
	addClass : function(elem, clazz) {
		elem.className = clazz;
	}
}

var globVar3;

// These 2 elements should be placed under objFunc.test
objFunc.test.extendedProperty = "";
objFunc.test.extendedMethod = function(){ };

var core = {};
core.team = {};
core.team.flow = {};
core.team.flow.feature = function(){};
core.team.flow.feature.prototype = {
	state : null,

	openLightbox : function(){
		return true;
	}
};
core.team.flow.feature.prototype.getValue = function(){

}

// Global function
function helloWorld(/*String*/ paramStr, /*boolean*/ paramBool){
	if(false){
		return 10;
	}

	// Loops
	var num = 25;
	for(var i = 0; i < num; i++){
		alert(i);
	}

	var loop = true;
	while(loop){
		loop = false;
	}

	do {
		loop = false;
	} while(loop);

	return "Hello World";
}

// The function's return statement should not appear as a local variable
function echo(str, number, multiplier){

	var out = str + "foo bar ("+ (number * multiplier) +")";

	return out;
}

// The anonFunc variable should appear as a function
var anonFunc = function(){
	return "Hello World";
}

// This should be seen as a singleton object
//  with 1 private variable.
var staticObj = (function() {

	var privateMember = "foo";

	return {
		fooMe : "hello world",
		fooYou : "bar baz",
		fooWorld : function(){ }
	}
})();

var functionVar = function(){

	function callMe(){
	}

}

function(){

	var cloakedVar = "you can't see me";

	function evilPlan(){
		return "Under the radar";
	}
}

window.onload = function() { };



// From prototype.js (problems with functions in the nested object literal)
Element._attributeTranslations = {
  names: {
    colspan:   "colSpan",
    rowspan:   "rowSpan",
    valign:    "vAlign",
    datetime:  "dateTime",
    accesskey: "accessKey",
    tabindex:  "tabIndex",
    enctype:   "encType",
    maxlength: "maxLength",
    readonly:  "readOnly",
    longdesc:  "longDesc",
    regTest : /^[a-z]$/,
    regTest2 : new RegExp("^[a-z]$")
  },
  values: {
    _getAttr: function(element, attribute) {
      return element.getAttribute(attribute, 2);
    },
    _flag: function(element, attribute) {
      return $(element).hasAttribute(attribute) ? attribute : null;
    },
    style: function(element) {
      return element.style.cssText.toLowerCase();
    },
    title: function(element) {
      var node = element.getAttributeNode('title');
      return node.specified ? node.nodeValue : null;
    }
  }
};