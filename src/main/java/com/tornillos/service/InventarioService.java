package com.tornillos.service;

import com.tornillos.dao.TornilloDAO;
import com.tornillos.dao.MovimientoInventarioDAO;
import com.tornillos.model.Tornillo;
import com.tornillos.model.MovimientoInventario;

import java.sql.SQLException;
import java.util.List;

public class InventarioService {

    private final TornilloDAO tornilloDAO = new TornilloDAO();
    private final MovimientoInventarioDAO movimientoDAO = new MovimientoInventarioDAO();
    private final AlertaService alertaService = new AlertaService();

    /**
     * Consulta un tornillo específico en la base de datos mediante su ID único.
     */
    public Tornillo obtenerTornilloPorId(int id) throws SQLException {
        return tornilloDAO.obtenerPorId(id);
    }

    /**
     * Filtra el catálogo maestro combinando términos de búsqueda y estados de criticidad de stock.
     */
    public List<Tornillo> buscarTornillos(String termino, String estadoStock) throws SQLException {
        return tornilloDAO.listarConFiltro(termino, estadoStock);
    }

    /**
     * Realiza la baja lógica (desactivación) de un tornillo y actualiza las alertas.
     */
    public void darDeBajaTornillo(int id) throws SQLException {
        tornilloDAO.darDeBaja(id);
        alertaService.verificarAlertas();
    }

    /**
     * Reactiva la disponibilidad de un producto en el catálogo.
     */
    public void reactivarTornillo(int id) throws SQLException {
        tornilloDAO.reactivar(id);
        alertaService.verificarAlertas();
    }

    /**
     * Elimina físicamente un tornillo del inventario central.
    public void eliminarTornilloPermanente(int id) throws SQLException {
        tornilloDAO.eliminar(id);
        alertaService.verificarAlertas();
    }*/

    /**
     * Consulta el Kardex histórico de auditoría relacional con funciones de ventana de PostgreSQL.
     */
    public List<MovimientoInventario> obtenerKardexMovimientos(String tipo, String desde, String hasta) throws SQLException {
        return movimientoDAO.listar(tipo, desde, hasta);
    }

    /**
     * Disparador de sincronización manual del motor de alertas por correo.
     */
    public void forzarVerificacionAlertas() {
        alertaService.verificarAlertas();
    }
}