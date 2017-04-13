package org.vaadin.example.treegrid.jdbc;

import com.vaadin.data.provider.AbstractHierarchicalDataProvider;
import com.vaadin.data.provider.HierarchicalQuery;
import org.vaadin.example.treegrid.jdbc.pojo.Company;
import org.vaadin.example.treegrid.jdbc.pojo.Department;
import org.vaadin.example.treegrid.jdbc.pojo.NamedItem;
import org.vaadin.example.treegrid.jdbc.pojo.NamedItemVisitor;
import org.vaadin.example.treegrid.jdbc.pojo.Person;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PeopleData extends AbstractHierarchicalDataProvider<NamedItem, Void> {
    private static final Logger LOGGER = Logger.getLogger(PeopleData.class.getName());
    private ChildrenCounter childrenCounter = new ChildrenCounter();
    private ChildrenReader childrenReader = new ChildrenReader();

    @Override
    public int getChildCount(HierarchicalQuery<NamedItem, Void> query) {
        NamedItem parent = query.getParent();
        if (parent == null) return childrenCounter.countRoots();
        return parent.visit(childrenCounter);
    }

    @Override
    public Stream<NamedItem> fetchChildren(HierarchicalQuery<NamedItem, Void> query) {
        NamedItem parent = query.getParent();
        if (parent == null) return childrenReader.fetchRoots();
        return parent.visit(childrenReader);
    }

    @Override
    public boolean hasChildren(NamedItem item) {
        return getChildCount(new HierarchicalQuery<>(null, item)) > 0;
    }

    @Override
    public boolean isInMemory() {
        return false;
    }

    @Override
    public Object getId(NamedItem item) {
        return item.getClass().getCanonicalName() + item.getId();
    }

    private class ChildrenCounter implements NamedItemVisitor<Integer> {
        private int selectCount(String sql, Object... params) {

            try (Connection connection = DBEngine.getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    statement.setObject(i + 1, params[i]);
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getInt(1);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Integer accept(Person person) {
            return 0;
        }

        @Override
        public Integer accept(Company company) {
            return selectCount(
                    "select count(*) from department where company_id = ?", company.getId());
        }

        @Override
        public Integer accept(Department department) {
            return selectCount(
                    "select count(*) from people where department_id = ?", department.getId());
        }

        int countRoots() {
            return selectCount("select count(*) from company");
        }
    }

    @FunctionalInterface
    public interface DataRetriever<T> {
        T readRow(ResultSet resultSet) throws SQLException;
    }

    private static class ResultSetToSpliterator<T> extends
            Spliterators.AbstractSpliterator<T> implements AutoCloseable {
        private final ResultSet resultSet;
        private final DataRetriever<T> jdbcReader;
        private final Statement statement;
        private final Connection connection;
        private int limit;

        ResultSetToSpliterator(Connection connection, Statement statement, ResultSet resultSet, DataRetriever<T> jdbcReader, int limit)
                throws SQLException {
            super(Long.MAX_VALUE, IMMUTABLE | NONNULL);
            this.connection = connection;
            this.statement = statement;
            this.resultSet = resultSet;
            this.jdbcReader = jdbcReader;
            if (resultSet.next()) {
                this.limit = limit;
            } else {
                this.limit = 0;
            }
            if (this.limit <= 0) {
                close();
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            try {
                if (resultSet.isClosed()) {
                    return false;
                }
                T dto = jdbcReader.readRow(resultSet);
                action.accept(dto);
                if (resultSet.next()) {
                    limit--;
                } else {
                    limit = 0;
                }
                if (limit <= 0) {
                    close();
                }
            } catch (SQLException e) {
                throw new RuntimeException("ResultSet row retrieve error", e);
            }

            return true;
        }

        @Override
        public void close() {
            Stream.of(resultSet, statement, connection)
                    .forEach(closeable -> {
                                try {
                                    closeable.close();
                                } catch (Exception e) {
                                    LOGGER.log(Level.WARNING,
                                            closeable + " was closed with exception", e);
                                }
                            }
                    );
        }
    }

    private class ChildrenReader implements NamedItemVisitor<Stream<NamedItem>> {
        Stream<NamedItem> fetchRoots() {
            try {
                    Connection connection = DBEngine.getDataSource().getConnection();
                    Statement statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery("SELECT * FROM company");
                    ResultSetToSpliterator<NamedItem> spliterator = new ResultSetToSpliterator<>(connection, statement, resultSet,
                        resultSet1 -> new Company(resultSet1.getLong("company_id"),
                                resultSet1.getString("company_name")), Integer.MAX_VALUE
                );
                return StreamSupport.stream(spliterator, false);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Stream<NamedItem> accept(Person person) {
            return Stream.empty();
        }

        @Override
        public Stream<NamedItem> accept(Company company) {
            try {
                Connection connection = DBEngine.getDataSource().getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM department WHERE company_id = ?");
                statement.setLong(1, company.getId());
                ResultSet resultSet = statement.executeQuery();
                ResultSetToSpliterator<NamedItem> spliterator = new ResultSetToSpliterator<>(connection, statement, resultSet, resultSet1 ->
                        new Department(
                                resultSet1.getLong("department_id"),
                                resultSet1.getLong("company_id"),
                                resultSet1.getString("department_name")), Integer.MAX_VALUE
                );
                return StreamSupport.stream(spliterator, false);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Stream<NamedItem> accept(Department department) {
            try {
                Connection connection = DBEngine.getDataSource().getConnection();
                PreparedStatement statement = connection
                        .prepareStatement("SELECT * FROM people WHERE department_id = ?");
                statement.setLong(1, department.getId());
                ResultSet resultSet = statement.executeQuery();
                ResultSetToSpliterator<NamedItem> spliterator = new ResultSetToSpliterator<>(connection, statement, resultSet, resultSet1 ->
                        new Person(resultSet1.getLong("id"),
                                resultSet1.getLong("department_id"),
                                resultSet1.getString("first_name"),
                                resultSet1.getString("last_name"),
                                resultSet1.getString("email"),
                                resultSet1.getString("gender")
                        ), Integer.MAX_VALUE
                );
                return StreamSupport.stream(spliterator, false);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
