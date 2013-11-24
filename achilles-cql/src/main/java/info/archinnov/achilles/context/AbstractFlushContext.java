/**
 *
 * Copyright (C) 2012-2013 DuyHai DOAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.archinnov.achilles.context;

import static info.archinnov.achilles.consistency.ConsistencyConvertor.getCQLLevel;
import info.archinnov.achilles.statement.prepared.BoundStatementWrapper;
import info.archinnov.achilles.type.ConsistencyLevel;

import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Query;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Statement;

public abstract class AbstractFlushContext {
	protected DaoContext daoContext;

	protected List<BoundStatementWrapper> boundStatementWrappers = new ArrayList<BoundStatementWrapper>();
	protected List<Statement> statements = new ArrayList<Statement>();

	protected ConsistencyLevel consistencyLevel;

	public AbstractFlushContext(DaoContext daoContext, ConsistencyLevel consistencyLevel) {
		this.daoContext = daoContext;
		this.consistencyLevel = consistencyLevel;
	}

	protected AbstractFlushContext(DaoContext daoContext, List<BoundStatementWrapper> boundStatementWrappers,
                                   ConsistencyLevel consistencyLevel) {
		this.boundStatementWrappers = boundStatementWrappers;
		this.daoContext = daoContext;
		this.consistencyLevel = consistencyLevel;
	}

	public void cleanUp() {
		boundStatementWrappers.clear();
		statements.clear();
		consistencyLevel = null;
	}

	protected void doFlush() {
		for (BoundStatementWrapper wrapper : boundStatementWrappers) {

			daoContext.execute(wrapper.getBs(), wrapper.getValues());
		}
		for (Statement statement : statements) {
			daoContext.execute(statement);
		}

		cleanUp();

	}

	public void pushBoundStatement(BoundStatementWrapper bsWrapper, ConsistencyLevel writeConsistencyLevel) {
		BoundStatement boundStatement = bsWrapper.getBs();
		if (consistencyLevel != null) {
			boundStatement.setConsistencyLevel(getCQLLevel(consistencyLevel));
		} else {
			boundStatement.setConsistencyLevel(getCQLLevel(writeConsistencyLevel));
		}
		boundStatementWrappers.add(bsWrapper);
	}

	public void pushStatement(Statement statement, ConsistencyLevel writeConsistencyLevel) {

		if (consistencyLevel != null) {
			statement.setConsistencyLevel(getCQLLevel(consistencyLevel));
		} else {
			statement.setConsistencyLevel(getCQLLevel(writeConsistencyLevel));
		}
		statements.add(statement);
	}

	public ResultSet executeImmediateWithConsistency(Query query, ConsistencyLevel readConsistencyLevel,
			Object... boundValues) {
		query.setConsistencyLevel(getCQLLevel(readConsistencyLevel));
		return daoContext.execute(query, boundValues);
	}

	public void setConsistencyLevel(ConsistencyLevel consistencyLevel) {
		this.consistencyLevel = consistencyLevel;
	}

	public ConsistencyLevel getConsistencyLevel() {
		return consistencyLevel;
	}


    public abstract void startBatch();

    public abstract void flush();

    public abstract void endBatch();

    public abstract FlushType type();

    public abstract AbstractFlushContext duplicate() ;

    public static enum FlushType {
        BATCH,IMMEDIATE;
    }

    @Override
    public String toString() {
        return type().toString();
    }
}