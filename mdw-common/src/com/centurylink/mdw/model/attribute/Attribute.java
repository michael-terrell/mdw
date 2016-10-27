/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.attribute;

import java.io.Serializable;
import java.util.List;

public class Attribute implements Serializable, Comparable<Attribute> {

    private Long id;
    private String name;
    private String value;
    private String group;

    public Attribute(){
    }

    public Attribute(String name, String value) {
        this.id = null;
        this.name = name;
        this.value = value;
    }

    public Attribute(Long id, String name, String value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public Long getAttributeId() {
        return id;
    }

    public void setAttributeId(Long id) {
        this.id = id;
    }

    public String getAttributeName() {
        return name;
    }

    public void setAttributeName(String name) {
        this.name = name;
    }

    public String getAttributeValue() {
        return value;
    }

    public void setAttributeValue(String value) {
        this.value = value;
    }

    public String getAttributeGroup() {
        return group;
    }

    public void setAttributeGroup(String group) {
        this.group = group;
    }

    public static String findAttribute(List<Attribute> attrs, String name) {
        if (attrs==null) return null;
        for (Attribute attr : attrs) {
            if (name.equals(attr.getAttributeName()))
                return attr.getAttributeValue();
        }
        return null;
    }

    /**
     * Set the value of a process attribute.
     * If the value is null, the attribute is removed.
     * If the attribute does not exist and the value is not null, the attribute
     * is created.
     * @param name attribute name
     * @param v value to be set. When it is null, the attribute is removed
     */
    public static void setAttribute(List<Attribute> attrs, String name, String v) {
        for (Attribute attr : attrs) {
            if (name.equals(attr.getAttributeName())) {
                if (v!=null) attr.setAttributeValue(v);
                else attrs.remove(attr);  // TODO this will throw a concurrent modification exception
                return;
            }
        }
        if (v!=null) {
            Attribute attr = new Attribute(null, name, v);
            // TODO: need to retire attribute type concept
            attrs.add(attr);
        }
    }

    public static void removeAttribute(List<Attribute> attrs, String name) {
        setAttribute(attrs, name, null);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Attribute))
            return false;
        Attribute other = (Attribute) o;

        if (getAttributeName() == null)
            return other.getAttributeName() == null;
        else
            return getAttributeName().equals(other.getAttributeName());
    }

    public int compareTo(Attribute other) {
        if (other == null || other.getAttributeName() == null)
            return 1;
        if (this.getAttributeName() == null)
            return -1;

        return this.getAttributeName().compareTo(other.getAttributeName());
    }

}