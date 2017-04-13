package org.vaadin.example.treegrid.jdbc.pojo;

/**
 * Created by elmot on 4/11/2017.
 */
public class Company extends NamedItem {
    private final String name;

    public Company(long id,String name) {
        super(id);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public <RESULT> RESULT visit(NamedItemVisitor<RESULT> visitor) {
        return visitor.accept(this);
    }

}
