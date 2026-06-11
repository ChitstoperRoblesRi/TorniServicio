-- ============================================================
-- SISTEMA DE INVENTARIO DE TORNILLOS - ESQUEMA PostgreSQL
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Roles
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion TEXT
);

INSERT INTO roles (nombre, descripcion) VALUES
    ('GERENTE', 'Acceso total al sistema'),
    ('EMPLEADO', 'Acceso operativo básico')
ON CONFLICT DO NOTHING;

-- Usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol_id INT NOT NULL REFERENCES roles(id),
    activo BOOLEAN DEFAULT TRUE,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultima_sesion TIMESTAMP
);

-- Categorías de tornillos
CREATE TABLE IF NOT EXISTS categorias (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT
);

INSERT INTO categorias (nombre, descripcion) VALUES
    ('Tornillo Métrico', 'Tornillos con rosca métrica estándar'),
    ('Tornillo Autorroscante', 'Tornillos que forman su propia rosca'),
    ('Tornillo de Madera', 'Tornillos para madera'),
    ('Tornillo de Máquina', 'Tornillos para ensamblaje mecánico'),
    ('Tornillo de Cabeza Hexagonal', 'Tornillos con cabeza hexagonal'),
    ('Tornillo Phillips', 'Tornillos con ranura Phillips')
ON CONFLICT DO NOTHING;

-- Proveedores
CREATE TABLE IF NOT EXISTS proveedores (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    contacto VARCHAR(100),
    telefono VARCHAR(20),
    email VARCHAR(150),
    direccion TEXT,
    activo BOOLEAN DEFAULT TRUE,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tornillos (inventario)
CREATE TABLE IF NOT EXISTS tornillos (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    categoria_id INT REFERENCES categorias(id),
    proveedor_id INT REFERENCES proveedores(id),
    material VARCHAR(100),
    sistema_medida VARCHAR(10) DEFAULT 'METRICO', -- <-- NUEVA COLUMNA
    diametro_mm DECIMAL(8,2),
    longitud_mm DECIMAL(8,2),
    paso_rosca DECIMAL(6,3),
    cabeza_tipo VARCHAR(50),
    unidad_medida VARCHAR(20) DEFAULT 'PZA',
    precio_costo DECIMAL(12,2) DEFAULT 0,
    precio_venta DECIMAL(12,2) DEFAULT 0,
    stock_actual INT DEFAULT 0,
    stock_minimo INT DEFAULT 10,
    stock_maximo INT DEFAULT 1000,
    ubicacion VARCHAR(100),
    activo BOOLEAN DEFAULT TRUE,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Entradas de inventario
CREATE TABLE IF NOT EXISTS entradas (
    id SERIAL PRIMARY KEY,
    folio VARCHAR(50) NOT NULL UNIQUE,
    tornillo_id INT NOT NULL REFERENCES tornillos(id),
    usuario_id INT NOT NULL REFERENCES usuarios(id),
    proveedor_id INT REFERENCES proveedores(id),
    cantidad INT NOT NULL CHECK (cantidad > 0),
    precio_unitario DECIMAL(12,2) DEFAULT 0,
    total DECIMAL(14,2) DEFAULT 0,
    numero_factura VARCHAR(100),
    observaciones TEXT,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Salidas de inventario
CREATE TABLE IF NOT EXISTS salidas (
    id SERIAL PRIMARY KEY,
    folio VARCHAR(50) NOT NULL UNIQUE,
    tornillo_id INT NOT NULL REFERENCES tornillos(id),
    usuario_id INT NOT NULL REFERENCES usuarios(id),
    cantidad INT NOT NULL CHECK (cantidad > 0),
    precio_unitario DECIMAL(12,2) DEFAULT 0,
    total DECIMAL(14,2) DEFAULT 0,
    motivo VARCHAR(100),
    cliente VARCHAR(150),
    observaciones TEXT,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Alertas
CREATE TABLE IF NOT EXISTS alertas (
    id SERIAL PRIMARY KEY,
    tornillo_id INT NOT NULL REFERENCES tornillos(id),
    tipo VARCHAR(50) NOT NULL,  -- 'STOCK_BAJO', 'STOCK_CRITICO', 'SIN_STOCK'
    mensaje TEXT NOT NULL,
    leida BOOLEAN DEFAULT FALSE,
    enviada_email BOOLEAN DEFAULT FALSE,
    creada_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    leida_en TIMESTAMP
);

-- Configuración del sistema
CREATE TABLE IF NOT EXISTS configuracion (
    clave VARCHAR(100) PRIMARY KEY,
    valor TEXT,
    descripcion TEXT
);

INSERT INTO configuracion (clave, valor, descripcion) VALUES
    ('empresa_nombre', 'TornillosMax S.A. de C.V.', 'Nombre de la empresa'),
    ('empresa_rfc', 'TMX010101ABC', 'RFC de la empresa'),
    ('empresa_email', 'contacto@tornillosmax.com', 'Email corporativo'),
    ('smtp_host', 'smtp.gmail.com', 'Servidor SMTP'),
    ('smtp_port', '587', 'Puerto SMTP'),
    ('smtp_user', '', 'Usuario SMTP'),
    ('smtp_password', '', 'Contraseña SMTP'),
    ('alertas_email_activo', 'false', 'Enviar alertas por email'),
    ('alertas_email_destino', '', 'Email destino para alertas')
ON CONFLICT DO NOTHING;

-- Índices
CREATE INDEX IF NOT EXISTS idx_tornillos_codigo ON tornillos(codigo);
CREATE INDEX IF NOT EXISTS idx_tornillos_nombre ON tornillos(nombre);
CREATE INDEX IF NOT EXISTS idx_tornillos_categoria ON tornillos(categoria_id);
CREATE INDEX IF NOT EXISTS idx_tornillos_stock ON tornillos(stock_actual);
CREATE INDEX IF NOT EXISTS idx_entradas_fecha ON entradas(fecha);
CREATE INDEX IF NOT EXISTS idx_salidas_fecha ON salidas(fecha);
CREATE INDEX IF NOT EXISTS idx_alertas_leida ON alertas(leida);

-- Función para actualizar timestamp
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tornillos_updated
    BEFORE UPDATE ON tornillos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- Usuario gerente por defecto (password: Admin123!)
INSERT INTO usuarios (nombre, apellido, email, username, password_hash, rol_id)
VALUES ('Administrador', 'Sistema', 'admin@tornillosmax.com', 'admin',
        crypt('Admin123!', gen_salt('bf')), 1)
ON CONFLICT DO NOTHING;

-- Datos de muestra
INSERT INTO proveedores (nombre, contacto, telefono, email) VALUES
    ('Tornillos Industriales del Norte', 'Juan García', '555-0001', 'ventas@tinorte.com'),
    ('MetalFix Distribuciones', 'María López', '555-0002', 'info@metalfix.com'),
    ('FastenerPro México', 'Carlos Ruiz', '555-0003', 'cp@fastenerpro.mx')
ON CONFLICT DO NOTHING;
