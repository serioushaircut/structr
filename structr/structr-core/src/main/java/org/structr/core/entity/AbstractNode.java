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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

import org.structr.common.*;
import org.structr.common.AccessControllable;
import org.structr.common.GraphObjectComparator;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.UuidCreationTransformation;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.NullArgumentToken;
import org.structr.common.error.ReadOnlyPropertyToken;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.IterableAdapter;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.PropertyGroup;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.converter.BooleanConverter;
import org.structr.core.node.*;
import org.structr.core.node.NodeRelationshipStatisticsCommand;
import org.structr.core.node.NodeRelationshipsCommand;
import org.structr.core.node.NodeService.NodeIndex;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.notion.Notion;
import org.structr.core.validator.SimpleRegexValidator;

//~--- JDK imports ------------------------------------------------------------

import java.text.ParseException;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.GraphObjectComparator.SortOrder;
import org.structr.core.converter.DateConverter;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.notion.PropertyNotion;

//~--- classes ----------------------------------------------------------------

/**
 * The base class for all node types in structr.
 *
 * @author Axel Morgner
 * @author Christian Morgner
 */
public abstract class AbstractNode implements GraphObject, Comparable<AbstractNode>, AccessControllable {

	private static final Logger logger              = Logger.getLogger(AbstractNode.class.getName());
	private static final boolean updateIndexDefault = true;

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(AbstractNode.class, PropertyView.All, Key.values());
		EntityContext.registerPropertySet(AbstractNode.class, PropertyView.Ui, Key.values());
		EntityContext.registerPropertyConverter(AbstractNode.class, Key.visibilityStartDate, DateConverter.class);
		EntityContext.registerPropertyConverter(AbstractNode.class, Key.visibilityEndDate, DateConverter.class);
		EntityContext.registerPropertyConverter(AbstractNode.class, Key.lastModifiedDate, DateConverter.class);
		EntityContext.registerPropertyConverter(AbstractNode.class, Key.createdDate, DateConverter.class);
		EntityContext.registerPropertyConverter(AbstractNode.class, Key.deleted, BooleanConverter.class);
		EntityContext.registerPropertyConverter(AbstractNode.class, Key.hidden, BooleanConverter.class);
		EntityContext.registerPropertyConverter(AbstractNode.class, Key.visibleToPublicUsers, BooleanConverter.class);
		EntityContext.registerPropertyConverter(AbstractNode.class, Key.visibleToAuthenticatedUsers, BooleanConverter.class);
		EntityContext.registerSearchablePropertySet(AbstractNode.class, NodeIndex.fulltext, Key.values());
		EntityContext.registerSearchablePropertySet(AbstractNode.class, NodeIndex.keyword, Key.values());
		EntityContext.registerSearchableProperty(AbstractNode.class, NodeIndex.uuid, Key.uuid);

		EntityContext.registerPropertyRelation(AbstractNode.class, Key.ownerId, Principal.class, RelType.OWNS, Direction.INCOMING, Cardinality.ManyToOne, new PropertyNotion(AbstractNode.Key.uuid));

		
		
		// register transformation for automatic uuid creation
		EntityContext.registerEntityCreationTransformation(AbstractNode.class, new UuidCreationTransformation());

		// register uuid validator
		EntityContext.registerPropertyValidator(AbstractNode.class, AbstractNode.Key.uuid, new SimpleRegexValidator("[a-zA-Z0-9]{32}"));

	}

	//~--- fields ---------------------------------------------------------

	protected Map<PropertyKey, Object> cachedConvertedProperties  = new LinkedHashMap<PropertyKey, Object>();
	protected Map<PropertyKey, Object> cachedRawProperties        = new LinkedHashMap<PropertyKey, Object>();
	protected Principal cachedOwnerNode                      = null;

	// request parameters
	protected SecurityContext securityContext                     = null;
	private Map<Long, AbstractRelationship> securityRelationships = null;
	private boolean readOnlyPropertiesUnlocked                    = false;

	// reference to database node
	protected Node dbNode;

	// dirty flag, true means that some changes are not yet written to the database
	protected Map<PropertyKey, Object> properties;
	protected String cachedUuid = null;
	protected boolean isDirty;

