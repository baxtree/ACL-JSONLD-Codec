/**
 * ***************************************************************
 * JADE - Java Agent DEvelopment Framework is a framework to develop
 * multi-agent systems in compliance with the FIPA specifications.
 * Copyright (C) 2000 CSELT S.p.A.
 * 
 * GNU Lesser General Public License
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation,
 * version 2.1 of the License.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 * **************************************************************
 */
package jade.content.lang.rdf;

import jade.content.abs.AbsAggregate;
import jade.content.abs.AbsConcept;
import jade.content.abs.AbsContentElement;
import jade.content.abs.AbsContentElementList;
import jade.content.abs.AbsHelper;
import jade.content.abs.AbsObject;
import jade.content.abs.AbsPredicate;
import jade.content.abs.AbsPrimitive;
import jade.content.abs.AbsTerm;
import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.schema.AggregateSchema;
import jade.content.schema.ContentElementListSchema;
import jade.content.schema.Facet;
import jade.content.schema.ObjectSchema;
import jade.content.schema.PrimitiveSchema;
import jade.content.schema.facets.TypedAggregateFacet;
import jade.lang.acl.ISO8601;
import jade.util.leap.ArrayList;

import org.apache.commons.codec.binary.Base64;
import org.xml.sax.Attributes;

/**
 * @author Rosalba Bochicchio - TELECOM ITALIA LAB
 */

class RDFDecoder {

	Ontology ontology;
	boolean initialited = false;
	boolean ContentElementList = false;
	String encoded=null;
	String previousTag=null;
	String temp=null;
	boolean emptyAgg=false;

	
	class StackElement {
		public String tag;
		public AbsObject term;
	}

	class SupportStack {
		ArrayList posComplex = new ArrayList();		
		ArrayList stack = new ArrayList();
				
		protected void clear() {
			stack.clear();
			posComplex.clear();
		}
		
		protected int size() {
			return stack.size();
		}
		
		protected void push(StackElement element) {
			if (stack.size()>0) {
				StackElement prev = getPreviousComplexElement(0);
				// Verify if the element that we want to push is
				// a child of a previous aggregate. In that 
				// put convetionally his tag equals to "*"				
				if ((prev.term instanceof AbsAggregate) &&
					(prev.tag.equals(element.tag))) {
						element.tag="*";
				}
			}
			stack.add(element);			
			// If the added element isn't a primitive, update the
			// references to complex elements in the stack
			if (!(element.term instanceof AbsPrimitive)) {
					posComplex.add(new Integer(stack.size()));
					}
		}
			
		protected StackElement get(int i) {
			return (StackElement)stack.get(i);
		}
		
		protected int numberOfComplex() {
			return posComplex.size();
		}
		
		protected StackElement getPreviousComplexElement(int offset) {
			int temp = ((Integer)posComplex.get(numberOfComplex()-offset-1)).intValue();
			StackElement element = (StackElement)stack.get(temp-1);
			return  element;
		}
		
		protected StackElement pop() {
			StackElement top = null;
			
			if (stack.size()>0) {
				top = (StackElement)stack.get(stack.size()-1);
				stack.remove(stack.size()-1);
				
				if ((top.term instanceof AbsConcept) || 
				(top.term instanceof AbsPredicate)) 
					posComplex.remove(numberOfComplex()-1);
			}
			
			return top;
		}
		protected void removeElement(int i){
			if (stack.size()>0) 
				stack.remove(stack.size()-1);
			}
	}
	
	AbsContentElement ce = null;
	SupportStack stack = new SupportStack();
			
	
	protected void setOntology(Ontology o) {
		ontology = o;
	}
	
	protected AbsContentElement getDecodedContent() {
		return ce;
	}
	
					
	
	protected void openTag(String qName,String localName, Attributes attr) throws OntologyException {
	if ((!qName.equals("rdf:RDF"))&&(!qName.equals("rdf:object"))&&(!qName.equals("rdf:Description"))&&(!qName.equals("rdf:Seq"))&&(!qName.equals("rdf:Bag"))){	
		
		ObjectSchema objectSchema = null;
		String schemaName = null;
			
			// Initialize the decoder		
			if (localName.equals("CONTENT_ELEMENT_LIST")) {
					ContentElementList = true;
					initialited=true;
					stack.clear();
					addToStack("", ContentElementListSchema.getBaseSchema());		
					return;
			}
			
			if (localName.equals("CONTENT_ELEMENT")) {
				if (initialited==false) {
					stack.clear();
					initialited=true;
					ContentElementList = false;
				}
				objectSchema = ObjectSchema.getBaseSchema();
			}
			
			//if (qName.equals("fipa-rdf:type")) {
			if (qName.equals("rdf:type")) {				
				return;
				}
				
			if (qName.equals("rdf:li")) {
				qName=stack.getPreviousComplexElement(0).tag;
				localName=qName.substring(qName.indexOf(":")+1);
				}	
			
						
				if (schemaName==null) {
					schemaName = localName;	
				}				
			// Identify the schema of the current tag
			if (objectSchema==null){
				objectSchema = getRelatedSchema(schemaName);
				}					
				
			if (objectSchema!=null)	addToStack(localName,objectSchema);
			else emptyAgg=true;
		}
										
	}
	
	protected void replaceTerm(String content){
				
		ObjectSchema objectSchema = null;	
		try{	
			objectSchema = getRelatedSchema(content);
			StackElement element = (StackElement)stack.get(stack.size()-1);
			element.term = (AbsObject)objectSchema.newInstance();
		}	
		catch (Exception e){
			e.printStackTrace();
			}			
		
		}
	
