package org.vaadin.example.treegrid.jdbc.pojo;

/**
 * Created by elmot on 4/11/2017.
 */
public class Department extends NamedItem {
    private final String name;
    private final long companyId;

    public Department(long id, long companyId, String name) {
        super(id);
        this.name = name;
        this.companyId = companyId;
    }

    private long getCompanyId() {
        return companyId;
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
