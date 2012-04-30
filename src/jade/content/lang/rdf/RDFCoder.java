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
import jade.content.abs.AbsContentElementList;
import jade.content.abs.AbsObject;
import jade.content.abs.AbsPrimitive;
import jade.content.lang.Codec;
import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.schema.Facet;
import jade.content.schema.ObjectSchema;
import jade.content.schema.facets.TypedAggregateFacet;
import jade.lang.acl.ISO8601;

import java.util.Date;

import org.apache.commons.codec.binary.Base64;

/**
 * @author Rosalba Bochicchio - TELECOM ITALIA LAB
 */

class RDFCoder {

	String ontologyName = null;
	Ontology ontology;
	boolean description = false;
	boolean type = false;
	String seqType = null;
	/* nspan, add: */
	String fullOntologyName = null;

	/* nspan, end */

	protected void setOntology(Ontology o, String prop) {
		ontology = o;
		if (prop != null)
			ontologyName = o.getName();
	}

	String normalizeString(String text) {
		String temp;

		if ((text.indexOf("<") > -1) || (text.indexOf(">") > -1)) {
			temp = new String();

			for (int i = 0; i < text.length(); i++)
				switch (text.charAt(i)) {
				case '>':
					temp = temp.concat("&gt;");
					break;
				case '<':
					temp = temp.concat("&lt;");
					break;
				default:
					temp = temp.concat(String.valueOf(text.charAt(i)));
				}
		} else
			temp = text;

		return temp;
	}

	void encodeAggregateAsTag(AbsObject content, String memberExpectedType,
			String tag, StringBuffer sb, boolean sq, boolean last, boolean first)
			throws Codec.CodecException {
		AbsAggregate absAggregate = (AbsAggregate) content;
		for (int i = 0; i < absAggregate.size(); i++)
			encodeAsTag(absAggregate.get(i), null, memberExpectedType, tag, sb,
					false, false, false);
	}

	void encodeSequenceAsTag(AbsObject content, String memberExpectedType,
			String tag, StringBuffer sb) throws Codec.CodecException {
		AbsAggregate absAggregate = (AbsAggregate) content;
		int size = absAggregate.size();
		if (size > 0) {
			for (int i = 0; i < size; i++) {
				int k = absAggregate.size() - 1;
				if (i == 0) {
					if (k == 0)
						encodeAsTag(absAggregate.get(i), null,
								memberExpectedType, tag, sb, true, true, true);
					else
						encodeAsTag(absAggregate.get(i), null,
								memberExpectedType, tag, sb, true, false, true);

				} else if (i != k)
					encodeAsTag(absAggregate.get(i), null, memberExpectedType,
							tag, sb, true, false, false);
				else
					encodeAsTag(absAggregate.get(i), null, memberExpectedType,
							tag, sb, true, true, false);
			}
		} else {
			if (seqType.equals("sequence")) {
				sb.append("<" + "rdf:Seq" + ">");
				sb.append("<" + "rdf:li" + ">");
				sb.append("<" + "rdf:object" + ">");
				sb.append("<" + tag + ">");
				sb.append("</" + tag + ">");
				sb.append("</" + "rdf:object" + ">");
				sb.append("</" + "rdf:li" + ">");
				sb.append("</" + "rdf:Seq" + ">");
			} else if (seqType.equals("set")) {
				sb.append("<" + "rdf:Bag" + ">");
				sb.append("<" + "rdf:li" + ">");
				sb.append("<" + "rdf:object" + ">");
				sb.append("<" + tag + ">");
				sb.append("</" + tag + ">");
				sb.append("</" + "rdf:object" + ">");
				sb.append("</" + "rdf:li" + ">");
				sb.append("</" + "rdf:Bag" + ">");
			}

		}
	}

