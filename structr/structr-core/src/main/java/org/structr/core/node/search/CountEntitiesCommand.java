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
package org.structr.core.node.search;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.structr.common.error.FrameworkException;
import org.structr.core.node.NodeService.NodeIndex;
import org.structr.core.node.NodeServiceCommand;

/**
 *
 * @author Christian Morgner
 */
public class CountEntitiesCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(CountEntitiesCommand.class.getName());
	
	@Override
	public Object execute(Object... parameters) throws FrameworkException {
		
		Index<Node> index = (Index<Node>)arguments.get(NodeIndex.keyword.name());
		String lowerTerm = null;
		String upperTerm = null;
		String type = null;
		int count = -1;

		if(parameters.length > 0 && parameters[0] instanceof Class) {
			type = ((Class)parameters[0]).getSimpleName();
		}
		
		if(parameters.length > 1 && parameters[1] instanceof String) {
			lowerTerm = (String)parameters[1];
		}
		
		if(parameters.length > 2 && parameters[2] instanceof String) {
			upperTerm = (String)parameters[2];
		}
		
		if(type != null) {

			// create type query first
			Query typeQuery = new TermQuery(new Term("type", type));
			Query actualQuery = null;

			// if date terms are set, create date query
			if(lowerTerm != null && upperTerm != null) {

				// do range query, including start value, excluding end value!
				Query dateQuery = new TermRangeQuery("createdDate", lowerTerm, upperTerm, true, false);
				
				BooleanQuery booleanQuery = new BooleanQuery();
				booleanQuery.add(dateQuery, BooleanClause.Occur.MUST);
				booleanQuery.add(typeQuery, BooleanClause.Occur.MUST);
				
				actualQuery = booleanQuery;
				
			} else {
				
				actualQuery  = typeQuery;
				
			}

			long start = System.currentTimeMillis();

			IndexHits hits = index.query(actualQuery);
			count = hits.size();

			long end = System.currentTimeMillis();

			logger.log(Level.FINE, "Counted {0} entities in {1} ms.", new Object[] { count, (end-start) } );

			hits.close();
		}
		
		return count;
	}
}
