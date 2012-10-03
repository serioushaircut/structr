/*
 *  Copyright (C) 2010-2012 Axel Morgner
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



package org.structr.websocket.command;

import org.apache.commons.lang.StringUtils;

import org.neo4j.graphdb.Direction;

import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.web.common.PageHelper;
import org.structr.web.common.RelationshipHelper;
import org.structr.web.entity.Page;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class RemoveCommand extends AbstractCommand {

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final Command deleteRel               = Services.command(securityContext, DeleteRelationshipCommand.class);
		String id                             = webSocketData.getId();

		// String parentId          = (String) webSocketData.getNodeData().get("id");
		// final String componentId = (String) webSocketData.getNodeData().get("componentId");
		// The tree address of the node to remove
		final String treeAddress = (String) webSocketData.getNodeData().get("treeAddress");

		if (id != null) {

			final AbstractNode nodeToRemove = getNode(id);

			if (nodeToRemove != null) {

				final List<AbstractRelationship> rels = nodeToRemove.getRelationships(RelType.CONTAINS, Direction.INCOMING);

				try {

					Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

						@Override
						public Object execute() throws FrameworkException {

							List<AbstractRelationship> relsToReorder = new ArrayList<AbstractRelationship>();
							String parentTreeAddress                 = PageHelper.getParentTreeAddress(treeAddress);

							// Iterate through all incoming CONTAINS relationships
							for (AbstractRelationship rel : rels) {

								// Check if this relationship has a position property for the parent's treeAddress
								if (treeAddress == null || rel.getRelationship().hasProperty(parentTreeAddress)) {
									
									List<AbstractRelationship> siblingRels = (List<AbstractRelationship>) rel.getStartNode().getOutgoingRelationships(RelType.CONTAINS);

									relsToReorder.addAll(siblingRels);

									Long pos = rel.getLongProperty(parentTreeAddress);

									if (pos == null) {

										deleteRel.execute(rel);
										relsToReorder.remove(rel);
									} else {

										rel.removeProperty(parentTreeAddress);

										// RelationshipHelper.untagOutgoingRelsFromPageId(nodeToRemove, nodeToRemove, pageId, pageId);
										// If no pageId property is left, remove relationship
										if (!hasTreeAdresses(securityContext, rel)) {

											deleteRel.execute(rel);
											relsToReorder.remove(rel);
											
											//break;

										}

									}

								}

							}

							// Re-order relationships
							RelationshipHelper.reorderRels(relsToReorder, parentTreeAddress);

							return null;

						}

					});
					webSocketData.setTreeAddress(treeAddress);

				} catch (FrameworkException fex) {

					getWebSocket().send(MessageBuilder.status().code(400).message(fex.getMessage()).build(), true);

				}

			} else {

				getWebSocket().send(MessageBuilder.status().code(404).build(), true);
			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(400).message("Add needs id and data.id!").build(), true);
		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "REMOVE";

	}

	private boolean hasTreeAdresses(final SecurityContext securityContext, final AbstractRelationship rel) throws FrameworkException {

		Command searchNode = Services.command(securityContext, SearchNodeCommand.class);
		long count         = 0;

		for (Entry entry : rel.getProperties().entrySet()) {

			String key = (String) entry.getKey();

			// Object val = entry.getValue();
			// Check if key is a valid tree address (UUID format)
			if (PageHelper.isTreeAddress(key)) {

				String pageId               = PageHelper.getPageIdFromTreeAddress(key);
				List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();

				attrs.add(Search.andExactType(Page.class.getSimpleName()));
				attrs.add(Search.andExactUuid(pageId));

				Result results = (Result) searchNode.execute(null, false, false, attrs);

				if (results != null && !results.isEmpty()) {

					count++;
				} else {

					// UUID, but not page found: Remove this property
					rel.removeProperty(key);
				}

			}

		}

		return count > 0;

	}

}
