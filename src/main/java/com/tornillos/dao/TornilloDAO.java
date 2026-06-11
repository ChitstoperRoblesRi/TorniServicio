package com.tornillos.dao;

import com.tornillos.config.DatabaseConfig;
import com.tornillos.model.Tornillo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TornilloDAO {

    private static final String BASE_QUERY =
        "SELECT t.* FROM tornillos t ";

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
        StringBuilder sb = new StringBuilder(BASE_QUERY + "WHERE t.activo=true ");
        List<Object> params = new ArrayList<>();

        if (termino != null && !termino.isBlank()) {
            sb.append("AND (LOWER(t.nombre) LIKE ? OR LOWER(t.codigo) LIKE ?) ");
            String like = "%" + termino.toLowerCase() + "%";
            params.add(like); params.add(like);
        }
        if (estadoStock != null) {
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

    public boolean crear(Tornillo tornillo) throws SQLException {
        String sql = "INSERT INTO tornillos (codigo, nombre, descripcion, " +
            "material, diametro_mm, longitud_mm, paso_rosca, cabeza_tipo, unidad_medida, " +
            "precio_costo, precio_venta, stock_actual, stock_minimo, stock_maximo, ubicacion, activo) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,true)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, tornillo.getCodigo());
            ps.setString(2, tornillo.getNombre());
            ps.setString(3, tornillo.getDescripcion());
            ps.setString(4, tornillo.getMaterial());
            if (tornillo.getDiametroMm() != null) ps.setBigDecimal(5, tornillo.getDiametroMm()); else ps.setNull(5, Types.DECIMAL);
            if (tornillo.getLongitudMm() != null) ps.setBigDecimal(6, tornillo.getLongitudMm()); else ps.setNull(6, Types.DECIMAL);
            if (tornillo.getPasoRosca() != null) ps.setBigDecimal(7, tornillo.getPasoRosca()); else ps.setNull(7, Types.DECIMAL);
            ps.setString(8, tornillo.getCabezaTipo());
            ps.setString(9, tornillo.getUnidadMedida() != null ? tornillo.getUnidadMedida() : "PZA");
            ps.setBigDecimal(10, tornillo.getPrecioCosto());
            ps.setBigDecimal(11, tornillo.getPrecioVenta());
            ps.setInt(12, tornillo.getStockActual());
            ps.setInt(13, tornillo.getStockMinimo());
            ps.setInt(14, tornillo.getStockMaximo());
            ps.setString(15, tornillo.getUbicacion());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean actualizar(Tornillo tornillo) throws SQLException {
        String sql = "UPDATE tornillos SET codigo=?, nombre=?, descripcion=?, " +
            "material=?, diametro_mm=?, longitud_mm=?, paso_rosca=?, cabeza_tipo=?, unidad_medida=?, " +
            "precio_costo=?, precio_venta=?, stock_minimo=?, stock_maximo=?, ubicacion=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, tornillo.getCodigo()); ps.setString(2, tornillo.getNombre()); ps.setString(3, tornillo.getDescripcion());
            ps.setString(4, tornillo.getMaterial());
            if (tornillo.getDiametroMm() != null) ps.setBigDecimal(5, tornillo.getDiametroMm()); else ps.setNull(5, Types.DECIMAL);
            if (tornillo.getLongitudMm() != null) ps.setBigDecimal(6, tornillo.getLongitudMm()); else ps.setNull(6, Types.DECIMAL);
            if (tornillo.getPasoRosca() != null) ps.setBigDecimal(7, tornillo.getPasoRosca()); else ps.setNull(7, Types.DECIMAL);
            ps.setString(8, tornillo.getCabezaTipo()); ps.setString(9, tornillo.getUnidadMedida());
            ps.setBigDecimal(10, tornillo.getPrecioCosto()); ps.setBigDecimal(11, tornillo.getPrecioVenta());
            ps.setInt(12, tornillo.getStockMinimo()); ps.setInt(13, tornillo.getStockMaximo());
            ps.setString(14, tornillo.getUbicacion()); ps.setInt(15, tornillo.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean actualizarStock(int tornilloId, int nuevoStock) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement("UPDATE tornillos SET stock_actual=? WHERE id=?")) {
            ps.setInt(1, nuevoStock); ps.setInt(2, tornilloId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement("UPDATE tornillos SET activo=false WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

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

    private Tornillo mapear(ResultSet rs) throws SQLException {
        Tornillo tornillo = new Tornillo();
        tornillo.setId(rs.getInt("id"));
        tornillo.setCodigo(rs.getString("codigo"));
        tornillo.setNombre(rs.getString("nombre"));
        tornillo.setDescripcion(rs.getString("descripcion"));
        tornillo.setMaterial(rs.getString("material"));
        tornillo.setDiametroMm(rs.getBigDecimal("diametro_mm"));
        tornillo.setLongitudMm(rs.getBigDecimal("longitud_mm"));
        tornillo.setPasoRosca(rs.getBigDecimal("paso_rosca"));
        tornillo.setCabezaTipo(rs.getString("cabeza_tipo"));
        tornillo.setUnidadMedida(rs.getString("unidad_medida"));
        tornillo.setPrecioCosto(rs.getBigDecimal("precio_costo"));
        tornillo.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        tornillo.setStockActual(rs.getInt("stock_actual"));
        tornillo.setStockMinimo(rs.getInt("stock_minimo"));
        tornillo.setStockMaximo(rs.getInt("stock_maximo"));
        tornillo.setUbicacion(rs.getString("ubicacion"));
        tornillo.setActivo(rs.getBoolean("activo"));
        Timestamp creadoEnTs = rs.getTimestamp("creado_en");
        if (creadoEnTs != null) tornillo.setCreadoEn(creadoEnTs.toLocalDateTime());
        Timestamp actualizadoEnTs = rs.getTimestamp("actualizado_en");
        if (actualizadoEnTs != null) tornillo.setActualizadoEn(actualizadoEnTs.toLocalDateTime());
        return tornillo;
    }

}
