/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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



package org.structr.web.common;

import org.apache.commons.lang.StringUtils;

import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.*;
import org.structr.web.entity.Component;
import org.structr.web.entity.Content;
import org.structr.web.entity.Element;
import org.structr.web.entity.Page;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class PageHelper {

	public static final String REQUEST_CONTAINS_UUID_IDENTIFIER = "request_contains_uuids";
	private static final Logger logger                          = Logger.getLogger(PageHelper.class.getName());

	//~--- methods --------------------------------------------------------

	public static String expandTreeAddress(final String treeAddress, final AbstractRelationship relationship) {

		Long position = relationship.getLongProperty(treeAddress);

		if (position == null) {

			position = relationship.getLongProperty(getPageIdFromTreeAddress(treeAddress));
		}

		return treeAddress.concat("_").concat(position.toString());

	}

	private static void sortByPosition(final List<AbstractRelationship> rels, final String treeAddress) {

		Collections.sort(rels, new Comparator<AbstractRelationship>() {

			@Override
			public int compare(AbstractRelationship o1, AbstractRelationship o2) {

				Long pos1 = getPosition(o1, treeAddress);
				Long pos2 = getPosition(o2, treeAddress);

				return pos1.compareTo(pos2);

			}

		});

	}

	//~--- get methods ----------------------------------------------------

	public static AbstractNode getNodeById(SecurityContext securityContext, String id) {

		if (id == null) {

			return null;
		}

		try {

			return (AbstractNode) Services.command(securityContext, GetNodeByIdCommand.class).execute(id);

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Unable to load node with id {0}, {1}", new java.lang.Object[] { id, t.getMessage() });

		}

		return null;

	}

	/**
	 * Find all pages which contain the given html element node
	 *
	 * @param securityContext
	 * @param node
	 * @return
	 */
	public static List<Page> getPages(SecurityContext securityContext, final AbstractNode node) {

		List<Page> pages = new LinkedList<Page>();
		AbstractNode pageNode;
		List<AbstractRelationship> rels = node.getIncomingRelationships(RelType.CONTAINS);

		for (AbstractRelationship rel : rels) {

			for (String key : rel.getProperties().keySet()) {

				pageNode = getNodeById(securityContext, key);

				if (pageNode != null && pageNode instanceof Page) {

					pages.add((Page) pageNode);
				}

			}

		}

		return pages;

	}

	public static String getPageIdFromTreeAddress(final String treeAddress) {

		return StringUtils.substringBefore(treeAddress, "_");

	}

	public static String getParentTreeAddress(final String treeAddress) {

		return StringUtils.substringBeforeLast(treeAddress, "_");

	}

	public static List<AbstractRelationship> getChildRelationships(final HttpServletRequest request, final AbstractNode node, final String treeAddress, final String componentId) {

		List<AbstractRelationship> rels = new LinkedList<AbstractRelationship>();

		for (AbstractRelationship rel : node.getOutgoingRelationships(RelType.CONTAINS)) {

			boolean hasTreeAddress = rel.getProperty(treeAddress) != null;
			String pageId          = PageHelper.getPageIdFromTreeAddress(treeAddress);
			Long positionForPageId = rel.getLongProperty(pageId);

			if (!hasTreeAddress && positionForPageId != null) {

				// Seemless migration from pageId to treeAddress scheme:
				// Persist the paths of the current node as relationship attributes
				Set<String> paths = (Set<String>) node.getProperty(Element.UiKey.paths);

				for (String path : paths) {

					try {

						rel.setProperty(path, positionForPageId);
						rel.removeProperty(pageId);
						logger.log(Level.INFO, "Set property {0} for path {1} and removed old pageId property {2}", new Object[] { positionForPageId, path, pageId });

					} catch (FrameworkException ex) {

						logger.log(Level.SEVERE, "Could not set property " + positionForPageId + " for path " + path, ex);

					}

				}
			}

			if ((treeAddress == null) || ((treeAddress != null) && rel.getRelationship().hasProperty(treeAddress)) || rel.getRelationship().hasProperty("*")) {

				AbstractNode endNode = rel.getEndNode();

				if (endNode == null || (endNode instanceof Component && !isVisible(request, endNode, rel, componentId))) {

					continue;
				}

				if ((componentId != null) && ((endNode instanceof Content) || (endNode instanceof Component))) {

					// Add content nodes if they don't have the data-key property set
					if (endNode instanceof Content && endNode.getStringProperty("data-key") == null) {

						rels.add(rel);

						// Add content or component nodes if rel's componentId attribute matches

					} else if (componentId.equals(rel.getStringProperty(Component.Key.componentId.name()))) {

						rels.add(rel);
					}
				} else {

					rels.add(rel);
				}

			}

		}

		if (treeAddress != null) {

			sortByPosition(rels, treeAddress);
		}

		return rels;

	}

	private static long getPosition(final AbstractRelationship relationship, final String treeAddress) {

//              final Relationship rel = relationship.getRelationship();
		long position = 0;

		try {

//                      Map<Integer, Relationship> sortedRelationshipMap = new TreeMap<Integer, Relationship>();
			Object prop = null;
			final String key;

			// "*" is a wildcard for "matches any page id"
			// TOOD: use pattern matching here?
			if (relationship.getProperty("*") != null) {

				prop = relationship.getProperty("*");
				key  = "*";

			} else if (relationship.getProperty(treeAddress) != null) {

				prop = relationship.getLongProperty(treeAddress);
				key  = treeAddress;

			} else {

				key = null;
			}

			if ((key != null) && (prop != null)) {

				if (prop instanceof Long) {

					position = (Long) prop;
				} else if (prop instanceof Integer) {

					position = ((Integer) prop).longValue();
				} else if (prop instanceof String) {

					position = Long.parseLong((String) prop);
				} else {

					throw new java.lang.IllegalArgumentException("Expected Long, Integer or String");
				}

			}
		} catch (Throwable t) {

			// fail fast, no check
			logger.log(Level.SEVERE, "While reading property " + treeAddress, t);
		}

		return position;
	}

	public static String getFirstPath(final AbstractNode node) {

		Set<String> paths = (Set<String>) node.getProperty(Element.UiKey.paths);

		if (paths != null && !paths.isEmpty()) {

			return paths.iterator().next();
		} else {

			return null;
		}

	}

	private static boolean hasAttribute(HttpServletRequest request, String key) {

		return (key != null) && (request.getAttribute(key) != null);

	}

	public static boolean isVisible(HttpServletRequest request, AbstractNode node, AbstractRelationship incomingRelationship, String parentComponentId) {

		if (request == null) {

			return true;
		}

		// check if component is in "list" mode
		if (node instanceof Component) {

			Boolean requestContainsUuidsValue = (Boolean) request.getAttribute(REQUEST_CONTAINS_UUID_IDENTIFIER);
			boolean requestContainsUuids      = false;

			if (requestContainsUuidsValue != null) {

				requestContainsUuids = requestContainsUuidsValue.booleanValue();
			}

			String componentId = node.getStringProperty(AbstractNode.Key.uuid);

			// new default behaviour: make all components visible
			// only filter if uuids are present in the request URI
			// and we are examining a top-level component (children
			// of filtered components are not reached anyway)
			if (requestContainsUuids) {

				if (hasAttribute(request, componentId) || (parentComponentId != null)) {

					return true;
				}

				return false;

			} else {

				return true;
			}

		}

		// we can return false here by default, as we're only examining nodes of type Component
		return false;

	}

	/**
	 * Check if given string is a valid tree address
	 *
	 * @param key
	 * @return
	 */
	public static boolean isTreeAddress(final String key) {

		return StringUtils.substringBefore(key, "_").matches("[a-zA-Z0-9]{32}");

	}

}
