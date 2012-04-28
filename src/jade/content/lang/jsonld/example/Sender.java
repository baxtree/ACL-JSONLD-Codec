/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A.

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation,
version 2.1 of the License.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
*****************************************************************/

package jade.content.lang.jsonld.example;

import jade.content.lang.Codec;
import jade.content.lang.jsonld.JSONLDCodec;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import jade.util.leap.ArrayList;
import jade.util.leap.List;
import examples.rdfcontent.ontology.Address;
import examples.rdfcontent.ontology.FatherOf;
import examples.rdfcontent.ontology.Man;
import examples.rdfcontent.ontology.PeopleOntology;

public class Sender extends Agent {
    // This agent speaks the RDF language
    private Codec          codec    = new JSONLDCodec();
    // This agent understands terms about people relationships
    private Ontology   ontology = PeopleOntology.getInstance();
    
    private static Logger logger = Logger.getMyLogger(Sender.class.getName());
    
    class SenderBehaviour extends SimpleBehaviour {
	private boolean finished = false;
	
	//private static Logger logger = Logger.getMyLogger(Sender.class.getName());

	public SenderBehaviour(Agent a) { super(a); }

	public boolean done() { return finished; }

	public void action() {
	    try {
		// Preparing the message
		if(logger.isLoggable(Logger.FINE))
			logger.log(Logger.FINE, "[" + getLocalName() + "] Creating inform message with content fatherOf(man :name John :address London, (man :name Bill :address Paris)");

		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		AID receiver = new AID("receiver", false);

		msg.setSender(getAID());
		msg.addReceiver(receiver);
		msg.setLanguage(codec.getName());
		msg.setOntology(ontology.getName());

		// The message informs that John (who lives in London) 
		// is the father of Bill (who lives in Paris) and Tony 
		// (who lives in Naples) .

		Man john = new Man();
		Man bill = new Man();
		Man tony = new Man();
		
		john.setName("John");
		bill.setName("Bill");
		tony.setName("Tony");

		Address johnAddress = new Address();
		johnAddress.setCity("London");
		johnAddress.setStreet("John Kennedy Street");
		johnAddress.setNumber(3);
		john.setAddress(johnAddress);

		Address billAddress = new Address();
		billAddress.setCity("Paris");
		billAddress.setStreet("Lionel");
		billAddress.setNumber(24);
		bill.setAddress(billAddress);
		
		Address tonyAddress = new Address();
		tonyAddress.setCity("Naples");
		tonyAddress.setStreet("Via Bologna");
		tonyAddress.setNumber(11);		
		tony.setAddress(tonyAddress);


		FatherOf fatherOf = new FatherOf();
		fatherOf.setFather(john);

		List children = new ArrayList();
		children.add(bill);
		children.add(tony);
		fatherOf.setChildren(children);
		
		
		// Fill the content of the message
		myAgent.getContentManager().fillContent(msg, fatherOf);
				
		// Send the message
		if(logger.isLoggable(Logger.INFO)){
		logger.log(Logger.INFO, "[" + getLocalName() + "] Sending message. RDF content is:");
		logger.log(Logger.INFO,msg.getContent());
		}
		send(msg);
	    } catch(Exception e) { 
	    	if(logger.isLoggable(Logger.WARNING))
	    		logger.log(Logger.WARNING,"Sender: error in sending message");
	    	e.printStackTrace(); }

	    finished = true;
	}
    }

    protected void setup() {
	getContentManager().registerLanguage(codec);
	getContentManager().registerOntology(ontology);

	addBehaviour(new SenderBehaviour(this));
    }
}
