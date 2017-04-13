package org.vaadin.example.treegrid.jdbc.pojo;

/**
 * Created by elmot on 4/13/2017.
 */
public interface NamedItemVisitor<RESULT> {
    RESULT accept(Person person);
    RESULT accept(Company company);
    RESULT accept(Department department);
}
