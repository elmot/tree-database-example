package org.vaadin.example.treegrid.jdbc;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.ValueProvider;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.vaadin.example.treegrid.jdbc.pojo.NamedItem;
import org.vaadin.example.treegrid.jdbc.pojo.Person;

import javax.servlet.annotation.WebServlet;
import java.util.function.Function;

/**
 * This UI is the application entry point.
 */
@SuppressWarnings("unused")
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
        return (NamedItem item) -> {
            if (item instanceof Person) {
                return personExtractor.apply((Person) item);
            }
            else {
                return "--";
            }
        };
    }
}
