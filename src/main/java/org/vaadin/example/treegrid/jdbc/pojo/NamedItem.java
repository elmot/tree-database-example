package org.vaadin.example.treegrid.jdbc.pojo;

/**
 * Created by elmot on 4/13/2017.
 */
public abstract class  NamedItem {
    private final long id;

    public NamedItem(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public abstract String getName();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NamedItem namedItem = (NamedItem) o;

        return id == namedItem.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    public abstract <RESULT> RESULT visit(NamedItemVisitor<RESULT> visitor);
}
