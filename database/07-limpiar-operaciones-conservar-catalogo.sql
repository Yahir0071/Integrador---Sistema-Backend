-- ============================================================================
-- SCRIPT: REINICIO LIMPIO DE OPERACIONES CONSERVANDO CATÁLOGO DE PRODUCTOS
-- - Conserva intactos: categorias, productos (con imágenes), presentacion_producto,
--   proveedores y roles.
-- - Reinicia todos los IDs (secuencias desde 1) de pedidos, ventas, compras,
--   mermas, movimientos, alertas, clientes y empleados.
-- - Libera cualquier stock_reservado pendiente en productos.
-- ============================================================================

BEGIN;

TRUNCATE TABLE
    public.eventos_pedido_web,
    public.comprobantes_pedido_web,
    public.lineas_pedido_web,
    public.pedidos_web,
    public.pagos,
    public.detalles_venta,
    public.ventas,
    public.detalles_compra,
    public.compras,
    public.mermas,
    public.movimientos_inventario,
    public.alertas_reposicion,
    public.clientes,
    public.empleados
RESTART IDENTITY CASCADE;

-- Asegurar que ningún producto quede con stock reservado de pedidos borrados
UPDATE public.productos
SET stock_reservado = 0;

-- Corregir categoría Lácteos si aún faltaba (L02)
UPDATE public.categorias
SET nombre = 'Lácteos'
WHERE nombre = 'Lacteos';

-- ============================================================================
-- OPCIONAL: Insertar 1 ADMINISTRADOR inicial para entrar al Escritorio y crear
-- ahí a los otros 3 integrantes (la app encripta automáticamente las contraseñas).
-- Contraseña de ejemplo del hash BCrypt abajo: "Admin2026!"
-- ============================================================================
INSERT INTO public.empleados (username, password, nombre_completo, email, telefono, rol, activo, creado_en)
VALUES (
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Administrador Principal',
    'admin@comercialnova.pe',
    '999000001',
    'ADMINISTRADOR',
    true,
    CURRENT_TIMESTAMP
);

COMMIT;
