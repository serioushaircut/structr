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



package org.structr.core.entity;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.time.DateUtils;

import org.neo4j.graphdb.*;

import org.structr.common.*;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.UuidCreationTransformation;
import org.structr.common.error.*;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.IdNotFoundToken;
import org.structr.common.error.NullPropertyToken;
import org.structr.common.error.ReadOnlyPropertyToken;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.PropertyGroup;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.node.*;
import org.structr.core.node.NodeService.RelationshipIndex;
import org.structr.core.notion.Notion;
import org.structr.core.notion.RelationshipNotion;
import org.structr.core.validator.SimpleRegexValidator;

//~--- JDK imports ------------------------------------------------------------

import java.text.ParseException;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.GraphObjectComparator.SortOrder;
import org.structr.core.converter.DateConverter;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public abstract class AbstractRelationship implements GraphObject, Comparable<AbstractRelationship> {

	private static final Logger logger = Logger.getLogger(AbstractRelationship.class.getName());

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(AbstractRelationship.class, PropertyView.All, Key.values());
		EntityContext.registerSearchableProperty(AbstractRelationship.class, RelationshipIndex.rel_uuid, Key.uuid);

		// register transformation for automatic uuid creation
		EntityContext.registerEntityCreationTransformation(AbstractRelationship.class, new UuidCreationTransformation());

		// register uuid validator
		EntityContext.registerPropertyValidator(AbstractRelationship.class, Key.uuid, new SimpleRegexValidator("[a-zA-Z0-9]{32}"));

		EntityContext.registerPropertyConverter(AbstractRelationship.class, HiddenKey.createdDate, DateConverter.class);
		
	}

	//~--- fields ---------------------------------------------------------

	private String cachedEndNodeId = null;

	// test
	protected Map<PropertyKey, Object> cachedConvertedProperties = new LinkedHashMap<PropertyKey, Object>();
	protected Map<PropertyKey, Object> cachedRawProperties       = new LinkedHashMap<PropertyKey, Object>();
	private String cachedStartNodeId                        = null;
	protected SecurityContext securityContext               = null;
	private boolean readOnlyPropertiesUnlocked              = false;

	// reference to database relationship
	protected Relationship dbRelationship;
	protected Map<PropertyKey, Object> properties;
	protected String cachedUuid = null;
	protected boolean isDirty;

	//~--- constant enums -------------------------------------------------

	public enum HiddenKey implements PropertyKey{ combinedType,    // internal combinedType, see IndexRelationshipCommand#indexRelationship method
					 cascadeDelete, createdDate, allowed }

	public enum Key implements PropertyKey{ uuid, id }

	//~--- constructors ---------------------------------------------------

