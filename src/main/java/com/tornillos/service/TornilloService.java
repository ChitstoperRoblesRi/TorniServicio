package com.tornillos.service;

import com.tornillos.dao.TornilloDAO;
import com.tornillos.dao.CategoriaDAO;
import com.tornillos.model.Tornillo;
import com.tornillos.model.Categoria;

import java.sql.SQLException;
import java.util.List;

public class TornilloService {

    private final TornilloDAO tornilloDAO = new TornilloDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    /**
     * Recupera el listado completo de tornillos activos en el sistema.
     */
    public List<Tornillo> obtenerTodosLosTornillos() throws SQLException {
        return tornilloDAO.listarTodos();
    }

    /**
     * Filtra el catálogo maestro combinando términos de búsqueda y estados de criticidad de stock.
     */
    public List<Tornillo> buscarConFiltros(String termino, String estadoStock) throws SQLException {
        return tornilloDAO.listarConFiltro(termino, estadoStock);
    }

    /**
     * Valida la integridad del código del producto y registra el tornillo en la base de datos.
     */
    public void guardarTornillo(Tornillo t) throws SQLException {
        int idExcluir = (t.getId() <= 0) ? -1 : t.getId();
        if (tornilloDAO.existeCodigo(t.getCodigo(), idExcluir)) {
            throw new IllegalArgumentException("El código \"" + t.getCodigo() + "\" ya está asignado a otro tornillo.");
        }

        if (t.getId() <= 0) {
            tornilloDAO.crear(t);
        } else {
            tornilloDAO.actualizar(t);
        }
    }

    /**
     * Cambia el estado del tornillo a inactivo (borrado lógico).
     */
    public void darDeBajaTornillo(int id) throws SQLException {
        tornilloDAO.darDeBaja(id);
    }

    /**
     * Reactiva un producto que había sido dado de baja previamente.
     */
    public void reactivarTornillo(int id) throws SQLException {
        tornilloDAO.reactivar(id);
    }

    /**
     * Recupera todas las categorías registradas para poblar los componentes de UI.
     */
    public List<Categoria> obtenerTodasLasCategorias() throws SQLException {
        return categoriaDAO.listarTodas();
    }

    /**
     * Verifica de forma aislada si un código ya pertenece a otro registro.
     */
    public boolean existeCodigoProducto(String codigo, int idExcluir) throws SQLException {
        return tornilloDAO.existeCodigo(codigo, idExcluir);
    }
}