package com.restaurante.shared.domain;

public abstract class ValueObject {

    public abstract java.util.List<?> getValueObjects();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ValueObject that = (ValueObject) o;
        return java.util.Objects.equals(getValueObjects(), that.getValueObjects());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getValueObjects());
    }
}