//      public enum Permission implements PropertyKey {
//              allowed, denied, read, showTree, write, execute, createNode, deleteNode, editProperties, addRelationship, removeRelationship, accessControl;
//      }
	public AbstractRelationship() {

		this.properties = new HashMap<PropertyKey, Object>();
		isDirty         = true;

	}

	public AbstractRelationship(final Map<PropertyKey, Object> properties) {

		this.properties = properties;
		isDirty         = true;

	}

	public AbstractRelationship(final SecurityContext securityContext, final Map<PropertyKey, Object> data) {

		if (data != null) {

			this.securityContext = securityContext;
			this.properties      = data;
			this.isDirty         = true;

		}

	}

	public AbstractRelationship(final SecurityContext securityContext, final Relationship dbRel) {

		init(securityContext, dbRel);

	}

	//~--- methods --------------------------------------------------------

	/**
	 * Called when a relationship of this combinedType is instatiated. Please note that
	 * a relationship can (and will) be instantiated several times during a
	 * normal rendering turn.
	 */
	public void onRelationshipInstantiation() {

		try {

			if (dbRelationship != null) {

				Node startNode = dbRelationship.getStartNode();
				Node endNode   = dbRelationship.getEndNode();

				if ((startNode != null) && (endNode != null) && startNode.hasProperty(AbstractNode.Key.uuid.name()) && endNode.hasProperty(AbstractNode.Key.uuid.name())) {

					cachedStartNodeId = (String) startNode.getProperty(AbstractNode.Key.uuid.name());
					cachedEndNodeId   = (String) endNode.getProperty(AbstractNode.Key.uuid.name());

				}

			}

		} catch (Throwable t) {}

	}

	public AbstractNode identifyStartNode(RelationshipMapping namedRelation, Map<String, Object> propertySet) throws FrameworkException {

		Notion startNodeNotion = getStartNodeNotion();    // new RelationshipNotion(getStartNodeIdKey());

		startNodeNotion.setType(namedRelation.getSourceType());

		PropertyKey startNodeIdentifier = startNodeNotion.getPrimaryPropertyKey();

		if (startNodeIdentifier != null) {

			Object identifierValue = propertySet.get(startNodeIdentifier.name());

			propertySet.remove(startNodeIdentifier.name());

			return (AbstractNode) startNodeNotion.getAdapterForSetter(securityContext).adapt(identifierValue);

		}

		return null;

	}

	public AbstractNode identifyEndNode(RelationshipMapping namedRelation, Map<String, Object> propertySet) throws FrameworkException {

		Notion endNodeNotion = getEndNodeNotion();    // new RelationshipNotion(getEndNodeIdKey());

		endNodeNotion.setType(namedRelation.getDestType());

		PropertyKey endNodeIdentifier = endNodeNotion.getPrimaryPropertyKey();

		if (endNodeIdentifier != null) {

			Object identifierValue = propertySet.get(endNodeIdentifier.name());

			propertySet.remove(endNodeIdentifier.name());

			return (AbstractNode) endNodeNotion.getAdapterForSetter(securityContext).adapt(identifierValue);

		}

		return null;

	}

	/**
	 * Commit unsaved property values to the relationship node.
	 */
	public void commit() throws FrameworkException {

		isDirty = false;

		// Create an outer transaction to combine any inner neo4j transactions
		// to one single transaction
		Command transactionCommand = Services.command(securityContext, TransactionCommand.class);

		transactionCommand.execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				Command createRel        = Services.command(securityContext, CreateRelationshipCommand.class);
				AbstractRelationship rel = (AbstractRelationship) createRel.execute();

				init(securityContext, rel.getRelationship());

				Set<PropertyKey> keys = properties.keySet();

				for (PropertyKey key : keys) {

					Object value = properties.get(key);

					if ((key != null) && (value != null)) {

						setProperty(key, value);
					}

				}

				return null;

			}

		});

	}

	public void init(final SecurityContext securityContext, final Relationship dbRel) {

		this.dbRelationship  = dbRel;
		this.isDirty         = false;
		this.securityContext = securityContext;
	}

	public void init(final SecurityContext securityContext) {

		this.securityContext = securityContext;
		this.isDirty         = false;

	}

	public void init(final SecurityContext securityContext, final AbstractRelationship rel) {

		this.dbRelationship  = rel.dbRelationship;
		this.isDirty         = false;
		this.securityContext = securityContext;
	}

	@Override
	public void unlockReadOnlyPropertiesOnce() {

		this.readOnlyPropertiesUnlocked = true;

	}

	@Override
	public void removeProperty(final PropertyKey key) throws FrameworkException {

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				try {
					
					dbRelationship.removeProperty(key.name());

				} finally {}

				return null;

			}

		});
	}

	@Override
	public boolean equals(final Object o) {

		return (o != null && new Integer(this.hashCode()).equals(new Integer(o.hashCode())));

	}

	@Override
	public int hashCode() {

		if (this.dbRelationship == null) {

			return (super.hashCode());
		}

		return Long.valueOf(dbRelationship.getId()).hashCode();

	}

	@Override
	public int compareTo(final AbstractRelationship rel) {

		// TODO: implement finer compare methods, e.g. taking title and position into account
		if (rel == null) {

			return -1;
		}

		return ((Long) this.getId()).compareTo((Long) rel.getId());
	}

	public int cascadeDelete() {

		Integer cd = getIntProperty(AbstractRelationship.HiddenKey.cascadeDelete);

		return (cd != null)
		       ? cd
		       : 0;

	}

	public void addPermission(final Permission permission) {

		String[] allowed = getPermissions();

		if (ArrayUtils.contains(allowed, permission.name())) {

			return;
		}

		setAllowed((String[]) ArrayUtils.add(allowed, permission.name()));

	}

	public void removePermission(final Permission permission) {

		String[] allowed = getPermissions();

		if (!ArrayUtils.contains(allowed, permission.name())) {

			return;
		}

		setAllowed((String[]) ArrayUtils.removeElement(allowed, permission.name()));

	}
	//~--- get methods ----------------------------------------------------

	@Override
	public PropertyKey getDefaultSortKey() {

		return null;

	}

	@Override
	public SortOrder getDefaultSortOrder() {

		return GraphObjectComparator.SortOrder.ASCENDING;

	}

	public abstract PropertyKey getStartNodeIdKey();

	public abstract PropertyKey getEndNodeIdKey();

	public Notion getEndNodeNotion() {

		return new RelationshipNotion(getEndNodeIdKey());

	}

	public Notion getStartNodeNotion() {

		return new RelationshipNotion(getStartNodeIdKey());

	}

	@Override
	public long getId() {

		return getInternalId();

	}
	
	@Override
	public String getUuid() {

		return getStringProperty(Key.uuid);

	}
	
	public long getRelationshipId() {

		return getInternalId();

	}

	public long getInternalId() {

		return dbRelationship.getId();

	}

	public Map<PropertyKey, Object> getProperties() {

		Map<PropertyKey, Object> properties = new HashMap<PropertyKey, Object>();

		for (String key : dbRelationship.getPropertyKeys()) {

			properties.put(Key.valueOf(key), dbRelationship.getProperty(key));
		}

		return properties;

	}

	@Override
	public Object getProperty(final PropertyKey key) {
		return getProperty(key, true);
	}

	private Object getProperty(final PropertyKey key, boolean applyConverter) {

		Object value      = applyConverter ? cachedConvertedProperties.get(key) : cachedRawProperties.get(key);
		boolean dontCache = false;
		Class type         = this.getClass();
		
		if(value == null || !applyConverter) {

			PropertyKey startNodeIdKey = getStartNodeIdKey();
			PropertyKey endNodeIdKey   = getEndNodeIdKey();

			if (startNodeIdKey != null && key.equals(startNodeIdKey.name())) {

				value = getStartNodeId();
				
				if(applyConverter) {
					
					cachedConvertedProperties.put(key, value);
					
				} else {
					
					cachedRawProperties.put(key, value);
				}
				
				return value;

			}
			
			if (endNodeIdKey != null && key.equals(endNodeIdKey.name())) {

				value = getEndNodeId();
				
				if(applyConverter) {
					
					cachedConvertedProperties.put(key, value);
					
				} else {
					
					cachedRawProperties.put(key, value);
				}

				return value;
				
			}
			
			// ----- BEGIN property group resolution -----
			PropertyGroup propertyGroup = EntityContext.getPropertyGroup(type, key);

			if (propertyGroup != null) {

				return propertyGroup.getGroupedProperties(this);
			}

			if (dbRelationship.hasProperty(key.name())) {

				value = dbRelationship.getProperty(key.name());
			}

			// no value found, use schema default
			if (value == null) {

				value = EntityContext.getDefaultValue(type, key);
				dontCache = true;
			}

			// only apply converter if requested
			// (required for getComparableProperty())
			if(applyConverter) {

				// apply property converters
				PropertyConverter converter = EntityContext.getPropertyConverter(securityContext, type, key);

				if (converter != null) {

					Value conversionValue = EntityContext.getPropertyConversionParameter(type, key);

					converter.setCurrentObject(this);

					value = converter.convertForGetter(value, conversionValue);

				}
			}

			if(!dontCache) {

				// only cache value if it is NOT the schema default
				if(applyConverter) {
					
					cachedConvertedProperties.put(key, value);
					
				} else {
					
					cachedRawProperties.put(key, value);
				}
			}
		}

		return value;

	}

	@Override
	public String getStringProperty(final PropertyKey key) {

		Object propertyValue = getProperty(key);
		String result        = null;

		if (propertyValue == null) {

			return null;
		}

		if (propertyValue instanceof String) {

			result = ((String) propertyValue);
		}

		return result;

	}

	@Override
	public Integer getIntProperty(final PropertyKey key) {

		Object propertyValue = getProperty(key);
		Integer result       = null;

		if (propertyValue == null) {

			return null;
		}

		if (propertyValue instanceof Integer) {

			result = ((Integer) propertyValue);
		} else if (propertyValue instanceof String) {

			if ("".equals((String) propertyValue)) {

				return null;
			}

			result = Integer.parseInt(((String) propertyValue));

		}

		return result;

	}

	@Override
	public Long getLongProperty(final PropertyKey key) {

		Object propertyValue = getProperty(key);
		Long result          = null;

		if (propertyValue == null) {

			return null;
		}

		if (propertyValue instanceof Long) {

			result = ((Long) propertyValue);
		} else if (propertyValue instanceof Integer) {

			result = ((Integer) propertyValue).longValue();
		} else if (propertyValue instanceof String) {

			if ("".equals((String) propertyValue)) {

				return null;
			}

			result = Long.parseLong(((String) propertyValue));

		}

		return result;

	}

	@Override
	public Date getDateProperty(final PropertyKey key) {

		Object propertyValue = getProperty(key);

		if (propertyValue != null) {

			if (propertyValue instanceof Date) {

				return (Date) propertyValue;
			} else if (propertyValue instanceof Long) {

				return new Date((Long) propertyValue);
			} else if (propertyValue instanceof String) {

				try {

					// try to parse as a number
					return new Date(Long.parseLong((String) propertyValue));
				} catch (NumberFormatException nfe) {

					try {

						Date date = DateUtils.parseDate(((String) propertyValue), new String[] { "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss", "yyyymmdd", "yyyymm",
							"yyyy" });

						return date;

					} catch (ParseException ex2) {

						logger.log(Level.WARNING, "Could not parse " + propertyValue + " to date", ex2);

					}

					logger.log(Level.WARNING, "Can''t parse String {0} to a Date.", propertyValue);

					return null;

				}

			} else {

				logger.log(Level.WARNING, "Date property is not null, but type is neither Long nor String, returning null");

				return null;

			}

		}

		return null;

	}

	@Override
	public boolean getBooleanProperty(final PropertyKey key) {

		Object propertyValue = getProperty(key);
		Boolean result       = false;

		if (propertyValue == null) {

			return Boolean.FALSE;
		}

		if (propertyValue instanceof Boolean) {

			result = ((Boolean) propertyValue);
		} else if (propertyValue instanceof String) {

			result = Boolean.parseBoolean(((String) propertyValue));
		}

		return result;

	}

	@Override
	public Double getDoubleProperty(final PropertyKey key) throws FrameworkException {

		Object propertyValue = getProperty(key);
		Double result        = null;

		if (propertyValue == null) {

			return null;
		}

		if (propertyValue instanceof Double) {

			Double doubleValue = (Double) propertyValue;

			if (doubleValue.equals(Double.NaN)) {

				// clean NaN values from database
				setProperty(key, null);

				return null;
			}

			result = doubleValue;

		} else if (propertyValue instanceof String) {

			if ("".equals((String) propertyValue)) {

				return null;
			}

			result = Double.parseDouble(((String) propertyValue));

		}

		return result;

	}

	@Override
	public Comparable getComparableProperty(final PropertyKey key) {

		Object propertyValue = getProperty(key, false);	// get "raw" property without converter
		Class type = getClass();
		
		// check property converter
		PropertyConverter converter = EntityContext.getPropertyConverter(securityContext, type, key);
		if (converter != null) {
			
			Value conversionValue = EntityContext.getPropertyConversionParameter(type, key);
			converter.setCurrentObject(this);

			return converter.convertForSorting(propertyValue, conversionValue);
		}
		
		// conversion failed, may the property value itself is comparable
		if(propertyValue instanceof Comparable) {
			return (Comparable)propertyValue;
		}
		
		// last try: convert to String to make comparable
		if(propertyValue != null) {
			return propertyValue.toString();
		}
		
		return null;
	}

	/**
	 * Return database relationship
	 *
	 * @return
	 */
	public Relationship getRelationship() {

		return dbRelationship;

	}

	public AbstractNode getEndNode() {

		try {

			Command nodeFactory = Services.command(SecurityContext.getSuperUserInstance(), NodeFactoryCommand.class);

			return (AbstractNode) nodeFactory.execute(dbRelationship.getEndNode());

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to instantiate node", fex);

		}

		return null;

	}

	public AbstractNode getStartNode() {

		try {

			Command nodeFactory = Services.command(SecurityContext.getSuperUserInstance(), NodeFactoryCommand.class);

			return (AbstractNode) nodeFactory.execute(dbRelationship.getStartNode());

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to instantiate node", fex);

		}

		return null;

	}

	public AbstractNode getOtherNode(final AbstractNode node) {

		try {

			Command nodeFactory = Services.command(SecurityContext.getSuperUserInstance(), NodeFactoryCommand.class);

			return (AbstractNode) nodeFactory.execute(dbRelationship.getOtherNode(node.getNode()));

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to instantiate node", fex);

		}

		return null;

	}

	public RelationshipType getRelType() {

		return dbRelationship.getType();

	}

	public String[] getPermissions() {

		if (dbRelationship.hasProperty(HiddenKey.allowed.name())) {

			// StringBuilder result             = new StringBuilder();
			String[] allowedProperties = (String[]) dbRelationship.getProperty(HiddenKey.allowed.name());

			return allowedProperties;

//                      if (allowedProperties != null) {
//
//                              for (String p : allowedProperties) {
//
//                                      result.append(p).append("\n");
//
//                              }
//
//                      }
//
//                      return result.toString();
		} else {

			return null;
		}

	}

	/**
	 * Return all property keys.
	 *
	 * @return
	 */
	public Iterable<PropertyKey> getPropertyKeys() {

		return getPropertyKeys(PropertyView.All);

	}

	/**
	 * Return property value which is used for indexing.
	 *
	 * This is useful f.e. to filter markup from HTML to index only text
	 *
	 * @param key
	 * @return
	 */
	@Override
	public Object getPropertyForIndexing(final PropertyKey key) {

		return getProperty(key);

	}

	// ----- interface GraphObject -----
	@Override
	public Iterable<PropertyKey> getPropertyKeys(final String propertyView) {

		return EntityContext.getPropertySet(this.getClass(), propertyView);

	}

	public Map<RelationshipType, Long> getRelationshipInfo(Direction direction) {

		return null;

	}

	public List<AbstractRelationship> getRelationships(RelationshipType type, Direction dir) {

		return null;

	}

	@Override
	public String getType() {

		return getRelType().name();

	}

	public String getStartNodeId() {

		return getStartNode().getUuid();

	}

	public String getEndNodeId() {

		return getEndNode().getUuid();

	}

	public String getOtherNodeId(final AbstractNode node) {

		return getOtherNode(node).getStringProperty(AbstractRelationship.Key.uuid);

	}

	private AbstractNode getNodeByUuid(final String uuid) throws FrameworkException {

		return (AbstractNode) Services.command(securityContext, GetNodeByIdCommand.class).execute(uuid);

	}

	public String getCachedStartNodeId() {

		return cachedStartNodeId;

	}

	public String getCachedEndNodeId() {

		return cachedEndNodeId;

	}

	@Override
	public boolean beforeCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return isValid(errorBuffer);
	}

	@Override
	public boolean beforeModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return isValid(errorBuffer);
	}

	@Override
	public boolean beforeDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, Map<String, Object> properties) throws FrameworkException {
		
		cachedUuid = (String)properties.get(AbstractRelationship.Key.uuid.name());
		
		return true;
	}
	
	@Override
	public void afterCreation(SecurityContext securityContext) {
	}

	@Override
	public void afterModification(SecurityContext securityContext) {
	}

	@Override
	public void afterDeletion(SecurityContext securityContext) {
	}

	@Override
	public void ownerModified(SecurityContext securityContext) {
	}
	
	@Override
	public void securityModified(SecurityContext securityContext) {
	}
	
	@Override
	public void locationModified(SecurityContext securityContext) {
	}
	
	private boolean isValid(ErrorBuffer errorBuffer) {

		boolean error = false;

		error |= ValidationHelper.checkStringNotBlank(this, AbstractRelationship.Key.uuid, errorBuffer);

		return !error;

	}

	public boolean isType(RelType type) {

		return ((type != null) && type.equals(dbRelationship.getType()));

	}

	public boolean isAllowed(final Permission permission) {

		if (dbRelationship.hasProperty(HiddenKey.allowed.name())) {

			String[] allowedProperties = (String[]) dbRelationship.getProperty(HiddenKey.allowed.name());

			if (allowedProperties != null) {

				for (String p : allowedProperties) {

					if (p.equals(permission.name())) {

						return true;
					}

				}

			}

		}

		return false;

	}

	//~--- set methods ----------------------------------------------------

	public void setProperties(final Map<PropertyKey, Object> properties) throws FrameworkException {

		for (Entry prop : properties.entrySet()) {

			setProperty((PropertyKey) prop.getKey(), prop.getValue());
		}

	}

	@Override
	public void setProperty(final PropertyKey key, final Object value) throws FrameworkException {

		PropertyKey startNodeIdKey = getStartNodeIdKey();
		PropertyKey endNodeIdKey   = getEndNodeIdKey();

		// clear cached property
		cachedConvertedProperties.remove(key);
		cachedRawProperties.remove(key);
		
		if ((startNodeIdKey != null) && key.equals(startNodeIdKey.name())) {

			setStartNodeId((String) value);

			return;

		}

		if ((endNodeIdKey != null) && key.equals(endNodeIdKey.name())) {

			setEndNodeId((String) value);

			return;

		}

		Class type = this.getClass();

		// check for read-only properties
		if (EntityContext.isReadOnlyProperty(type, key) || (EntityContext.isWriteOnceProperty(type, key) && (dbRelationship != null) && dbRelationship.hasProperty(key.name()))) {

			if (readOnlyPropertiesUnlocked) {

				// permit write operation once and
				// lock read-only properties again
				readOnlyPropertiesUnlocked = false;
			} else {

				throw new FrameworkException(type.getSimpleName(), new ReadOnlyPropertyToken(key));
			}

		}

		// ----- BEGIN property group resolution -----
		PropertyGroup propertyGroup = EntityContext.getPropertyGroup(type, key);

		if (propertyGroup != null) {

			propertyGroup.setGroupedProperties(value, this);

			return;

		}

		PropertyConverter converter = EntityContext.getPropertyConverter(securityContext, type, key);
		final Object convertedValue;

		if (converter != null) {

			Value conversionValue = EntityContext.getPropertyConversionParameter(type, key);

			converter.setCurrentObject(this);

			convertedValue = converter.convertForSetter(value, conversionValue);

		} else {

			convertedValue = value;
		}

		final Object oldValue = getProperty(key);
		
		// don't make any changes if
		// - old and new value both are null
		// - old and new value are not null but equal
		if (((convertedValue == null) && (oldValue == null)) || ((convertedValue != null) && (oldValue != null) && convertedValue.equals(oldValue))) {

			return;
		}

		// Commit value directly to database
		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				try {

					// save space
					if (convertedValue == null) {

						dbRelationship.removeProperty(key.name());
					} else {

						if (convertedValue instanceof Date) {

							dbRelationship.setProperty(key.name(), ((Date) convertedValue).getTime());
						} else {

							dbRelationship.setProperty(key.name(), convertedValue);
						}

					}
				} finally {}

				return null;

			}

		};

		// execute transaction
		Services.command(securityContext, TransactionCommand.class).execute(transaction);

		// clear cached property
		cachedConvertedProperties.remove(key);
		cachedRawProperties.remove(key);
	}

	/**
	 * Set node id of start node.
	 *
	 * Internally, this method deletes the old relationship
	 * and creates a new one, ends at the same end node,
	 * but starting from the node with startNodeId
	 *
	 */
	public void setStartNodeId(final String startNodeId) throws FrameworkException {

		final String type = this.getClass().getSimpleName();
		final PropertyKey key  = getStartNodeIdKey();

		// May never be null!!
		if (startNodeId == null) {

			throw new FrameworkException(type, new NullPropertyToken(key));
		}
		
		// Do nothing if new id equals old
		if (getStartNodeId().equals(startNodeId)) {
			return;
		}

		Command transaction = Services.command(securityContext, TransactionCommand.class);

		transaction.execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				Command deleteRel         = Services.command(securityContext, DeleteRelationshipCommand.class);
				Command createRel         = Services.command(securityContext, CreateRelationshipCommand.class);
				Command nodeFactory       = Services.command(securityContext, NodeFactoryCommand.class);
				AbstractNode newStartNode = getNodeByUuid(startNodeId);
				AbstractNode endNode      = (AbstractNode) nodeFactory.execute(getEndNode());

				if (newStartNode == null) {

					throw new FrameworkException(type, new IdNotFoundToken(startNodeId));
				}

				RelationshipType type = dbRelationship.getType();

				properties = getProperties();

				deleteRel.execute(dbRelationship);

				AbstractRelationship newRel = (AbstractRelationship) createRel.execute(newStartNode, endNode, type, properties, false);

				dbRelationship = newRel.getRelationship();

				return (null);

			}

		});

	}

	/**
	 * Set node id of end node.
	 *
	 * Internally, this method deletes the old relationship
	 * and creates a new one, start from the same start node,
	 * but pointing to the node with endNodeId
	 *
	 */
	public void setEndNodeId(final String endNodeId) throws FrameworkException {

		final String type = this.getClass().getSimpleName();
		final PropertyKey key  = getStartNodeIdKey();

		// May never be null!!
		if (endNodeId == null) {

			throw new FrameworkException(type, new NullPropertyToken(key));
		}
		
		// Do nothing if new id equals old
		if (getEndNodeId().equals(endNodeId)) {
			return;
		}

		Command transaction = Services.command(securityContext, TransactionCommand.class);

		transaction.execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				Command deleteRel       = Services.command(securityContext, DeleteRelationshipCommand.class);
				Command createRel       = Services.command(securityContext, CreateRelationshipCommand.class);
				Command nodeFactory     = Services.command(securityContext, NodeFactoryCommand.class);
				AbstractNode startNode  = (AbstractNode) nodeFactory.execute(getStartNode());
				AbstractNode newEndNode = getNodeByUuid(endNodeId);

				if (newEndNode == null) {

					throw new FrameworkException(type, new IdNotFoundToken(endNodeId));
				}

				RelationshipType type = dbRelationship.getType();

				properties = getProperties();

				deleteRel.execute(dbRelationship);

				AbstractRelationship newRel = (AbstractRelationship) createRel.execute(startNode, newEndNode, type, properties, false);

				dbRelationship = newRel.getRelationship();

				return (null);

			}

		});

	}

	/**
	 * Set relationship combinedType
	 *
	 * Internally, this method deletes the old relationship
	 * and creates a new one, with the same start and end node,
	 * but with another combinedType
	 *
	 */
	public void setType(final String type) {

		if (type != null) {

			try {

				Command transacted = Services.command(securityContext, TransactionCommand.class);

				transacted.execute(new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						Command deleteRel      = Services.command(securityContext, DeleteRelationshipCommand.class);
						Command createRel      = Services.command(securityContext, CreateRelationshipCommand.class);
						Command nodeFactory    = Services.command(securityContext, NodeFactoryCommand.class);
						AbstractNode startNode = (AbstractNode) nodeFactory.execute(getStartNode());
						AbstractNode endNode   = (AbstractNode) nodeFactory.execute(getEndNode());

						deleteRel.execute(dbRelationship);

						dbRelationship = ((AbstractRelationship) createRel.execute(startNode, endNode, type)).getRelationship();

						return (null);

					}

				});

			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Unable to set relationship type", fex);

			}

		}

	}

	public void setAllowed(final List<String> allowed) {

		String[] allowedActions = (String[]) allowed.toArray(new String[allowed.size()]);

		setAllowed(allowedActions);

	}

	public void setAllowed(final Permission[] allowed) {

		List<String> allowedActions = new ArrayList<String>();

		for (Permission permission : allowed) {

			allowedActions.add(permission.name());
		}

		setAllowed(allowedActions);

	}

	public void setAllowed(final String[] allowed) {

		dbRelationship.setProperty(AbstractRelationship.HiddenKey.allowed.name(), allowed);

	}

}
