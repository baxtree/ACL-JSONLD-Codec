package jade.content.lang.jsonld;

import jade.content.abs.AbsAggregate;
import jade.content.abs.AbsConcept;
import jade.content.abs.AbsContentElement;
import jade.content.abs.AbsContentElementList;
import jade.content.abs.AbsHelper;
import jade.content.abs.AbsObject;
import jade.content.abs.AbsPredicate;
import jade.content.abs.AbsPrimitive;
import jade.content.abs.AbsTerm;
import jade.content.lang.Codec;
import jade.content.lang.StringCodec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.rdf.RDFCodec;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.Properties;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class JSONLDCodec extends StringCodec {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String NAME = "JSONLDCodec";
	
	Ontology ontology;
	
	private String ontologyName = null;
	
	private String seqType = null;
	
	boolean description = false;
	
	boolean type = false;
	
	String fullOntologyName = null;
	
	String ontologyNS = null;
	
	String NAMESPACE_ = null;
	
	AbsContentElement ce = null;
	
	boolean initialited = false;
	
	boolean ContentElementList = false;
	
	String encoded=null;
	
	String previousTag=null;
	
	String temp=null;
	
	boolean emptyAgg=false;
	
	String lastValue;
	
	XMLContentHandler handler;
	
	SAXParser parser;
	
	boolean XMLValidation;
	
	static final String JAXP_SCHEMA_LANGUAGE =
	    "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	static final String W3C_XML_SCHEMA =
	    "http://www.w3.org/2001/XMLSchema";
	    
	static final String JAXP_SCHEMA_SOURCE = 
		"http://java.sun.com/xml/jaxp/properties/schemaSource";
	
	static final String NAMESPACE   = 	"\"@context\" : {" +
		"  \"rdf\" : \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"," +
		"  \"rdfs\" : \"http://www.w3.org/TR/1999/PR-rdf-schema-19990303#\"," +
		"  \"fipa-rdf\" : \"http://www.fipa.org/schemas/FIPA-RDF#\"";
	
	public JSONLDCodec() {
		super(NAME);
		initComponents(false);
	}
	
	public JSONLDCodec(boolean pXMLValidation) {
		super(NAME);
		initComponents(pXMLValidation);	
	}	
	
	class XMLErrorHandler implements ErrorHandler {
		
		public void error(SAXParseException exception) {
			exception.printStackTrace();
		}
			
		public void fatalError(SAXParseException exception) {
			exception.printStackTrace();
		}
			
		public void warning(SAXParseException exception) {
			exception.printStackTrace();
		}
	}

	class XMLContentHandler extends DefaultHandler {
		
		String lastValue;
		boolean finished=false;	
		boolean foundType=false;		
		String prevTag=null;
		String validTag=null;
			public void startElement(String namespaceURI,
								 String localName,
								 String qname,
								 Attributes attr) throws SAXException {
			
		
			try {
				lastValue = new String();
				openTag(qname,localName, attr);		
			} catch (Exception e) {
				e.printStackTrace();
				throw new SAXException(e.getMessage());
			}
		}
		
			
		public void endElement(String namespaceURI,
							   String localName,
							   String qname) throws SAXException { 
							   
			
										  
			try {
	   			closeTag(qname,localName, lastValue);
	   		} catch (OntologyException e) {
	   			throw new SAXException(e.getMessage());
	   		}
		}
		
		
			public void characters(char[] ch, 
							   int start,
							   int length) throws SAXException {

			String temp = new String(ch, start, length);
			temp.trim();
			if ((temp.charAt(0)=='\t') && (temp.charAt(temp.length()-1)=='\t'))
				temp = "";
			lastValue = lastValue + temp;
			// Remove unexpected text data like sequence of tabs
			
		}					   	

		
		   		
	}
	
	protected void initComponents(boolean pXMLValidation) {
	
		XMLValidation = pXMLValidation;
		handler = new XMLContentHandler();
		
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(XMLValidation);
			factory.setNamespaceAware(!XMLValidation);
			parser = factory.newSAXParser();
			if (XMLValidation) {
				parser.getXMLReader().setErrorHandler(new XMLErrorHandler());	
				parser.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}	
	
	public String normaliseJSON(StringBuffer sb){
		return sb.toString().replaceAll(",}", "}").replaceAll(",]", "]");
	}

	@Override
	public String encode(AbsContentElement arg0) throws CodecException {
		StringBuffer sb = new StringBuffer();
		AbsObject content = (AbsObject) arg0;
		sb.append("{");
		sb.append(NAMESPACE_);
		encodeWithJSON((AbsObject)content, null, null, null, sb, false, false, false);
	    RDFCodec rdfCodec = new RDFCodec();
		String rDFXMLEncoding = rdfCodec.encode(ontology, arg0);
		sb.append(", \"rdfxml\" : \"" + rDFXMLEncoding.replaceAll("\"", "\\\\\"").replaceAll("\n", "") + "\"");
		sb.append("}");
		return normaliseJSON(sb);
	}
	
	@Override
	public String encode(Ontology ontology, AbsContentElement content)
			throws CodecException {
		ontologyName = ontology.getName();
       	String prop = null;
       	Properties props = new Properties();
        try{ 
			 
			 props.load(new FileInputStream("RDFCodec.properties"));	
			 prop = props.getProperty(ontologyName);	 
  			
        		}
       	 catch (IOException e){
        	e.printStackTrace();
        	}
       
        if (prop!=null)
        	{
				ontologyNS ="\""+ontologyName+"\" : "+"\""+prop+"#"+"\"" ;                    				
				NAMESPACE_= NAMESPACE + "," + ontologyNS+"},";	
				fullOntologyName = prop;
       		}
       	
       	else 
       		NAMESPACE_= NAMESPACE + "},";
       		
        setOntology(ontology);       	
        String temp = encode(content);
        return temp;
	}
	
	void encodeAggregateAsTag(AbsObject content,String memberExpectedType,String tag, StringBuffer sb, boolean sq,boolean last,boolean first) throws Codec.CodecException {
		AbsAggregate absAggregate = (AbsAggregate)content;
		for (int i=0; i < absAggregate.size(); i++) 
			encodeWithJSON(absAggregate.get(i),null, memberExpectedType,tag, sb, false,false,false);			
	}

	void encodeSequenceAsTag(AbsObject content, String memberExpectedType, String tag, StringBuffer sb) throws Codec.CodecException {
		AbsAggregate absAggregate = (AbsAggregate)content;
		int size = absAggregate.size();
		if (size > 0){
			for (int i = 0; i < size; i++) {
				int k = absAggregate.size()-1;
				if (i == 0){
					if (k == 0)
						encodeWithJSON(absAggregate.get(i), null ,memberExpectedType, tag, sb, true, true, true);				
					else
						encodeWithJSON(absAggregate.get(i), null, memberExpectedType, tag, sb, true, false, true);				
					
					}
				else
					if (i != k)					
						encodeWithJSON(absAggregate.get(i), null, memberExpectedType, tag, sb, true, false, false);				
					else
						encodeWithJSON(absAggregate.get(i), null, memberExpectedType, tag, sb, true, true, false);			
			}	
		}
		else {
			if (seqType.equals("sequence")){
				sb.append("[{\"@type\" : \"" + tag +"\"}]");
			}
			else if (seqType.equals("set")){
				sb.append("[{\"@type\" : \"" + tag +"\"}]");
			}
		}		
	}

	public void encodeWithJSON(AbsObject content, ObjectSchema parentSchema,String slotExpectedType, String tag, StringBuffer sb, boolean sq, boolean last, boolean first ) throws Codec.CodecException {
		String startTag;
//    	String closeTag;
		try {

			// Encoding a ContentElementList
			if (content instanceof AbsContentElementList) {
				sb.append("\"fipa-rdf:CONTENT_ELEMENT_LIST\" : [");
				sb.append("{");
				AbsContentElementList absCEList = (AbsContentElementList) content;
				for (int i = 0; i < absCEList.size(); i++) {
					AbsObject temp = (AbsObject) absCEList.get(i);
					encodeWithJSON(temp, null, temp.getTypeName(), null, sb, false, false, false);
				}
				sb.append("}");
				sb.append("],");
				return;
			}

			// Encoding a Primitive
			if (content instanceof AbsPrimitive) {
				AbsPrimitive absPrimitive = (AbsPrimitive) content;
				String typeName = ((AbsObject) absPrimitive).getTypeName();
				String temp = null;
				if (ontologyName != null)
					sb.append("\"" + ontologyName + ":" + tag + "\"");
				else
					sb.append("\"" + tag + "\"");
				sb.append(":");
				// sb.append("<"+ontologyName+":"+tag+">");

				Object v = absPrimitive.getObject();
				if (typeName.equals(BasicOntology.DATE))
					temp = "\"" + ISO8601.toString((Date) v) + "\"";
				else if (typeName.equals(BasicOntology.FLOAT)
						|| typeName.equals(BasicOntology.INTEGER))
					temp = v.toString();
				else if (typeName.equals(BasicOntology.BYTE_SEQUENCE))
					temp = "\"" + String.valueOf(Base64.encodeBase64((byte[]) v)) + "\"";
				else
					temp = "\"" + ((AbsObject) absPrimitive)
							.toString() + "\"";
				sb.append(temp);
				sb.append(",");
				// sb.append("</"+ontologyName+":"+tag+">");
				return;

			}

			// Encoding an Aggregate
			if (content instanceof AbsAggregate) {

				String memberExpectedType = null;

				Facet[] facets = parentSchema.getFacets(tag);
				if (facets != null) {
					for (int i = 0; i < facets.length; i++) {
						if (facets[i] instanceof TypedAggregateFacet) {
							memberExpectedType = ((TypedAggregateFacet) facets[i])
									.getType().getTypeName();
						}
					}
				}

				seqType = content.getTypeName();
				if (!seqType.equals("sequence") && !seqType.equals("set"))
					encodeAggregateAsTag(content, memberExpectedType, tag, sb,
							false, false, false);
				else {
					encodeSequenceAsTag(content, memberExpectedType, tag, sb);
				}
				return;
			}

			// Encoding a Concept
			ObjectSchema currSchema = ontology.getSchema(content.getTypeName());

			if (tag == null) {
				startTag = "fipa-rdf:CONTENT_ELEMENT";
//				closeTag = "fipa-rdf:CONTENT_ELEMENT";
			} else {
				startTag = new String(tag);
//				closeTag = startTag;
			}

			if (slotExpectedType != null) {
				if (!(currSchema.getTypeName().equals(slotExpectedType))) {
					description = true;
					type = true;
				} else if ((tag != null) && (parentSchema == null)) {
					description = true;
					type = true;
				}

			}
			if (startTag.equals("fipa-rdf:CONTENT_ELEMENT")) {
				sb.append("\"" + startTag + "\" : ");
				sb.append("{");
				/*
				 * nspan, change: sb.append("<fipa-rdf:type>");
				 * sb.append(content.getTypeName());
				 * sb.append("</fipa-rdf:type>"); with:
				 */
				sb.append("\"@type\" : ");
				sb.append("\"" + fullOntologyName + "#" + content.getTypeName() + "\"" + ",");

				/* nspan, end */
			} else if (startTag.equals("fipa-rdf:CONTENT_ELEMENT_LIST")) {
				sb.append("\"" + startTag + "\" : ");
				sb.append("[");
			}
			else {
				if (sq && first) {
//					sb.append("{");
					sb.append("\"");
					if (ontologyName != null)
						sb.append(ontologyName + ":" + startTag);
					else
						sb.append("\"" + startTag + "\"");

					// sb.append(ontologyName+":"+startTag);
					sb.append("\" : ");
				} else if (!sq) {
					sb.append("\"");
					if (ontologyName != null)
						sb.append(ontologyName + ":" + startTag);
					else
						sb.append("\"" + startTag + "\"");

					// sb.append(ontologyName+":"+startTag);
					sb.append("\" : ");
				}

				if (!sq)
					sb.append("{");
				else {
					if (first)
						if (seqType.equals("sequence"))
							sb.append("[{");
						else if (seqType.equals("set"))
							sb.append("[{");
				}
				if (type) {
					/*
					 * nspan, change: sb.append("<fipa-rdf:type>");
					 * sb.append(currSchema.getTypeName());
					 * sb.append("</fipa-rdf:type>"); with:
					 */
					sb.append("\"@type\" : ");
					sb.append("\"" + fullOntologyName + "#"
									+ currSchema.getTypeName() + "\"" + ",");
					/* nspan, end */
					type = false;
				}
			}
			
			String[] names = content.getNames();
			for (int i = 0; i < names.length; i++) {
				AbsObject temp = content.getAbsObject(names[i]);
				encodeWithJSON(temp, currSchema, currSchema.getSchema(names[i])
						.getTypeName(), names[i], sb, false, false, false);
			}
			if (startTag.equals("fipa-rdf:CONTENT_ELEMENT")) {
				sb.append("}");
//				sb.append(",");
			} else if (startTag.equals("fipa-rdf:CONTENT_ELEMENT_LIST")) {
				sb.append("}");
				sb.append("]");
//				sb.append(",");
			}
			else {
				if (sq) {
					if (last) {

						if (seqType.equals("sequence"))
							sb.append("}]");
						else if (seqType.equals("set")){
							sb.append("}]");
						}
//						sb.append(",");
					}
					else{
						sb.append("},{");
					}
				}

				else {
					sb.append("}");
					sb.append(",");
				}

			}
		} catch (OntologyException e) {
			e.printStackTrace();
		} catch (Exception e) {
			throw new Codec.CodecException(e.getMessage());
		}
	}
	
	public void setOntology(Ontology o) {
		ontology = o;
		ontologyName = o.getName();
	}
	
	@Override
	public AbsContentElement decode(String content) throws CodecException {
		String rdfxml = null;
		try{
			JSONObject jsonObj = new JSONObject(content);
			rdfxml = jsonObj.getString("rdfxml").toString();
			try {
				parser.parse(new InputSource(new StringReader(rdfxml)), handler);
	 		} catch (Exception e) {
	 			e.printStackTrace();
	 			throw new CodecException(e.getMessage());
	 			
	 		}
		}
		catch(JSONException jsone){
			jsone.printStackTrace();
		}
//		content = getXMLFromJSONLD(content);
		AbsContentElement temp = getDecodedContent();
//		System.out.println("Decoded ace: " + temp);
		return temp;
	}

	@Override
	public AbsContentElement decode(Ontology ontology, String content) throws CodecException {
		try {
			setOntology(ontology);
			if (XMLValidation) {
				System.out.println(ontology.getName().concat(".xsd"));			
				parser.setProperty(JAXP_SCHEMA_SOURCE, ontology.getName().concat(".xsd"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new CodecException(e.getMessage());
		}
		return decode(content);
	}
	
//	TODO: JSONLD => JSON => XML
//	public String getXMLFromJSONLD(String content){
//		
//		return null;
//	}
	
	public void startElement(String namespaceURI, String localName,
			String qname, Attributes attr) throws SAXException {

		try {
			lastValue = new String();
			openTag(qname, localName, attr);
		} catch (Exception e) {
			e.printStackTrace();
			throw new SAXException(e.getMessage());
		}
	}

	public void endElement(String namespaceURI, String localName, String qname)
			throws SAXException {

		try {
			closeTag(qname, localName, lastValue);
		} catch (OntologyException e) {
			throw new SAXException(e.getMessage());
		}
	}

	public void characters(char[] ch, int start, int length)
			throws SAXException {

		String temp = new String(ch, start, length);
		temp.trim();
		if ((temp.charAt(0) == '\t')
				&& (temp.charAt(temp.length() - 1) == '\t'))
			temp = "";
		lastValue = lastValue + temp;
		// Remove unexpected text data like sequence of tabs

	}
	
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
	
	SupportStack stack = new SupportStack();
			
	
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
