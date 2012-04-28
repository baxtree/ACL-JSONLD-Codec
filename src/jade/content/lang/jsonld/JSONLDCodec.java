package jade.content.lang.jsonld;

import jade.content.abs.AbsAggregate;
import jade.content.abs.AbsContentElement;
import jade.content.abs.AbsContentElementList;
import jade.content.abs.AbsObject;
import jade.content.abs.AbsPrimitive;
import jade.content.lang.Codec;
import jade.content.lang.StringCodec;
import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.schema.Facet;
import jade.content.schema.ObjectSchema;
import jade.content.schema.facets.TypedAggregateFacet;
import jade.lang.acl.ISO8601;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;

public class JSONLDCodec extends StringCodec {
	
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
	
	static final String NAMESPACE   = 	"\"@context\" : {" +
		"  \"rdf\" : \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"," +
		"  \"rdfs\" : \"http://www.w3.org/TR/1999/PR-rdf-schema-19990303#\"," +
		"  \"fipa-rdf\" : \"http://www.fipa.org/schemas/FIPA-RDF#\"";
	
	public JSONLDCodec() {
		super(NAME);
	}

	@Override
	public AbsContentElement decode(String arg0) throws CodecException {
		AbsContentElement temp = getDecodedContent(arg0);
		return temp;
	}

	@Override
	public AbsContentElement decode(Ontology ontology, String content) throws CodecException {
		setOntology(ontology);
		return decode(content);
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
		sb.append("}");
		return normaliseJSON(sb);
	}
	
	@Override
	public String encode(Ontology ontology, AbsContentElement content)
			throws CodecException {
		ontologyName = ontology.getName();
        
        //If the file RDFCodec.properties exists
		//reads it to get namespace location 
		//and sets XMLValidation true
		//the user has to edit a file RDF.properties as follows
		//OntologyName = namespace location
		//if the file doesn't exist no namespace and no validation are used is used
		
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
    	String closeTag;
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
				closeTag = "fipa-rdf:CONTENT_ELEMENT";
			} else {
				startTag = new String(tag);
				closeTag = startTag;
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
	
	public AbsContentElement getDecodedContent(String content){
		return null;
	}

	public void setOntology(Ontology o) {
		ontology = o;
		ontologyName = o.getName();
	}
	
}
