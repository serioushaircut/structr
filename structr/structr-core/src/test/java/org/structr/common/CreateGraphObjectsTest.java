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



package org.structr.common;

import org.neo4j.graphdb.RelationshipType;

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Cache;
import org.structr.core.entity.Category;
import org.structr.core.entity.File;
import org.structr.core.entity.Folder;
import org.structr.core.entity.GenericNode;
import org.structr.core.entity.GenericRelationship;
import org.structr.core.entity.Image;
import org.structr.core.entity.Location;
import org.structr.core.entity.NodeList;
import org.structr.core.entity.Person;
import org.structr.core.entity.PlainText;
import org.structr.core.entity.PrincipalImpl;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.node.StructrTransaction;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.entity.TestTwo;

//~--- classes ----------------------------------------------------------------

/**
 * Test basic create operations with graph objects (nodes, relationships)
 *
 * All tests are executed in superuser context
 *
 * @author Axel Morgner
 */
public class CreateGraphObjectsTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(CreateGraphObjectsTest.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {
		super.test00DbAvailable();
	}
	
	public void test01CreateNode() {

		try {

			AbstractNode node;

			try {

				// Create node out of transaction => should give a NotInTransactionException
				createNodeCommand.execute();
				fail("Should have raised an org.neo4j.graphdb.NotInTransactionException");
			} catch (org.neo4j.graphdb.NotInTransactionException e) {}

			final Map<PropertyKey, Object> props = new HashMap<PropertyKey, Object>();

			props.put(AbstractNode.Key.type, "UnknownTestTypeÄÖLß");

			try {

				// Try to create node without parameters => should fail
				createNodeCommand.execute();
				fail("Should have raised an org.neo4j.graphdb.NotInTransactionException");
			} catch (org.neo4j.graphdb.NotInTransactionException e) {}

			node = (AbstractNode) transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					// Create node with a type which has no entity class => should result in a node of type 'GenericNode'
					return (AbstractNode) createNodeCommand.execute(props);
				}

			});

			assertTrue(node != null);
			assertTrue(node instanceof GenericNode);

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test02CreateRelationship() {

		try {

			List<AbstractNode> nodes       = createTestNodes("UnknownTestType", 2);
			final AbstractNode startNode   = nodes.get(0);
			final AbstractNode endNode     = nodes.get(1);
			final RelationshipType relType = RelType.CONTAINS;

			assertTrue(startNode != null);
			assertTrue(endNode != null);

			AbstractRelationship rel = (AbstractRelationship) transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					return (AbstractRelationship) createRelationshipCommand.execute(startNode, endNode, relType);

				}

			});

			assertTrue(rel.getStartNodeId().equals(startNode.getUuid()));
			assertTrue(rel.getEndNodeId().equals(endNode.getUuid()));
			assertTrue(rel.getType().equals(relType.name()));

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	/**
	 * Create a node for each configured entity class and check the type
	 */
	public void test03CheckNodeEntities() {

		final Map<String, Object> props = new HashMap<String, Object>();

		try {

			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					List<Class> entityList = null;

					try {

						entityList = getClasses("org.structr.core.entity");

					} catch (ClassNotFoundException ex) {

						logger.log(Level.SEVERE, null, ex);

					} catch (IOException ex) {

						logger.log(Level.SEVERE, null, ex);

					}

					assertTrue(entityList.contains(AbstractNode.class));
					assertTrue(entityList.contains(Cache.class));
					assertTrue(entityList.contains(Category.class));
					assertTrue(entityList.contains(File.class));
					assertTrue(entityList.contains(GenericNode.class));
					assertTrue(entityList.contains(Image.class));
					assertTrue(entityList.contains(Location.class));
					assertTrue(entityList.contains(NodeList.class));
					assertTrue(entityList.contains(Folder.class));
					assertTrue(entityList.contains(PlainText.class));
					assertTrue(entityList.contains(PrincipalImpl.class));
					assertTrue(entityList.contains(Person.class));
					assertTrue(entityList.contains(ResourceAccess.class));
					
					// Don't test this one, it would fail due to violated constraints
					entityList.remove(TestTwo.class);

					for (Class entityClass : entityList) {

						// for (Entry<String, Class> entity : entities.entrySet()) {
						// Class entityClass = entity.getValue();
						if (AbstractNode.class.isAssignableFrom(entityClass)) {

							String type = entityClass.getSimpleName();

							// For ResourceAccess, fill mandatory fields
							if (type.equals(ResourceAccess.class.getSimpleName())) {

								props.put(ResourceAccess.Key.signature.name(), "/");
								props.put(ResourceAccess.Key.flags.name(), 6);

							}

							// For Location, set coordinates
							if (type.equals(Location.class.getSimpleName())) {

								props.put(Location.Key.latitude.name(), 12.34);
								props.put(Location.Key.longitude.name(), 56.78);

							}
							
							logger.log(Level.INFO, "Creating node of type {0}", type);
							props.put(AbstractNode.Key.type.name(), type);

							AbstractNode node = (AbstractNode) createNodeCommand.execute(props);

							assertTrue(type.equals(node.getStringProperty(AbstractNode.Key.type)));

							// Remove mandatory fields for ResourceAccess from props map
							if (type.equals(ResourceAccess.class.getSimpleName())) {

								props.remove(ResourceAccess.Key.signature.name());
								props.remove(ResourceAccess.Key.flags.name());

							}

						}
					}

					return null;

				}

			});

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	/**
	 * Create a node for each configured entity class and check the type
	 */
	public void test04CheckRelationshipEntities() {

		try {

			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					List<Class> entityList = null;

					try {

						entityList = getClasses("org.structr.core.entity");

					} catch (ClassNotFoundException ex) {

						Logger.getLogger(CreateGraphObjectsTest.class.getName()).log(Level.SEVERE, null, ex);

					} catch (IOException ex) {

						Logger.getLogger(CreateGraphObjectsTest.class.getName()).log(Level.SEVERE, null, ex);

					}

					assertTrue(entityList.contains(AbstractRelationship.class));
					assertTrue(entityList.contains(GenericRelationship.class));

					for (Class entityClass : entityList) {

						// for (Entry<String, Class> entity : entities.entrySet()) {
						// Class entityClass = entity.getValue();
						if (AbstractRelationship.class.isAssignableFrom(entityClass)) {

							String type = entityClass.getSimpleName();

							logger.log(Level.INFO, "Creating relationship of type {0}", type);

							List<AbstractNode> nodes       = createTestNodes("UnknownTestType", 2);
							final AbstractNode startNode   = nodes.get(0);
							final AbstractNode endNode     = nodes.get(1);
							final RelationshipType relType = RelType.LINK;
							AbstractRelationship rel       = (AbstractRelationship) createRelationshipCommand.execute(startNode, endNode, relType);

							assertTrue(rel != null);
							assertTrue(rel.getType().equals(relType.name()));

						}
					}

					return null;

				}

			});

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	/**
	 * Creation of duplicate relationships is blocked.
	 *
	 * A relationship is considered duplicate if all of the following criteria are met:
	 *
	 * - same start node
	 * - same end node
	 * - same relationship type
	 * - same set of property keys and values
	 *
	 */
	public void test05DuplicateRelationships() {

		try {

			List<AbstractNode> nodes        = createTestNodes("UnknownTestType", 2);
			final AbstractNode startNode    = nodes.get(0);
			final AbstractNode endNode      = nodes.get(1);
			final RelationshipType relType  = RelType.UNDEFINED;
			final Map<String, Object> props = new HashMap<String, Object>();

			props.put("foo", "bar");
			props.put("bar", 123);
			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					AbstractRelationship rel1 = (AbstractRelationship) createRelationshipCommand.execute(startNode, endNode, relType, props, true);

					assertTrue(rel1 != null);

					AbstractRelationship rel2 = (AbstractRelationship) createRelationshipCommand.execute(startNode, endNode, relType, props, true);

					assertTrue(rel2 == null);

					return null;

				}

			});

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

}
