/*
 *  Copyright (C) 2012 Axel Morgner
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
package org.structr.core;

import java.util.*;
import org.structr.common.GraphObjectComparator;
import org.structr.common.GraphObjectComparator.SortOrder;
import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */

public class GraphObjectMap implements GraphObject, Map<PropertyKey, Object> {

	private Map<PropertyKey, Object> values = new LinkedHashMap<PropertyKey, Object>();

	@Override
	public long getId() {
		return -1;
	}

	@Override
	public String getUuid() {
		return getStringProperty(AbstractNode.Key.uuid);
	}

	@Override
	public String getType() {
		return getStringProperty(AbstractNode.Key.uuid);
	}

	@Override
	public Iterable<PropertyKey> getPropertyKeys(String propertyView) {
		return values.keySet();
	}

	@Override
	public void setProperty(PropertyKey key, Object value) throws FrameworkException {
		values.put(key, value);
	}

	@Override
	public Object getProperty(PropertyKey key) {
		return values.get(key);
	}

	@Override
	public String getStringProperty(PropertyKey propertyKey) {
		return (String)getProperty(propertyKey);
	}

	@Override
	public Integer getIntProperty(PropertyKey propertyKey) {
		return (Integer)getProperty(propertyKey);
	}

	@Override
	public Long getLongProperty(PropertyKey propertyKey) {
		return (Long)getProperty(propertyKey);
	}

	@Override
	public Date getDateProperty(PropertyKey key) {
		return (Date)getProperty(key);
	}

	@Override
	public boolean getBooleanProperty(PropertyKey key) throws FrameworkException {
		return (Boolean)getProperty(key);
	}

	@Override
	public Double getDoubleProperty(PropertyKey key) throws FrameworkException {
		return (Double)getProperty(key);
	}

	@Override
	public Comparable getComparableProperty(PropertyKey key) throws FrameworkException {
		return (Comparable)getProperty(key);
	}

	@Override
	public void removeProperty(PropertyKey key) throws FrameworkException {
		values.remove(key);
	}

	@Override
	public PropertyKey getDefaultSortKey() {
		return null;
	}

	@Override
	public SortOrder getDefaultSortOrder() {
		return GraphObjectComparator.SortOrder.ASCENDING;
	}

	@Override
	public void unlockReadOnlyPropertiesOnce() {
	}

	@Override
	public boolean beforeCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return true;
	}

	@Override
	public boolean beforeModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return true;
	}

	@Override
	public boolean beforeDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, Map<String, Object> properties) throws FrameworkException {
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

	// ----- interface map -----
	@Override
	public int size() {
		return values.size();
	}

	@Override
	public boolean isEmpty() {
		return values.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return values.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return values.containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return values.get(key);
	}

	@Override
	public Object put(PropertyKey key, Object value) {
		return values.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return values.remove(key);
	}

	@Override
	public void putAll(Map m) {
		values.putAll(m);
	}

	@Override
	public void clear() {
		values.clear();
	}

	@Override
	public Set keySet() {
		return values.keySet();
	}

	@Override
	public Collection values() {
		return values.values();
	}

	@Override
	public Set entrySet() {
		return values.entrySet();
	}

	@Override
	public Object getPropertyForIndexing(PropertyKey key) {
		return getProperty(key);
	}
}