	void encodeAsTag(AbsObject content, ObjectSchema parentSchema,
			String slotExpectedType, String tag, StringBuffer sb, boolean sq,
			boolean last, boolean first) throws Codec.CodecException {

		boolean hasChild = false;
		boolean hasAttributes = false;
		String startTag;
		String closeTag;

		try {

			// Encoding a ContentElementList
			if (content instanceof AbsContentElementList) {
				sb.append("<fipa-rdf:CONTENT_ELEMENT_LIST>");
				sb.append("<rdf:Description>");
				AbsContentElementList absCEList = (AbsContentElementList) content;
				for (int i = 0; i < absCEList.size(); i++) {
					AbsObject temp = (AbsObject) absCEList.get(i);
					encodeAsTag(temp, null, temp.getTypeName(), null, sb,
							false, false, false);
				}
				sb.append("</rdf:Description>");
				sb.append("</fipa-rdf:CONTENT_ELEMENT_LIST>");
				return;
			}

			// Encoding a Primitive
			if (content instanceof AbsPrimitive) {
				AbsPrimitive absPrimitive = (AbsPrimitive) content;
				String typeName = ((AbsObject) absPrimitive).getTypeName();
				String temp = null;
				if (ontologyName != null)
					sb.append("<" + ontologyName + ":" + tag + ">");
				else
					sb.append("<" + tag + ">");

				// sb.append("<"+ontologyName+":"+tag+">");

				Object v = absPrimitive.getObject();
				if (typeName.equals(BasicOntology.DATE))
					temp = ISO8601.toString((Date) v);
				else if (typeName.equals(BasicOntology.FLOAT)
						|| typeName.equals(BasicOntology.INTEGER))
					temp = v.toString();
				else if (typeName.equals(BasicOntology.BYTE_SEQUENCE))
					temp = String.valueOf(Base64.encodeBase64((byte[]) v));
				else
					temp = normalizeString(((AbsObject) absPrimitive)
							.toString());
				sb.append(temp);
				if (ontologyName != null)
					sb.append("</" + ontologyName + ":" + tag + ">");
				else
					sb.append("</" + tag + ">");

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
				sb.append("<");
				sb.append(startTag);
				sb.append(">");
				sb.append("<rdf:Description>");
				/*
				 * nspan, change: sb.append("<fipa-rdf:type>");
				 * sb.append(content.getTypeName());
				 * sb.append("</fipa-rdf:type>"); with:
				 */
				sb.append("<rdf:type>");
				sb.append(fullOntologyName + "#" + content.getTypeName());
				sb.append("</rdf:type>");
				/* nspan, end */
			} else if (startTag.equals("fipa-rdf:CONTENT_ELEMENT_LIST")) {
				sb.append("<");
				sb.append(startTag);
				sb.append(">");
			}

			else {
				if (sq && first) {
					sb.append("<");
					if (ontologyName != null)
						sb.append(ontologyName + ":" + startTag);
					else
						sb.append(startTag);
					sb.append(">");
				} else if (!sq) {
					sb.append("<");
					if (ontologyName != null)
						sb.append(ontologyName + ":" + startTag);
					else
						sb.append(startTag);
					sb.append(">");
				}
				if (!sq)
					sb.append("<rdf:Description>");
				else {
					if (first)
						if (seqType.equals("sequence"))
							sb.append("<" + "rdf:Seq" + ">");
						else if (seqType.equals("set"))
							sb.append("<" + "rdf:Bag" + ">");

					sb.append("<" + "rdf:li" + ">");
					sb.append("<" + "rdf:object" + ">");
				}
				if (type) {
					/*
					 * nspan, change: sb.append("<fipa-rdf:type>");
					 * sb.append(currSchema.getTypeName());
					 * sb.append("</fipa-rdf:type>"); with:
					 */
					sb.append("<rdf:type>");
					sb
							.append(fullOntologyName + "#"
									+ currSchema.getTypeName());
					sb.append("</rdf:type>");
					/* nspan, end */
					type = false;
				}
			}

			String[] names = content.getNames();
			for (int i = 0; i < names.length; i++) {
				AbsObject temp = content.getAbsObject(names[i]);
				encodeAsTag(temp, currSchema, currSchema.getSchema(names[i])
						.getTypeName(), names[i], sb, false, false, false);
			}
			if (startTag.equals("fipa-rdf:CONTENT_ELEMENT")) {
				sb.append("</rdf:Description>");
				sb.append("</");
				sb.append(closeTag);
				sb.append(">");
			} else if (startTag.equals("fipa-rdf:CONTENT_ELEMENT_LIST")) {
				sb.append("</rdf:Description>");
				sb.append("</");
				sb.append(closeTag);
				sb.append(">");
			}

			else {
				if (sq) {
					sb.append("</rdf:object>");
					sb.append("</rdf:li>");
					if (last) {

						if (seqType.equals("sequence"))
							sb.append("</rdf:Seq>");
						else if (seqType.equals("set"))
							sb.append("</rdf:Bag>");

						sb.append("</");

						if (ontologyName != null)
							sb.append(ontologyName + ":" + closeTag);
						else
							sb.append(closeTag);

						sb.append(">");
					}
				}

				else {
					sb.append("</rdf:Description>");
					sb.append("</");

					if (ontologyName != null)
						sb.append(ontologyName + ":" + closeTag);
					else
						sb.append(closeTag);
					// sb.append(ontologyName+":"+closeTag);
					sb.append(">");
				}

			}
		} catch (OntologyException e) {
			e.printStackTrace();
		} catch (Exception e) {
			throw new Codec.CodecException(e.getMessage());
		}
	}

}