package com.tornillos.service;

import com.tornillos.dao.EntradaDAO;
import com.tornillos.dao.TornilloDAO;
import com.tornillos.model.Entrada;
import com.tornillos.model.Tornillo;

import java.sql.SQLException;
import java.util.List;

public class EntradaService {

    private final EntradaDAO entradaDAO = new EntradaDAO();
    private final TornilloDAO tornilloDAO = new TornilloDAO();
    private final AlertaService alertaService = new AlertaService();

    /**
     * Intermedia el filtrado y búsqueda de entradas en la base de datos.
     */
    public List<Entrada> buscarEntradas(String termino, String desde, String hasta) throws SQLException {
        String t = (termino != null) ? termino.trim() : "";
        String d = (desde != null && !desde.trim().isEmpty()) ? desde.trim() : null;
        String h = (hasta != null && !hasta.trim().isEmpty()) ? hasta.trim() : null;
        return entradaDAO.buscar(t, d, h);
    }

    /**
     * Registra una nueva entrada en el sistema, actualiza el stock del producto
     * y ejecuta el disparador automático de verificación de alertas.
     */
    public void registrarEntrada(Entrada entrada) throws SQLException {
        // Ejecuta la transacción SQL empaquetada en el DAO
        entradaDAO.registrar(entrada);
        
        // Ejecución síncrona post-registro del motor de alertas
        alertaService.verificarAlertas();
    }

    /**
     * Elimina una entrada revirtiendo el stock, validando reglas de negocio previas 
     * y reevaluando el estado de alertas en el sistema.
     */
    public void eliminarEntrada(int id) throws SQLException {
        // Ejecuta la reversión transaccional segura
        entradaDAO.eliminar(id);
        
        // Reevalúa el estado de alertas de inventario
        alertaService.verificarAlertas();
    }

    /**
     * Recupera el catálogo completo de tornillos activos para poblar el combo predictivo.
     */
    public List<Tornillo> obtenerCatalogoTornillos() throws SQLException {
        return tornilloDAO.listarTodos();
    }
}