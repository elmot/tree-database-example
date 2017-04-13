package org.vaadin.example.treegrid.jdbc;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.ValueProvider;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.vaadin.example.treegrid.jdbc.pojo.Company;
import org.vaadin.example.treegrid.jdbc.pojo.Department;
import org.vaadin.example.treegrid.jdbc.pojo.NamedItem;
import org.vaadin.example.treegrid.jdbc.pojo.NamedItemVisitor;
import org.vaadin.example.treegrid.jdbc.pojo.Person;

import java.util.function.Function;

/**
 * This UI is the application entry point. A UI may either represent a browser window
 * (or tab) or some part of a html page where a Vaadin application is embedded.
 * <p>
 * The UI is initialized using {@link #init(VaadinRequest)}. This method is intended to be
 * overridden to add component to the user interface and initialize non-component functionality.
 */
public class TreeUI extends UI {

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        final VerticalLayout layout = new VerticalLayout();
        TreeGrid<NamedItem> treeGrid = new TreeGrid<>();

        treeGrid.addColumn(NamedItem::getName).setId("name").setCaption("Name");
        treeGrid.setHierarchyColumn("name");

        treeGrid.addColumn(ofPerson(Person::getFirstName)).setCaption("First Name");
        treeGrid.addColumn(ofPerson(Person::getLastName)).setCaption("Last Name");
        treeGrid.addColumn(ofPerson(Person::getEmail)).setCaption("e-mail");
        treeGrid.addColumn(ofPerson(Person::getGender)).setCaption("Gender");
        treeGrid.setDataProvider(new PeopleData());

        layout.addComponentsAndExpand(treeGrid);
        setContent(layout);
    }

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = TreeUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {
    }

    private static ValueProvider<NamedItem, String> ofPerson(Function<Person, String> personExtractor) {
        return (NamedItem item) -> item.visit(new NamedItemVisitor<String>() {

            @Override
            public String accept(Person person) {
                return personExtractor.apply(person);
            }

            @Override
            public String accept(Company company) {
                return "--";
            }

            @Override
            public String accept(Department department) {
                return "--";
            }
        });

    }

}
