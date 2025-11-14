package fr.anisekai.server.domain.entities;

import fr.anisekai.core.persistence.domain.BaseEntity;
import fr.anisekai.utils.EntityUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Entity
public class Setting extends BaseEntity<String> {

    @Id
    private String id;

    @Column
    private String value;

    @Override
    public String getId() {

        return this.id;
    }

    @Override
    public void setId(String id) {

        this.id = id;
    }

    public @Nullable String getValue() {

        return this.value;
    }

    public void setValue(String value) {

        this.value = value;
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof Setting setting) return EntityUtils.equals(this, setting);
        return false;
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(this.getId());
    }

}
