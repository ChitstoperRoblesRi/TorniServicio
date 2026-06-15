package com.tornillos.dao;

import com.tornillos.config.DatabaseConfig;
import com.tornillos.model.Alerta;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AlertaDAO {

    // 🌟 MODIFICADO: Ahora inserta siempre un registro nuevo e independiente para conservar la trazabilidad histórica lineal
    public void crear(Alerta a) throws SQLException {
        String sql = "INSERT INTO alertas (tornillo_id, tipo, mensaje) VALUES (?,?,?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getTornilloId());
            ps.setString(2, a.getTipo());
            ps.setString(3, a.getMensaje());
            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    a.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public boolean existeAlertaActivaEvitarRepetidos(int tornilloId, String tipo) throws SQLException {
        // 1. Buscamos la fecha de la última vez que este tornillo registró EXACTAMENTE este tipo de alerta
        String sqlAlerta = "SELECT creada_en FROM alertas WHERE tornillo_id = ? AND tipo = ? ORDER BY creada_en DESC LIMIT 1";
        Timestamp fechaUltimaAlerta = null;
        
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sqlAlerta)) {
            ps.setInt(1, tornilloId);
            ps.setString(2, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    fechaUltimaAlerta = rs.getTimestamp("creada_en");
                }
            }
        }
        
        // SOLUCIÓN AL ERROR EN TERMINAL: Si el tornillo es nuevo o no tiene alertas previas de este tipo, 
        // significa que NO es un duplicado. Retornamos false INMEDIATAMENTE sin ejecutar la segunda query.
        // Esto evita que se intente enviar un parámetro NULL a PostgreSQL y rompa el hilo del servicio.
        if (fechaUltimaAlerta == null) {
            return false;
        }
        
        // 2. Si llegamos aquí, sí existe una alerta previa. Evaluamos si hubo reabastecimientos posteriores.
        String sqlEntradaIntermedia = "SELECT 1 FROM entradas WHERE tornillo_id = ? AND fecha > ? AND activo = true LIMIT 1";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sqlEntradaIntermedia)) {
            ps.setInt(1, tornilloId);
            ps.setTimestamp(2, fechaUltimaAlerta); // Ahora garantizamos que nunca será null
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Hubo una entrada intermedia en el almacén. El ciclo anterior se cerró.
                    return false; 
                }
            }
        }
        
        // Si no ha habido entradas desde esa última alerta, es un duplicado redundante del Timer. Bloqueamos.
        return true;
    }

    public List<Alerta> listarActivas() throws SQLException {
        List<Alerta> lista = new ArrayList<>();
        String sql = "SELECT DISTINCT ON (a.tornillo_id) a.*, t.nombre AS tornillo_nombre, t.codigo AS tornillo_codigo " +
                     "FROM alertas a " +
                     "JOIN tornillos t ON a.tornillo_id = t.id " +
                     "WHERE t.activo = true AND t.stock_actual <= t.stock_minimo " +
                     "AND a.tipo = (CASE " +
                     "    WHEN t.stock_actual = 0 THEN 'SIN_STOCK' " +
                     "    WHEN t.stock_actual <= t.stock_minimo * 0.5 THEN 'STOCK_CRITICO' " +
                     "    ELSE 'STOCK_BAJO' " +
                     "END) " +
                     "ORDER BY a.tornillo_id, a.creada_en DESC";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public int contarActivas() throws SQLException {
        String sql = "SELECT COUNT(DISTINCT a.tornillo_id) FROM alertas a " +
                     "JOIN tornillos t ON a.tornillo_id = t.id " +
                     "WHERE t.activo = true AND t.stock_actual <= t.stock_minimo";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public List<Alerta> buscar(String criterio) throws SQLException {
        List<Alerta> lista = new ArrayList<>();
        String sql = "SELECT DISTINCT ON (a.tornillo_id) a.*, t.nombre AS tornillo_nombre, t.codigo AS tornillo_codigo " +
                     "FROM alertas a " +
                     "JOIN tornillos t ON a.tornillo_id = t.id " +
                     "WHERE t.activo = true AND t.stock_actual <= t.stock_minimo " +
                     "AND a.tipo = (CASE " +
                     "    WHEN t.stock_actual = 0 THEN 'SIN_STOCK' " +
                     "    WHEN t.stock_actual <= t.stock_minimo * 0.5 THEN 'STOCK_CRITICO' " +
                     "    ELSE 'STOCK_BAJO' " +
                     "END) " +
                     "AND (LOWER(t.nombre) LIKE LOWER(?) OR LOWER(t.codigo) LIKE LOWER(?)) " +
                     "ORDER BY a.tornillo_id, a.creada_en DESC";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            String p = "%" + criterio + "%";
            ps.setString(1, p);
            ps.setString(2, p);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public Alerta buscarNoEnviada(int tornilloId, String tipo) throws SQLException {
        String sql = "SELECT a.*, t.nombre AS tornillo_nombre, t.codigo AS tornillo_codigo " +
                     "FROM alertas a JOIN tornillos t ON a.tornillo_id=t.id " +
                     "WHERE a.tornillo_id=? AND a.tipo=? AND a.enviada_email=false " +
                     "ORDER BY a.creada_en DESC LIMIT 1"; // Sincronizado para traer el registro lineal más reciente
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, tornilloId);
            ps.setString(2, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapear(rs);
            }
        }
        return null;
    }

    public void marcarComoEnviada(int id) throws SQLException {
        String sql = "UPDATE alertas SET enviada_email=true WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Alerta> listarHistorialPorTornillo(int tornilloId) throws SQLException {
        List<Alerta> lista = new ArrayList<>();
        String sql = "SELECT a.*, t.nombre AS tornillo_nombre, t.codigo AS tornillo_codigo " +
                     "FROM alertas a JOIN tornillos t ON a.tornillo_id=t.id " +
                     "WHERE a.tornillo_id=? ORDER BY a.creada_en DESC";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, tornilloId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public List<Alerta> listarHistorial(String desde, String hasta, String criterio) throws SQLException {
        List<Alerta> lista = new ArrayList<>();
        StringBuilder sb = new StringBuilder(
            "SELECT a.*, t.nombre AS tornillo_nombre, t.codigo AS tornillo_codigo " +
            "FROM alertas a JOIN tornillos t ON a.tornillo_id = t.id WHERE 1=1"
        );
        
        if (desde != null && !desde.isEmpty()) {
            sb.append(" AND a.creada_en >= ?");
        }
        if (hasta != null && !hasta.isEmpty()) {
            sb.append(" AND a.creada_en <= ?");
        }
        if (criterio != null && !criterio.isEmpty()) {
            sb.append(" AND (LOWER(t.nombre) LIKE LOWER(?) OR LOWER(t.codigo) LIKE LOWER(?))");
        }
        
        sb.append(" ORDER BY a.creada_en DESC");

        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sb.toString())) {
            int paramIdx = 1;
            if (desde != null && !desde.isEmpty()) {
                ps.setTimestamp(paramIdx++, Timestamp.valueOf(desde + " 00:00:00"));
            }
            if (hasta != null && !hasta.isEmpty()) {
                ps.setTimestamp(paramIdx++, Timestamp.valueOf(hasta + " 23:59:59"));
            }
            if (criterio != null && !criterio.isEmpty()) {
                String p = "%" + criterio + "%";
                ps.setString(paramIdx++, p);
                ps.setString(paramIdx++, p);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM alertas WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void eliminarTodas() throws SQLException {
        String sql = "DELETE FROM alertas";
        try (Statement st = DatabaseConfig.getConnection().createStatement()) {
            st.executeUpdate(sql);
        }
    }

    public List<Alerta> buscarConFiltroCruzado(String criterio, String tipoFiltro) throws SQLException {
        List<Alerta> lista = new ArrayList<>();
        StringBuilder sb = new StringBuilder(
            "SELECT DISTINCT ON (a.tornillo_id) a.*, t.nombre AS tornillo_nombre, t.codigo AS tornillo_codigo " +
            "FROM alertas a JOIN tornillos t ON a.tornillo_id = t.id " +
            "WHERE t.activo = true AND t.stock_actual <= t.stock_minimo " +
            "AND a.tipo = (CASE " +
            "    WHEN t.stock_actual = 0 THEN 'SIN_STOCK' " +
            "    WHEN t.stock_actual <= t.stock_minimo * 0.5 THEN 'STOCK_CRITICO' " +
            "    ELSE 'STOCK_BAJO' " +
            "END) "
        );
        
        if (tipoFiltro != null && !tipoFiltro.isEmpty()) {
            sb.append(" AND a.tipo = ? ");
        }
        if (criterio != null && !criterio.isEmpty()) {
            sb.append(" AND (LOWER(t.nombre) LIKE LOWER(?) OR LOWER(t.codigo) LIKE LOWER(?) OR LOWER(a.mensaje) LIKE LOWER(?)) ");
        }
        
        sb.append(" ORDER BY a.tornillo_id, a.creada_en DESC");

        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sb.toString())) {
            int idx = 1;
            if (tipoFiltro != null && !tipoFiltro.isEmpty()) {
                ps.setString(idx++, tipoFiltro);
            }
            if (criterio != null && !criterio.isEmpty()) {
                String p = "%" + criterio + "%";
                ps.setString(idx++, p);
                ps.setString(idx++, p);
                ps.setString(idx++, p);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    private Alerta mapear(ResultSet rs) throws SQLException {
        Alerta a = new Alerta();
        a.setId(rs.getInt("id"));
        a.setTornilloId(rs.getInt("tornillo_id"));
        a.setTornilloNombre(rs.getString("tornillo_nombre"));
        a.setTornilloCodigo(rs.getString("tornillo_codigo"));
        a.setTipo(rs.getString("tipo"));
        a.setMensaje(rs.getString("mensaje"));
        a.setEnviadaEmail(rs.getBoolean("enviada_email"));
        Timestamp ts = rs.getTimestamp("creada_en");
        if (ts != null) {
            a.setCreadaEn(ts.toLocalDateTime());
        }
        return a;
    }
}