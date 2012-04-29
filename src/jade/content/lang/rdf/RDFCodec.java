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

import jade.content.lang.StringCodec;

import java.io.StringReader;

import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.abs.AbsContentElement;
import jade.content.abs.AbsObject;
import jade.util.*;

import java.util.*;
import java.io.*;
import java.lang.*;

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * @author Rosalba Bochicchio - TELECOM ITALIA LAB
 */

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

	RDFDecoder decoder;
	String lastValue;
	static boolean finished = false;
	boolean foundType = false;
	String prevTag = null;
	String validTag = null;

	protected void setXMLDecoder(RDFDecoder d) {
		decoder = d;
	}

	public void startElement(String namespaceURI, String localName,
			String qname, Attributes attr) throws SAXException {

		try {
			lastValue = new String();
			decoder.openTag(qname, localName, attr);
		} catch (Exception e) {
			e.printStackTrace();
			throw new SAXException(e.getMessage());
		}
	}

	public void endElement(String namespaceURI, String localName, String qname)
			throws SAXException {

		try {
			decoder.closeTag(qname, localName, lastValue);
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

}

public class RDFCodec extends StringCodec {

	static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

	static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

	static final String XML_VERSION = "<?xml version=\"1.0\"?>\n";

	static final String NAMESPACE = "<rdf:RDF \n"
			+ "  xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
			+ "  xmlns:rdfs=\"http://www.w3.org/TR/1999/PR-rdf-schema-19990303#\""
			+ "  xmlns:fipa-rdf=\"http://www.fipa.org/schemas/FIPA-RDF#\"\n";

	static final String TAIL = "</rdf:RDF>";
	public static final String NAME = "RDFCodec";

	boolean XMLValidation;
	XMLContentHandler handler;
	SAXParser parser;
	RDFDecoder decoder;
	RDFCoder coder;
	String ontologyName = null;
	String ontologyNS = null;
	String NAMESPACE_ = null;

	public RDFCodec() {
		super(NAME);
		initComponents(false);
	}

	protected void initComponents(boolean pXMLValidation) {

		XMLValidation = pXMLValidation;
		decoder = new RDFDecoder();
		coder = new RDFCoder();
		handler = new XMLContentHandler();
		handler.setXMLDecoder(decoder);

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

	public RDFCodec(boolean pXMLValidation) {
		super(NAME);
		initComponents(pXMLValidation);
	}

	/**
	 * Encodes a content into a string.
	 * 
	 * @param content
	 *            the content as an abstract descriptor.
	 * @return the content as a string.
	 * @throws CodecException
	 */
	public String encode(AbsContentElement content) throws CodecException {
		StringBuffer sb = new StringBuffer();
		sb.append(XML_VERSION);
		sb.append(NAMESPACE_);
		sb.append("<rdf:object>");
		coder.encodeAsTag((AbsObject) content, null, null, null, sb, false,
				false, false);
		sb.append("</rdf:object>");
		sb.append(TAIL);
		return sb.toString();
	}

	/**
	 * Encodes a content into a string using a given ontology.
	 * 
	 * @param ontology
	 *            the ontology
	 * @param content
	 *            the content as an abstract descriptor.
	 * @return the content as a string.
	 * @throws CodecException
	 */
	public String encode(Ontology ontology, AbsContentElement content)
			throws CodecException {

		ontologyName = ontology.getName();

		// If the file RDFCodec.properties exists
		// reads it to get namespace location
		// and sets XMLValidation true
		// the user has to edit a file RDF.properties as follows
		// OntologyName = namespace location
		// if the file doesn't exist no namespace and no validation are used is
		// used

		String prop = null;
		Properties props = new Properties();
		try {

			props.load(new FileInputStream("RDFCodec.properties"));
			prop = props.getProperty(ontologyName);

		} catch (IOException e) {
			e.printStackTrace();
		}

		if (prop != null) {
			ontologyNS = "  xmlns:" + ontologyName + "=" + "\"" + prop + "#"
					+ "\"";
			NAMESPACE_ = NAMESPACE + ontologyNS + ">";
		}

		else
			NAMESPACE_ = NAMESPACE + ">";

		coder.setOntology(ontology, prop);

		String temp = encode(content);
		return temp;
	}

	/**
	 * Decodes the content to an abstract description.
	 * 
	 * @param content
	 *            the content as a string.
	 * @return the content as an abstract description.
	 * @throws CodecException
	 */
	public AbsContentElement decode(String content) throws CodecException {

		try {
			parser.parse(new InputSource(new StringReader(content)), handler);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CodecException(e.getMessage());

		}

		AbsContentElement temp = decoder.getDecodedContent();
		return temp;
	}

	/**
	 * . Decodes the content to an abstract description using a given ontology.
	 * 
	 * @param ontology
	 *            the ontology.
	 * @param content
	 *            the content as a string.
	 * @return the content as an abstract description.
	 * @throws CodecException
	 */
	public AbsContentElement decode(Ontology o, String content)
			throws CodecException {

		try {
			decoder.setOntology(o);
			if (XMLValidation) {
				System.out.println(o.getName().concat(".xsd"));
				parser.setProperty(JAXP_SCHEMA_SOURCE, o.getName().concat(
						".xsd"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new CodecException(e.getMessage());
		}

		return decode(content);
	}

}