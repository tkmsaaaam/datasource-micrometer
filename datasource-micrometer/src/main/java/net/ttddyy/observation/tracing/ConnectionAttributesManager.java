package net.ttddyy.observation.tracing;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.micrometer.common.lang.Nullable;
import io.micrometer.observation.Observation.Scope;
import net.ttddyy.dsproxy.ConnectionInfo;

/**
 * @author Tadaya Tsuyukubo
 */
public interface ConnectionAttributesManager {
	@Nullable
	ConnectionAttributes put(String connectionId, ConnectionAttributes attributes);

	@Nullable
	ConnectionAttributes get(String connectionId);

	@Nullable
	ConnectionAttributes remove(String connectionId);

	class ConnectionAttributes {
		ConnectionContext connectionContext;

		ConnectionInfo connectionInfo;

		URI connectionUrl;

		Scope scope;

		ResultSetAttributesManager resultSetAttributesManager = new ResultSetAttributesManager();
	}

	class ResultSetAttributesManager {
		Map<ResultSet, ResultSetAttributes> byResultSet = new ConcurrentHashMap<>();

		Map<ResultSet, Statement> statements = new ConcurrentHashMap<>();

		ResultSetAttributes add(ResultSet resultSet, @Nullable Statement statement, ResultSetAttributes attributes) {
			this.byResultSet.put(resultSet, attributes);
			if (statement != null) {
				this.statements.put(resultSet, statement);
			}
			return attributes;
		}

		@Nullable
		ResultSetAttributes getByResultSet(ResultSet resultSet) {
			return this.byResultSet.get(resultSet);
		}

		@Nullable
		ResultSetAttributes removeByResultSet(ResultSet resultSet) {
			this.statements.remove(resultSet);
			return this.byResultSet.remove(resultSet);
		}

		Set<ResultSetAttributes> removeByStatement(Statement statement) {
			Set<ResultSet> resultSets = new HashSet<>();
			Iterator<Entry<ResultSet, Statement>> iter = this.statements.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<ResultSet, Statement> entry = iter.next();
				if (entry.getValue().equals(statement)) {
					resultSets.add(entry.getKey());
					iter.remove();
				}
			}

			return resultSets.stream().map(this.byResultSet::remove)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
		}

		Set<ResultSetAttributes> removeAll() {
			Set<ResultSetAttributes> attributes = new HashSet<>(this.byResultSet.values());
			this.byResultSet.clear();
			this.statements.clear();
			return attributes;
		}
	}

	class ResultSetAttributes {
		Statement statement;

		Scope scope;

		ResultSetContext context;
	}

}