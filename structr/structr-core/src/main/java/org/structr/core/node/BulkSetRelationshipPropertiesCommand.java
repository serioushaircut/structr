/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.node;

import java.util.LinkedList;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;

import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchRelationshipCommand;

//~--- classes ----------------------------------------------------------------

/**
 * This command takes a property set as parameter.
 * 
 * Sets the properties found in the property set on all relationships matching the type.
 * If no type property is found, set the properties on all relationships.
 *
 * @author Axel Morgner
 */
public class BulkSetRelationshipPropertiesCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(BulkSetRelationshipPropertiesCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		final GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		final RelationshipFactory relationshipFactory      = (RelationshipFactory) arguments.get("relationshipFactory");
                final Command searchRel = Services.command(SecurityContext.getSuperUserInstance(), SearchRelationshipCommand.class);
                
                if (!((parameters != null) && (parameters.length == 1) && (parameters[0] instanceof Map) && !((Map) parameters[0]).isEmpty())) {
                        
                        throw new IllegalArgumentException("This command requires one argument of type Map. Map must not be empty.");
                        
                }
                
                final Map<PropertyKey, Object> properties = (Map<PropertyKey, Object>) parameters[0];

		if (graphDb != null) {

			final Command transactionCommand = Services.command(securityContext, TransactionCommand.class);

			transactionCommand.execute(new BatchTransaction() {

				@Override
				public Object execute(Transaction tx) throws FrameworkException {

					long n                  = 0L;
                                        List<AbstractRelationship> rels = null;
                                        
                                        if (properties.containsKey(AbstractRelationship.HiddenKey.combinedType)) {
                                                
                                                List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();
                                                attrs.add(Search.andExactType((String) properties.get(AbstractRelationship.HiddenKey.combinedType)));
                                                
                                                rels = (List<AbstractRelationship>) searchRel.execute(attrs);
                                                properties.remove(AbstractRelationship.HiddenKey.combinedType);
                                                
                                        } else {
                                        
                                                rels = (List<AbstractRelationship>) relationshipFactory.createRelationships(securityContext, GlobalGraphOperations.at(graphDb).getAllRelationships());
                                        }

					for (AbstractRelationship rel : rels) {

						// Treat only "our" nodes
						if (rel.getStringProperty(AbstractRelationship.Key.uuid) != null) {

							for (Entry entry : properties.entrySet()) {
                                                                
                                                                PropertyKey key	= (PropertyKey) entry.getKey();
                                                                Object val	= entry.getValue();
                                                                
                                                                rel.setProperty(key, val);
                                                                
                                                        }

							if (n > 1000 && n % 1000 == 0) {

								logger.log(Level.INFO, "Set properties on {0} relationships, committing results to database.", n);
								tx.success();
								tx.finish();

								tx = graphDb.beginTx();

								logger.log(Level.FINE, "######## committed ########", n);

							}

							n++;

						}
					}
                                        
                                        logger.log(Level.INFO, "Finished setting properties on {0} relationships", n);

					return null;
				}

			});

		}

		return null;
	}
}
