package com.tornillos.dao;

import com.tornillos.config.DatabaseConfig;
import com.tornillos.model.Tornillo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TornilloDAO {

    private static final String BASE_QUERY = "SELECT t.* FROM tornillos t ";

    public List<Tornillo> listarTodos() throws SQLException {
        List<Tornillo> lista = new ArrayList<>();
        String sql = BASE_QUERY + "WHERE t.activo=true ORDER BY t.nombre";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public List<Tornillo> buscar(String termino) throws SQLException {
        List<Tornillo> lista = new ArrayList<>();
        String sql = BASE_QUERY +
            "WHERE t.activo=true AND (LOWER(t.nombre) LIKE ? OR LOWER(t.codigo) LIKE ? " +
            "OR LOWER(t.material) LIKE ? OR LOWER(t.descripcion) LIKE ?) ORDER BY t.nombre";
        String like = "%" + termino.toLowerCase() + "%";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) ps.setString(i, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public List<Tornillo> listarConFiltro(String termino, String estadoStock) throws SQLException {
        List<Tornillo> lista = new ArrayList<>();
        StringBuilder sb = new StringBuilder(BASE_QUERY);
        List<Object> params = new ArrayList<>();

        if ("INACTIVO".equals(estadoStock)) {
            sb.append("WHERE t.activo=false ");
        } else {
            sb.append("WHERE t.activo=true ");
        }

        if (termino != null && !termino.isBlank()) {
            sb.append("AND (LOWER(t.nombre) LIKE ? OR LOWER(t.codigo) LIKE ?) ");
            String like = "%" + termino.toLowerCase() + "%";
            params.add(like); params.add(like);
        }
        
        if (estadoStock != null && !"INACTIVO".equals(estadoStock)) {
            switch (estadoStock) {
                case "BAJO": sb.append("AND t.stock_actual <= t.stock_minimo AND t.stock_actual > t.stock_minimo/2 "); break;
                case "CRÍTICO": sb.append("AND t.stock_actual <= t.stock_minimo/2 AND t.stock_actual > 0 "); break;
                case "SIN_STOCK": sb.append("AND t.stock_actual = 0 "); break;
                case "NORMAL": sb.append("AND t.stock_actual > t.stock_minimo "); break;
            }
        }
        sb.append("ORDER BY t.nombre");

        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++)
                ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public List<Tornillo> listarConStockBajo() throws SQLException {
        List<Tornillo> lista = new ArrayList<>();
        String sql = BASE_QUERY + "WHERE t.activo=true AND t.stock_actual <= t.stock_minimo ORDER BY t.stock_actual ASC";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Tornillo obtenerPorId(int id) throws SQLException {
        String sql = BASE_QUERY + "WHERE t.id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public void crear(Tornillo t) throws SQLException {
        String sql = "INSERT INTO tornillos (codigo, nombre, descripcion, categoria_id, material, sistema_medida, " +
            "diametro_mm, longitud_mm, paso_rosca, cabeza_tipo, unidad_medida, " +
            "precio_costo, precio_venta, stock_actual, stock_minimo, stock_maximo, ubicacion, activo) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true)";
            
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, t.getCodigo());
            ps.setString(2, t.getNombre());
            ps.setString(3, t.getDescripcion());
            
            if (t.getCategoriaId() <= 0) ps.setNull(4, java.sql.Types.INTEGER);
            else ps.setInt(4, t.getCategoriaId());
            
            ps.setString(5, t.getMaterial());
            ps.setString(6, t.getSistemaMedida() != null ? t.getSistemaMedida() : "METRICO");
            
            if (t.getDiametroMm() != null) ps.setBigDecimal(7, t.getDiametroMm()); else ps.setNull(7, Types.DECIMAL);
            if (t.getLongitudMm() != null) ps.setBigDecimal(8, t.getLongitudMm()); else ps.setNull(8, Types.DECIMAL);
            if (t.getPasoRosca() != null)  ps.setBigDecimal(9, t.getPasoRosca());  else ps.setNull(9, Types.DECIMAL);
            
            ps.setString(10, t.getCabezaTipo());
            ps.setString(11, t.getUnidadMedida() != null ? t.getUnidadMedida() : "PZA");
            ps.setBigDecimal(12, t.getPrecioCosto());
            ps.setBigDecimal(13, t.getPrecioVenta());
            ps.setInt(14, t.getStockActual());
            ps.setInt(15, t.getStockMinimo());
            ps.setInt(16, t.getStockMaximo());
            ps.setString(17, t.getUbicacion());
            
            ps.executeUpdate();
        }
    }

    public void actualizar(Tornillo t) throws SQLException {
        String sql = "UPDATE tornillos SET codigo=?, nombre=?, descripcion=?, categoria_id=?, material=?, sistema_medida=?, " +
            "diametro_mm=?, longitud_mm=?, paso_rosca=?, cabeza_tipo=?, unidad_medida=?, " +
            "precio_costo=?, precio_venta=?, stock_minimo=?, stock_maximo=?, ubicacion=?, activo=? WHERE id=?";
            
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, t.getCodigo()); 
            ps.setString(2, t.getNombre()); 
            ps.setString(3, t.getDescripcion());
            
            if (t.getCategoriaId() <= 0) ps.setNull(4, java.sql.Types.INTEGER);
            else ps.setInt(4, t.getCategoriaId());
            
            ps.setString(5, t.getMaterial());
            ps.setString(6, t.getSistemaMedida() != null ? t.getSistemaMedida() : "METRICO");
            
            if (t.getDiametroMm() != null) ps.setBigDecimal(7, t.getDiametroMm()); else ps.setNull(7, Types.DECIMAL);
            if (t.getLongitudMm() != null) ps.setBigDecimal(8, t.getLongitudMm()); else ps.setNull(8, Types.DECIMAL);
            if (t.getPasoRosca() != null)  ps.setBigDecimal(9, t.getPasoRosca());  else ps.setNull(9, Types.DECIMAL);
            
            ps.setString(10, t.getCabezaTipo()); 
            ps.setString(11, t.getUnidadMedida());
            ps.setBigDecimal(12, t.getPrecioCosto()); 
            ps.setBigDecimal(13, t.getPrecioVenta());
            ps.setInt(14, t.getStockMinimo()); 
            ps.setInt(15, t.getStockMaximo());
            ps.setString(16, t.getUbicacion()); 
            ps.setBoolean(17, t.isActivo()); 
            ps.setInt(18, t.getId());
            
            ps.executeUpdate();
        }
    }

    public void actualizarStock(int tornilloId, int nuevoStock) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement("UPDATE tornillos SET stock_actual=? WHERE id=?")) {
            ps.setInt(1, nuevoStock); ps.setInt(2, tornilloId);
            ps.executeUpdate();
        }
    }

    /*public void eliminar(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement("DELETE FROM tornillos WHERE id=?")) { 
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    } */

    public int contarTotal() throws SQLException {
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM tornillos WHERE activo=true")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int contarStockBajo() throws SQLException {
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT COUNT(*) FROM tornillos WHERE activo=true AND stock_actual <= stock_minimo")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public boolean existeCodigo(String codigo, int idExcluir) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tornillos WHERE codigo = ? AND id != ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public void darDeBaja(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement("UPDATE tornillos SET activo=false WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void reactivar(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement("UPDATE tornillos SET activo=true WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Tornillo> listarConStockDisponible() throws SQLException {
        List<Tornillo> lista = new ArrayList<>();
        String sql = "SELECT t.* FROM tornillos t WHERE t.activo = true AND t.stock_actual > 0 ORDER BY t.nombre";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    private Tornillo mapear(ResultSet rs) throws SQLException {
        Tornillo t = new Tornillo();
        t.setId(rs.getInt("id"));
        t.setCodigo(rs.getString("codigo"));
        t.setNombre(rs.getString("nombre"));
        t.setDescripcion(rs.getString("descripcion"));
        t.setCategoriaId(rs.getInt("categoria_id"));
        t.setMaterial(rs.getString("material"));
        t.setSistemaMedida(rs.getString("sistema_medida"));
        t.setDiametroMm(rs.getBigDecimal("diametro_mm"));
        t.setLongitudMm(rs.getBigDecimal("longitud_mm"));
        t.setPasoRosca(rs.getBigDecimal("paso_rosca"));
        t.setCabezaTipo(rs.getString("cabeza_tipo"));
        t.setUnidadMedida(rs.getString("unidad_medida"));
        t.setPrecioCosto(rs.getBigDecimal("precio_costo"));
        t.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        t.setStockActual(rs.getInt("stock_actual"));
        t.setStockMinimo(rs.getInt("stock_minimo"));
        t.setStockMaximo(rs.getInt("stock_maximo"));
        t.setUbicacion(rs.getString("ubicacion"));
        t.setActivo(rs.getBoolean("activo"));
        Timestamp c = rs.getTimestamp("creado_en");
        if (c != null) t.setCreadoEn(c.toLocalDateTime());
        Timestamp a = rs.getTimestamp("actualizado_en");
        if (a != null) t.setActualizadoEn(a.toLocalDateTime());
        return t;
    }
}