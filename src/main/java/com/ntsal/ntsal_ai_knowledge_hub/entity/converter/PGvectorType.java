package com.ntsal.ntsal_ai_knowledge_hub.entity.converter;

import com.pgvector.PGvector;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.*;

public class PGvectorType implements UserType<PGvector> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<PGvector> returnedClass() {
        return PGvector.class;
    }

    @Override
    public boolean equals(PGvector x, PGvector y) {
        return x != null && x.equals(y);
    }

    @Override
    public int hashCode(PGvector x) {
        return x != null ? x.hashCode() : 0;
    }

    @Override
    public PGvector nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        Object value = rs.getObject(position);
        if (value == null) {
            return null;
        }
        if (value instanceof PGvector) {
            return (PGvector) value;
        }
        return new PGvector(value.toString());
    }

    @Override
    public void nullSafeSet(PreparedStatement st, PGvector value, int index, SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            st.setObject(index, value, Types.OTHER);
        }
    }

    @Override
    public PGvector deepCopy(PGvector value) {
        if (value == null) {
            return null;
        }
        try {
            return new PGvector(value.toString());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to deep copy PGvector", e);
        }
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(PGvector value) {
        return value != null ? value.toString() : null;
    }

    @Override
    public PGvector assemble(Serializable cached, Object owner) {
        if (cached == null) {
            return null;
        }
        try {
            return new PGvector(cached.toString());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to assemble PGvector", e);
        }
    }

    @Override
    public PGvector replace(PGvector detached, PGvector managed, Object owner) {
        return deepCopy(detached);
    }
}