	protected void closeTag(String qName,String localName, String content) throws OntologyException {	

		   boolean finalize = false;  
		   //if (qName.equals("fipa-rdf:type")){
		   if (qName.equals("rdf:type")){		
		      	content = content.substring(content.indexOf('#')+1);
			   	replaceTerm(content);
		   	}
		   	
		   	if (qName.equals("rdf:li")) {
				qName=stack.getPreviousComplexElement(0).tag;
				localName=qName.substring(qName.indexOf(":")+1);	
				if(getRelatedSchema(localName)==null && emptyAgg){
					emptyAgg=false;
					return;
				}
						
			}	
		
		   if (qName.equals("fipa-rdf:CONTENT_ELEMENT")&& !ContentElementList) {
		   	finalize = true;
		   }
		   
		   if (qName.equals("fipa-rdf:CONTENT_ELEMENT_LIST")) finalize=true;
		   
		   //if ((!qName.equals("rdf:RDF"))&&(!qName.equals("rdf:object"))&&(!qName.equals("rdf:Description"))&&(!qName.equals("fipa-rdf:type"))&&(!qName.equals("rdf:Seq"))&&(!qName.equals("rdf:Bag"))){
		   if ((!qName.equals("rdf:RDF"))&&(!qName.equals("rdf:object"))&&(!qName.equals("rdf:Description"))&&(!qName.equals("rdf:type"))&&(!qName.equals("rdf:Seq"))&&(!qName.equals("rdf:Bag"))){		   	
		   
		 	    	
		   do { 
				StackElement top = stack.pop();	
				if (stack.size()==0) {
					ce = (AbsContentElement)top.term;
					initialited=false;
				} else {
		  			if (top.term instanceof AbsPrimitive) 
						decodePrimitive(top, content);
					StackElement prevComplex = stack.getPreviousComplexElement(0);
					if (prevComplex.term instanceof AbsAggregate) {
						((AbsAggregate)prevComplex.term).add((AbsTerm)top.term);
					} else if (prevComplex.term instanceof AbsContentElementList) {
						((AbsContentElementList)prevComplex.term).add((AbsContentElement)top.term);
					} else
						AbsHelper.setAttribute(prevComplex.term, top.tag, top.term);
				}
			} while (finalize && stack.size()>0);
			

		}
	}
	
	protected ObjectSchema getRelatedSchema(String qname) throws OntologyException {
		ObjectSchema objectSchema = null;
		boolean emptyAggregate = false;
		try {
			objectSchema = ontology.getSchema(qname);
		} catch (OntologyException e) {
		}	
		
		while (objectSchema==null && !emptyAggregate) {
					StackElement prevElement = stack.getPreviousComplexElement(0);
					ObjectSchema prevComplexSchema =  getConceptSchema(prevElement.term);
					if (prevComplexSchema instanceof AggregateSchema) {
						if (qname.equals(prevElement.tag)) {
							ObjectSchema temp = getConceptSchema(stack.getPreviousComplexElement(1).term); 						
							Facet[] facets = temp.getFacets(qname);
							if (facets!=null){
								objectSchema = getContentType(facets);
								}
							else  {
								objectSchema = null;
								emptyAggregate = true;
							}
						} else {
							stack.pop();
							AbsHelper.setAttribute(stack.getPreviousComplexElement(0).term, prevElement.tag, prevElement.term);
						}	
					} else{
						if (prevComplexSchema.containsSlot(qname)){
							objectSchema = prevComplexSchema.getSchema(qname);
							}
						else {
							objectSchema=null;
							emptyAggregate = true;
								}
						}
		}
		
		return objectSchema;
	}
		
	protected void addToStack(String qname, ObjectSchema objectSchema) throws OntologyException {
			StackElement stackElement = new StackElement();
			stackElement.tag = qname;
			try {
				stackElement.term = (AbsObject)objectSchema.newInstance();
			} catch (OntologyException e) {
				stackElement.term = null;
			}
			stack.push(stackElement);
			
	}

		
	protected ObjectSchema getContentType(Facet[] facets) {
		for (int i=0; i<facets.length-1; i++) {
			Facet temp = facets[i];
			if (temp instanceof TypedAggregateFacet) 
				return ((TypedAggregateFacet)temp).getType();
		}
		return null;	
	}
		
	protected ObjectSchema getConceptSchema(AbsObject absObject) throws OntologyException {
			return ontology.getSchema(absObject.getTypeName());
	}

	protected void decodePrimitive(StackElement element, String content) {

		try {
			ObjectSchema objectSchema = getConceptSchema(stack.getPreviousComplexElement(0).term);		
			PrimitiveSchema attributeSchema = (PrimitiveSchema)objectSchema.getSchema(element.tag);

			String type = attributeSchema.getTypeName();
			
			AbsObject abs = null;
	
	    	if (type.equals(BasicOntology.STRING)) {
      			abs = AbsPrimitive.wrap(content);
        	} 

        	if (type.equals(BasicOntology.BOOLEAN)) {
        		boolean value = content.equals("true");
        		abs = AbsPrimitive.wrap(value);
        	} 

        	if (type.equals(BasicOntology.INTEGER)) {
        		long value = Long.parseLong(content);
        		abs = AbsPrimitive.wrap(value);
        	} 

   			if (type.equals(BasicOntology.FLOAT)) {
   				double value = Double.parseDouble(content);
       			abs = AbsPrimitive.wrap(value);
   			} 

   			if (type.equals(BasicOntology.DATE)) {
            	abs = AbsPrimitive.wrap(ISO8601.toDate(content));
       	 	} 
       	 	
       	 	if (type.equals(BasicOntology.BYTE_SEQUENCE)) {
            	abs = AbsPrimitive.wrap(Base64.decodeBase64(content.getBytes()));
       	 	} 
       	 	
       	 	element.term = abs;
			
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
}	