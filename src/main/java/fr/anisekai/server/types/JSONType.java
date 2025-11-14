package fr.anisekai.server.types;

import fr.anisekai.core.internal.json.AnisekaiJson;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class JSONType implements UserType<AnisekaiJson> {

    @Override
    public int getSqlType() {

        return Types.LONGVARCHAR;
    }

    @Override
    public Class<AnisekaiJson> returnedClass() {

        return AnisekaiJson.class;
    }

    @Override
    public boolean equals(AnisekaiJson json1, AnisekaiJson json2) {

        return (json1 == null && json2 == null) ||
                (json1 != null && json2 != null && json1.toString().equals(json2.toString()));
    }

    @Override
    public int hashCode(AnisekaiJson bookshelfJson) {

        return bookshelfJson.hashCode();
    }

    @Override
    public AnisekaiJson nullSafeGet(ResultSet rs, int column, SharedSessionContractImplementor sharedSessionContractImplementor, Object o) throws SQLException {

        String raw = rs.getString(column);
        if (raw != null) {
            return new AnisekaiJson(raw);
        }

        return null;
    }

    @Override
    public void nullSafeSet(PreparedStatement pst, AnisekaiJson json, int column, SharedSessionContractImplementor sharedSessionContractImplementor) throws SQLException {

        if (json == null) {
            pst.setNull(column, this.getSqlType());
            return;
        }
        pst.setString(column, json.toString());
    }

    @Override
    public AnisekaiJson deepCopy(AnisekaiJson bookshelfJson) {

        return new AnisekaiJson(bookshelfJson.toMap());
    }

    @Override
    public boolean isMutable() {

        return true;
    }

    @Override
    public Serializable disassemble(AnisekaiJson bookshelfJson) {

        return bookshelfJson.toString();
    }

    @Override
    public AnisekaiJson assemble(Serializable serializable, Object o) {

        return new AnisekaiJson((String) serializable);
    }

}
