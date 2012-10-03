/*
 *  Copyright (C) 2010-2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.websocket.command;

import org.neo4j.graphdb.Direction;

import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.web.common.RelationshipHelper;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to sort a list of nodes.
 * @author Axel Morgner
 */
public class SortCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(SortCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		Map<String, Object> nodeData = webSocketData.getNodeData();
		String treeAddress           = webSocketData.getTreeAddress();

		for (String id : nodeData.keySet()) {

			AbstractNode nodeToMove         = getNode(id);
			Long newPos                     = Long.parseLong((String) nodeData.get(id));
			List<AbstractRelationship> rels = nodeToMove.getRelationships(RelType.CONTAINS, Direction.INCOMING);

			for (AbstractRelationship rel : rels) {

				try {

					Long oldPos = rel.getLongProperty(treeAddress);

					if ((oldPos != null) && !(oldPos.equals(newPos))) {

						rel.setProperty(treeAddress, newPos);

						AbstractNode endNode               = rel.getEndNode();
						List<AbstractRelationship> subRels = endNode.getOutgoingRelationships(RelType.CONTAINS);

						// propagate position index change to all subtrees
						RelationshipHelper.readdressRels(subRels, treeAddress + "_" + oldPos, treeAddress + "_" + newPos);

					}

				} catch (FrameworkException fex) {

					fex.printStackTrace();

				}

			}

		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "SORT";

	}

}