//      protected Principal user;

	//~--- constant enums -------------------------------------------------

	public static enum Key implements PropertyKey {

		id, uuid, name, type, createdBy, createdDate, deleted, hidden, lastModifiedDate, visibleToPublicUsers, visibilityEndDate, visibilityStartDate,
		visibleToAuthenticatedUsers, categories, ownerId, unknown_type

	}

	//~--- constructors ---------------------------------------------------

	public AbstractNode() {

		this.properties = new HashMap<PropertyKey, Object>();
		isDirty         = true;

	}

	public AbstractNode(final Map<PropertyKey, Object> properties) {

		this.properties = properties;
		isDirty         = true;

	}

	public AbstractNode(SecurityContext securityContext, final Node dbNode) {

		init(securityContext, dbNode);

	}

	//~--- methods --------------------------------------------------------
	
	public void onNodeCreation() {
	}

	public void onNodeInstantiation() {
	}
	
	public void onNodeDeletion() {
	}
	
	public final void init(final SecurityContext securityContext, final Node dbNode) {

		this.dbNode          = dbNode;
		this.isDirty         = false;
		this.securityContext = securityContext;
	}

	private void init(final SecurityContext securityContext, final AbstractNode node) {

		init(securityContext, node.dbNode);

	}

	@Override
	public boolean equals(final Object o) {

		if (o == null) {

			return false;
		}

		if (!(o instanceof AbstractNode)) {

			return false;
		}

		return (new Integer(this.hashCode()).equals(new Integer(o.hashCode())));

	}

	@Override
	public int hashCode() {

		if (this.dbNode == null) {

			return (super.hashCode());
		}

		return Long.valueOf(dbNode.getId()).hashCode();

	}

	@Override
	public int compareTo(final AbstractNode node) {

		if(node == null) {
			return -1;
		}
		
		
		String name = getName();
		
		if(name == null) {
			return -1;
		}
		
		
		String nodeName = node.getName();
		
		if(nodeName == null) {
			return -1;
		}
		
		return name.compareTo(nodeName);
	}

	/**
	 * Implement standard toString() method
	 */
	@Override
	public String toString() {

		if (dbNode == null) {

			return "AbstractNode with null database node";
		}

		try {

			String name = dbNode.hasProperty(Key.name.name())
				      ? (String) dbNode.getProperty(Key.name.name())
				      : "<null name>";
			String type = dbNode.hasProperty(Key.type.name())
				      ? (String) dbNode.getProperty(Key.type.name())
				      : "<AbstractNode>";
			String id   = dbNode.hasProperty(Key.uuid.name())
				      ? (String) dbNode.getProperty(Key.uuid.name())
				      : Long.toString(dbNode.getId());

			return type + " (" + name + "," + type + "," + id + ")";

		} catch (Throwable ignore) {}

		return "<AbstractNode>";

	}

	/**
	 * Populate the security relationship cache map
	 */
	private void populateSecurityRelationshipCacheMap() {

		if (securityRelationships == null) {

			securityRelationships = new HashMap<Long, AbstractRelationship>();
		}

		// Fill cache map
		for (AbstractRelationship r : getRelationships(RelType.SECURITY, Direction.INCOMING)) {
			
			Principal owner = (Principal) r.getStartNode();
			
			if (owner != null) {

				securityRelationships.put(owner.getId(), r);
			}
		}

	}

	/**
	 * Can be used to permit the setting of a read-only
	 * property once. The lock will be restored automatically
	 * after the next setProperty operation. This method exists
	 * to prevent automatic set methods from setting a read-only
	 * property while allowing a manual set method to override this
	 * default behaviour.
	 */
	@Override
	public void unlockReadOnlyPropertiesOnce() {

		this.readOnlyPropertiesUnlocked = true;

	}

	@Override
	public void removeProperty(final PropertyKey key) throws FrameworkException {

		if (this.dbNode != null) {

			if (key == null) {

				logger.log(Level.SEVERE, "Tried to set property with null key (action was denied)");

				return;

			}

			// check for read-only properties
			if (EntityContext.isReadOnlyProperty(this.getClass(), key)) {

				if (readOnlyPropertiesUnlocked) {

					// permit write operation once and
					// lock read-only properties again
					readOnlyPropertiesUnlocked = false;
				} else {

					throw new FrameworkException(this.getType(), new ReadOnlyPropertyToken(key));
				}

			}

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					dbNode.removeProperty(key.name());

					return null;

				}

			});

		}

	}

	/**
	 * Returns the number of elements in the given Iterable
	 *
	 * @param iterable
	 * @return the number of elements in the given iterable
	 */
	protected int countIterableElements(Iterable iterable) {

		int count = 0;

		for (Object o : iterable) {

			count++;
		}

		return (count);

	}

	/**
	 * Checks if the given object is an Iterable and collects the elements
	 * into a set. Returns null otherwise.
	 * 
	 * @param source
	 * @return the elements of the given iterable object in a set, or null
	 */
	protected Set toSet(Object source) {

		if (source instanceof Iterable) {

			Iterable<AbstractNode> iterable = (Iterable<AbstractNode>) source;
			Set<AbstractNode> nodes         = new LinkedHashSet();

			for (AbstractNode node : iterable) {

				nodes.add(node);
			}

			return nodes;

		}

		return null;

	}

	/**
	 * Checks if the given object is an Interable and collects the elements
	 * into a list. Returns null otherwise.
	 * 
	 * @param source
	 * @return the elements of the given iterable object in a list, or null
	 */
	protected List toList(Object source) {

		if (source instanceof Iterable) {

			Iterable<AbstractNode> iterable = (Iterable<AbstractNode>) source;
			List<AbstractNode> nodes        = new LinkedList();

			for (AbstractNode node : iterable) {

				nodes.add(node);
			}

			return nodes;

		}

		return null;

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

	@Override
	public String getType() {

		return (String) getProperty(Key.type);

	}

	/**
	 * Get name from underlying db node
	 *
	 * If name is null, return node id as fallback
	 */
	public String getName() {
		
		String name = getStringProperty(Key.name);
		if (name == null) {
			name = getNodeId().toString();
		}

		return name;
	}

	/**
	 * Get id from underlying db
	 */
	@Override
	public long getId() {

		if (isDirty) {

			return -1;
		}

		return dbNode.getId();

	}

	@Override
	public String getUuid() {

		return getStringProperty(Key.uuid);

	}

	public Long getNodeId() {

		return getId();

	}

	public String getIdString() {

		return Long.toString(getId());

	}

	/**
	 * Returns the property value for the given key as a Date object.
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Date object
	 */
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

	/**
	 * Indicates whether this node is visible to public users.
	 * 
	 * @return whether this node is visible to public users
	 */
	public boolean getVisibleToPublicUsers() {
		return getBooleanProperty(Key.visibleToPublicUsers);
	}

	/**
	 * Indicates whether this node is visible to authenticated users.
	 * 
	 * @return whether this node is visible to authenticated users
	 */
	public boolean getVisibleToAuthenticatedUsers() {
		return getBooleanProperty(Key.visibleToPublicUsers);
	}

	/**
	 * Indicates whether this node is hidden.
	 * 
	 * @return whether this node is hidden
	 */
	public boolean getHidden() {
		return getBooleanProperty(Key.hidden);
	}

	/**
	 * Indicates whether this node is deleted.
	 * 
	 * @return whether this node is deleted
	 */
	public boolean getDeleted() {
		return getBooleanProperty(Key.deleted);
	}

	/**
	 * Returns the property set for the given view as an Iterable.
	 *
	 * @param propertyView
	 * @return the property set for the given view
	 */
	@Override
	public Iterable<PropertyKey> getPropertyKeys(final String propertyView) {

		return EntityContext.getPropertySet(this.getClass(), propertyView);

	}

	/**
	 * Return property value which is used for indexing.
	 *
	 * This is useful f.e. to filter markup from HTML to index only text,
	 * or to get dates as long values.
	 *
	 * @param key
	 * @return
	 */
	@Override
	public Object getPropertyForIndexing(final PropertyKey key) {

		Object rawValue = getProperty(key, false);
		
		if (rawValue != null) {
			
			return rawValue;
			
		}
		
		return getProperty(key);

	}

	/**
	 * Returns the (converted, validated, transformed, etc.) property for the given
	 * property key.
	 * 
	 * @param propertyKey the property key to retrieve the value for
	 * @return the converted, validated, transformed property value
	 */
	@Override
	public Object getProperty(final PropertyKey key) {
		return getProperty(key, true);
	}
	
	private Object getProperty(final PropertyKey key, boolean applyConverter) {

		if (key == null) {

			logger.log(Level.SEVERE, "Invalid property key: null");

			return null;

		}
		
		Object value;
		
		if (isDirty) {

			return properties.get(key);
		}

		if (dbNode == null) {

			return null;
		}

		value             = applyConverter ? cachedConvertedProperties.get(key) : cachedRawProperties.get(key);
		Class type        = this.getClass();
		boolean dontCache = false;

		// only use cached value if property is accessed the "normal" way (i.e. WITH converters)
		if (value == null) {
			
			// ----- BEGIN property group resolution -----
			PropertyGroup propertyGroup = EntityContext.getPropertyGroup(type, key);

			if (propertyGroup != null) {

				return propertyGroup.getGroupedProperties(this);
			}

			if (dbNode.hasProperty(key.name())) {

				if ((key != null) && (dbNode != null)) {

					value = dbNode.getProperty(key.name());
				}

			} else {

				// ----- BEGIN automatic property resolution (check for static relationships and return related nodes) -----
				RelationClass rel = EntityContext.getRelationClass(type, key);

				if (rel != null) {

					// apply notion (default is "as-is")
					Notion notion = rel.getNotion();

					// return collection or single element depending on cardinality of relationship
					switch (rel.getCardinality()) {

						case ManyToMany :
						case OneToMany :
							value     = new IterableAdapter(rel.getRelatedNodes(securityContext, this), notion.getAdapterForGetter(securityContext));
							dontCache = true;
							break;

						case OneToOne :
						case ManyToOne :
							try {

								value = notion.getAdapterForGetter(securityContext).adapt(rel.getRelatedNode(securityContext, this));
								dontCache = true;

							} catch (FrameworkException fex) {

								logger.log(Level.WARNING, "Error while adapting related node", fex);

							}

							break;

					}
				}

				// ----- END automatic property resolution -----
			}

			// no value found, use schema default
			if (value == null) {

				value = EntityContext.getDefaultValue(type, key);
				dontCache = true;
			}

			// only apply converter if requested
			// (required for getComparableProperty())
			if (applyConverter) {

				// apply property converters
				PropertyConverter converter = EntityContext.getPropertyConverter(securityContext, type, key);

				if (converter != null) {

					Value conversionValue = EntityContext.getPropertyConversionParameter(type, key);

					converter.setCurrentObject(this);

					value = converter.convertForGetter(value, conversionValue);

				}
			}

			if (!dontCache) {
				
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

	public String getPropertyMD5(final PropertyKey key) {

		Object value = getProperty(key);

		if (value instanceof String) {

			return DigestUtils.md5Hex((String) value);
		} else if (value instanceof byte[]) {

			return DigestUtils.md5Hex((byte[]) value);
		}

		logger.log(Level.WARNING, "Could not create MD5 hex out of value {0}", value);

		return null;

	}

	/**
	 * Returns the property value for the given key as a String object.
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a String object
	 */
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

	/**
	 * Returns the property value for the given key as a List of Strings,
	 * split on [\r\n].
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a List of Strings
	 */
	public List<String> getStringListProperty(final PropertyKey key) {

		Object propertyValue = getProperty(key);
		List<String> result  = new LinkedList<String>();

		if (propertyValue == null) {

			return null;
		}

		if (propertyValue instanceof String) {

			// Split by carriage return / line feed
			String[] values = StringUtils.split(((String) propertyValue), "\r\n");

			result = Arrays.asList(values);
		} else if (propertyValue instanceof String[]) {

			String[] values = (String[]) propertyValue;

			result = Arrays.asList(values);

		}

		return result;

	}

	/**
	 * Returns the property value for the given key as an Array of Strings.
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as an Array of Strings
	 */
	public String getStringArrayPropertyAsString(final PropertyKey key) {

		Object propertyValue = getProperty(key);
		StringBuilder result = new StringBuilder();

		if (propertyValue instanceof String[]) {

			int i           = 0;
			String[] values = (String[]) propertyValue;

			for (String value : values) {

				result.append(value);

				if (i < values.length - 1) {

					result.append("\r\n");
				}

			}

		}

		return result.toString();

	}

	/**
	 * Returns the property value for the given key as a Boolean object
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Boolean object
	 */
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

	/**
	 * Returns the property value for the given key as an Integer object.
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as an Integer object
	 */
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

	/**
	 * Returns the property value for the given key as a Long object
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Long object
	 */
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

	/**
	 * Returns the property value for the given key as a Double object
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Double object
	 */
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

	/**
	 * Returns the property value for the given key as a Comparable
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Comparable
	 */
	@Override
	public Comparable getComparableProperty(PropertyKey key) {

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
	 * Returns a single related node of the given type, following the relationship(s) defined in
	 * {@see EntityContext}. This method will throw an Exception if the cardinality of the
	 * relationship is not set to OneToMany or ManyToMany.
	 * 
	 * @param type the type of the related node to fetch
	 * @return a single related node of the given type
	 */
	public AbstractNode getRelatedNode(Class type) {

		RelationClass rc = EntityContext.getRelationClass(this.getClass(), type);

		if (rc != null) {

			return rc.getRelatedNode(securityContext, this);
		}

		return null;

	}

	/**
	 * Returns a single related node of the given type, following the relationship(s) defined in
	 * {@see EntityContext}. This method will throw an Exception if the cardinality of the
	 * relationship is not set to OneToOne or ManyToOne.
	 * 
	 * @param type the type of the related node to fetch
	 * @return a single related node of the given type
	 */
	public AbstractNode getRelatedNode(PropertyKey propertyKey) {

		RelationClass rc = EntityContext.getRelationClassForProperty(getClass(), propertyKey);

		if (rc != null) {

			return rc.getRelatedNode(securityContext, this);
		}

		return null;

	}

	/**
	 * Returns a list of related nodes of the given type, following the relationship(s) defined in
	 * {@see EntityContext}. This method will throw an Exception if the cardinality of the
	 * relationship is not set to OneToMany or ManyToMany.
	 * 
	 * @param type the type of the related node to fetch
	 * @return a single related node of the given type
	 */
	public List<AbstractNode> getRelatedNodes(Class type) {

		RelationClass rc = EntityContext.getRelationClass(this.getClass(), type);

		if (rc != null) {

			return rc.getRelatedNodes(securityContext, this);
		}

		return Collections.emptyList();

	}

	/**
	 * Returns a list of related nodes of the given type, following the relationship(s) defined in
	 * {@see EntityContext}. This method will throw an Exception if the cardinality of the
	 * relationship is not set to OneToMany or ManyToMany.
	 * 
	 * @param type the type of the related node to fetch
	 * @return a single related node of the given type
	 */
	public List<AbstractNode> getRelatedNodes(PropertyKey propertyKey) {

		RelationClass rc = EntityContext.getRelationClassForProperty(getClass(), propertyKey);

		if (rc != null) {

			return rc.getRelatedNodes(securityContext, this);
		}

		return Collections.emptyList();

	}

	/**
	 * Returns database node.
	 *
	 * @return the database node
	 */
	public Node getNode() {

		return dbNode;

	}

	/**
	 * Return the (cached) incoming relationship between this node and the
	 * given principal which holds the security information.
	 *
	 * @param principal
	 * @return incoming security relationship
	 */
	@Override
	public AbstractRelationship getSecurityRelationship(final Principal principal) {

		if (principal == null) {

			return null;
		}

		long userId = principal.getId();

		if (securityRelationships == null) {

			securityRelationships = new HashMap<Long, AbstractRelationship>();
		}

		if (!(securityRelationships.containsKey(userId))) {

			populateSecurityRelationshipCacheMap();
		}

		return securityRelationships.get(userId);

	}

	/**
	 * Return all relationships of given type and direction
	 *
	 * @return list with relationships
	 */
	public List<AbstractRelationship> getRelationships(RelationshipType type, Direction dir) {

		try {

			return (List<AbstractRelationship>) Services.command(securityContext, NodeRelationshipsCommand.class).execute(this, type, dir);

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to get relationships", fex);

		}

		return null;

	}

	/**
	 * Return statistical information on all relationships of this node
	 *
	 * @return number of relationships
	 */
	public Map<RelationshipType, Long> getRelationshipInfo(Direction dir) {

		try {

			return (Map<RelationshipType, Long>) Services.command(securityContext, NodeRelationshipStatisticsCommand.class).execute(this, dir);

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to get relationship info", fex);

		}

		return null;

	}

	/**
	 * Convenience method to get all relationships of this node
	 *
	 * @return
	 */
	public List<AbstractRelationship> getRelationships() {

		return getRelationships(null, Direction.BOTH);

	}

	/**
	 * Convenience method to get all relationships of this node of given direction
	 *
	 * @return
	 */
	public List<AbstractRelationship> getRelationships(final Direction dir) {

		return getRelationships(null, dir);

	}

	/**
	 * Convenience method to get all incoming relationships of this node
	 *
	 * @return
	 */
	public List<AbstractRelationship> getIncomingRelationships() {

		return getRelationships(null, Direction.INCOMING);

	}

	/**
	 * Convenience method to get all outgoing relationships of this node
	 *
	 * @return
	 */
	public List<AbstractRelationship> getOutgoingRelationships() {

		return getRelationships(null, Direction.OUTGOING);

	}

	/**
	 * Convenience method to get all incoming relationships of this node of given type
	 *
	 * @return
	 */
	public List<AbstractRelationship> getIncomingRelationships(final RelationshipType type) {

		return getRelationships(type, Direction.INCOMING);

	}
	
	/**
	 * Convenience method to get all outgoing relationships of this node of given type
	 *
	 * @return
	 */
	public List<AbstractRelationship> getOutgoingRelationships(final RelationshipType type) {

		return getRelationships(type, Direction.OUTGOING);

	}

	/**
	 * Returns the owner node of this node, following an INCOMING OWNS relationship.
	 *
	 * @return the owner node of this node
	 */
	@Override
	public Principal getOwnerNode() {

		if (cachedOwnerNode == null) {

			for (AbstractRelationship s : getRelationships(RelType.OWNS, Direction.INCOMING)) {

				AbstractNode n = s.getStartNode();
				
				if (n == null) {
					
					logger.log(Level.WARNING, "Could not determine owner node!");
					
					return null;
				}

				if (n instanceof Principal) {

					cachedOwnerNode = (Principal) n;

					break;

				}

				logger.log(Level.SEVERE, "Owner node is not a user: {0}[{1}]", new Object[] { n.getName(), n.getId() });

			}

		}

		return cachedOwnerNode;

	}

	/**
	 * Returns the database ID of the owner node of this node.
	 * 
	 * @return the database ID of the owner node of this node
	 */
	public Long getOwnerId() {

		return getOwnerNode().getId();

	}

	/**
	 * Return a list with the connected principals (user, group, role)
	 * @return
	 */
	public List<AbstractNode> getSecurityPrincipals() {

		List<AbstractNode> principalList = new LinkedList<AbstractNode>();

		// check any security relationships
		for (AbstractRelationship r : getRelationships(RelType.SECURITY, Direction.INCOMING)) {

			// check security properties
			AbstractNode principalNode = r.getEndNode();

			principalList.add(principalNode);
		}

		return principalList;

	}

	/**
	 * Return true if this node has a relationship of given type and direction.
	 *
	 * @param type
	 * @param dir
	 * @return
	 */
	public boolean hasRelationship(final RelType type, final Direction dir) {

		List<AbstractRelationship> rels = this.getRelationships(type, dir);

		return ((rels != null) && !(rels.isEmpty()));

	}

	// ----- interface AccessControllable -----
	@Override
	public boolean isGranted(final Permission permission, final Principal principal) {

		if (principal == null) {

			return false;
		}

		// just in case ...
		if (permission == null) {

			return false;
		}

		// superuser
		if (principal instanceof SuperUser) {

			return true;
		}

		// user has full control over his/her own user node
		if (this.equals(principal)) {

			return true;
		}

		AbstractRelationship r = getSecurityRelationship(principal);

		if ((r != null) && r.isAllowed(permission)) {

			return true;
		}

		// Now check possible parent principals
		for (Principal parent : principal.getParents()) {

			if (isGranted(permission, parent)) {

				return true;
			}

		}

		return false;

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
		
		cachedUuid = (String)properties.get(AbstractNode.Key.uuid.name());
		
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
	
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean error = false;

		error |= ValidationHelper.checkStringNotBlank(this, Key.uuid, errorBuffer);
		error |= ValidationHelper.checkStringNotBlank(this, Key.type, errorBuffer);

		return !error;

	}

	@Override
	public boolean isVisibleToPublicUsers() {

		return getVisibleToPublicUsers();

	}

	@Override
	public boolean isVisibleToAuthenticatedUsers() {

		return getBooleanProperty(Key.visibleToAuthenticatedUsers);

	}

	@Override
	public boolean isNotHidden() {

		return !getHidden();

	}

	@Override
	public boolean isHidden() {

		return getHidden();

	}
	
	@Override
	public Date getVisibilityStartDate() {
		return getDateProperty(Key.visibilityStartDate);
	}
	
	@Override
	public Date getVisibilityEndDate() {
		return getDateProperty(Key.visibilityEndDate);
	}

	@Override
	public Date getCreatedDate() {
		return getDateProperty(Key.createdDate);
	}
	
	@Override
	public Date getLastModifiedDate() {
		return getDateProperty(Key.lastModifiedDate);
	}

	// ----- end interface AccessControllable -----
	public boolean isNotDeleted() {

		return !getDeleted();

	}

	public boolean isDeleted() {

		return getDeleted();

	}

	/**
	 * Return true if node is the root node
	 *
	 * @return
	 */
	public boolean isRootNode() {

		return getId() == 0;

	}

	public boolean isVisible() {

		return securityContext.isVisible(this);

	}

	//~--- set methods ----------------------------------------------------

	public void setCreatedBy(final String createdBy) throws FrameworkException {

		setProperty(Key.createdBy, createdBy);

	}

	public void setCreatedDate(final Date date) throws FrameworkException {

		setProperty(Key.createdDate, date);

	}

//	public void setLastModifiedDate(final Date date) throws FrameworkException {
//
//		setProperty(Key.lastModifiedDate.name(), date);
//
//	}

	public void setVisibilityStartDate(final Date date) throws FrameworkException {

		setProperty(Key.visibilityStartDate, date);

	}

	public void setVisibilityEndDate(final Date date) throws FrameworkException {

		setProperty(Key.visibilityEndDate, date);

	}

	public void setVisibleToPublicUsers(final boolean publicFlag) throws FrameworkException {

		setProperty(Key.visibleToPublicUsers, publicFlag);

	}

	public void setVisibleToAuthenticatedUsers(final boolean flag) throws FrameworkException {

		setProperty(Key.visibleToAuthenticatedUsers, flag);

	}

//
	public void setHidden(final boolean hidden) throws FrameworkException {

		setProperty(Key.hidden, hidden);

	}

	public void setDeleted(final boolean deleted) throws FrameworkException {

		setProperty(Key.deleted, deleted);

	}

	public void setType(final String type) throws FrameworkException {

		setProperty(Key.type, type);

	}

	public void setName(final String name) throws FrameworkException {

		setProperty(Key.name, name);

	}

	/**
	 * Set a property in database backend without updating index
	 *
	 * Set property only if value has changed
	 *
	 * @param key
	 * @param convertedValue
	 */
	@Override
	public void setProperty(final PropertyKey key, final Object value) throws FrameworkException {

		setProperty(key, value, updateIndexDefault);

	}

	/**
	 * Split String value and set as String[] property in database backend
	 *
	 * @param key
	 * @param stringList
	 *
	 */
	public void setPropertyAsStringArray(final PropertyKey key, final String value) throws FrameworkException {

		String[] values = StringUtils.split(((String) value), "\r\n");

		setProperty(key, values, updateIndexDefault);

	}

	/**
	 * Store a non-persistent value in this entity.
	 * 
	 * @param key
	 * @param value 
	 */
	public void setTemporaryProperty(final PropertyKey key, Object value) {
		cachedConvertedProperties.put(key, value);
		cachedRawProperties.put(key, value);
	}
	
	/**
	 * Retrieve a previously stored non-persistent value from this entity.
	 */
	public Object getTemporaryProperty(final PropertyKey key) {
		return cachedConvertedProperties.get(key);
	}
	
	/**
	 * Set a property in database backend
	 *
	 * Set property only if value has changed
	 *
	 * Update index only if updateIndex is true
	 *
	 * @param key
	 * @param convertedValue
	 * @param updateIndex
	 */
	public void setProperty(final PropertyKey key, final Object value, final boolean updateIndex) throws FrameworkException {

		Object oldValue = getProperty(key);

		// check null cases
		if ((oldValue == null) && (value == null)) {

			return;
		}

		// no old value exists, set property
		if ((oldValue == null) && (value != null)) {

			setPropertyInternal(key, value);

			return;

		}

		// old value exists and is NOT equal
		if ((oldValue != null) && !oldValue.equals(value)) {

			setPropertyInternal(key, value);
		}

	}

	private void setPropertyInternal(final PropertyKey key, final Object value) throws FrameworkException {

		final Class type = this.getClass();

		if (key == null) {

			logger.log(Level.SEVERE, "Tried to set property with null key (action was denied)");

			throw new FrameworkException(type.getSimpleName(), new NullArgumentToken(null));

		}
		
		// check for read-only properties
		if (EntityContext.isReadOnlyProperty(type, key) || (EntityContext.isWriteOnceProperty(type, key) && (dbNode != null) && dbNode.hasProperty(key.name()))) {

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

		// static relationship detected, create or remove relationship
		RelationClass rel = EntityContext.getRelationClass(type, key);

		if (rel != null) {

			if (value != null && (!(value instanceof Iterable) || ((Iterable) value).iterator().hasNext())) {

				// TODO: check cardinality here
				if (value instanceof Iterable) {

					Collection<GraphObject> collection = rel.getNotion().getCollectionAdapterForSetter(securityContext).adapt(value);

					for (GraphObject graphObject : collection) {

						rel.createRelationship(securityContext, this, graphObject);
					}

				} else {

					GraphObject graphObject = rel.getNotion().getAdapterForSetter(securityContext).adapt(value);

					rel.createRelationship(securityContext, this, graphObject);

				}
			} else {

				// new value is null
				Object existingValue = getProperty(key);

				// do nothing if value is already null
				if (existingValue == null) {

					return;
				}

				// support collection resources, too
				if (existingValue instanceof IterableAdapter) {

					for (Object val : ((IterableAdapter) existingValue)) {

						GraphObject graphObject = rel.getNotion().getAdapterForSetter(securityContext).adapt(val);

						rel.removeRelationship(securityContext, this, graphObject);

					}

				} else {

					GraphObject graphObject = rel.getNotion().getAdapterForSetter(securityContext).adapt(existingValue);

					rel.removeRelationship(securityContext, this, graphObject);

				}
			}

		} else {

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

			if (isDirty) {

				// Don't write directly to database, but store property values
				// in a map for later use
				properties.put(key, convertedValue);
				
			} else {

				// Commit value directly to database
				StructrTransaction transaction = new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						try {

							// save space
							if (convertedValue == null) {

								dbNode.removeProperty(key.name());
							} else {

								// Setting last modified date explicetely is not allowed
								if (!key.equals(Key.lastModifiedDate.name())) {

									if (convertedValue instanceof Date) {

										dbNode.setProperty(key.name(), ((Date) convertedValue).getTime());
									} else {

										dbNode.setProperty(key.name(), convertedValue);

										// set last modified date if not already happened
										dbNode.setProperty(Key.lastModifiedDate.name(), (new Date()).getTime());

									}

									// notify listeners here
									// EntityContext.getGlobalModificationListener().propertyModified(securityContext, thisNode, key, oldValue, convertedValue);

								} else {

									logger.log(Level.FINE, "Tried to set lastModifiedDate explicitely (action was denied)");
								}
							}
						} finally {}

						return null;

					}

				};

				// execute transaction
				Services.command(securityContext, TransactionCommand.class).execute(transaction);
			}

		}

		// remove property from cached properties
		cachedConvertedProperties.remove(key);
		cachedRawProperties.remove(key);
	}

	public void setOwner(final Principal owner) {

		try {

			Command setOwner = Services.command(securityContext, SetOwnerCommand.class);

			setOwner.execute(this, owner);

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to set owner node", fex);

		}

	}

}
