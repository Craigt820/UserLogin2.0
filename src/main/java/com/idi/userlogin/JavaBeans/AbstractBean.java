package com.idi.userlogin.JavaBeans;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import javafx.beans.property.*;

public abstract class AbstractBean<T> extends RecursiveTreeObject<T> {
    private StringProperty name;
    private IntegerProperty id;

    public AbstractBean(int id, String name) {
        super();
        this.name = new SimpleStringProperty(name);
        this.id = new SimpleIntegerProperty(id);
    }


    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public int getId() {
        return id.get();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public void setId(int id) {
        this.id.set(id);
    }


    public AbstractBean() {
        super();
    }
}
