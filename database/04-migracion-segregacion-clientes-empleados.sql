-- ============================================================================
-- SCRIPT DE MIGRACIÓN: SEGREGACIÓN DE CLIENTES Y EMPLEADOS
-- Proyecto: Sistema Comercial Nova (Grupo06 / Grupo06-Ecommerce)
-- Base de Datos: PostgreSQL en Supabase
-- Fecha: 2026-10-28
-- ============================================================================

BEGIN;

-- 1. Crear tabla de clientes (E-Commerce)
CREATE TABLE IF NOT EXISTS public.clientes (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(150) NOT NULL,
    nombre_completo VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    telefono VARCHAR(15) NOT NULL,
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. Crear tabla de empleados (POS / ERP / Staff)
CREATE TABLE IF NOT EXISTS public.empleados (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(150) NOT NULL,
    nombre_completo VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    telefono VARCHAR(15),
    rol VARCHAR(20) NOT NULL CHECK (rol IN ('ADMINISTRADOR', 'VENDEDOR')),
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 3. Migrar Clientes desde usuarios
INSERT INTO public.clientes (id, username, password, nombre_completo, email, telefono, activo)
SELECT u.id, u.username, u.password, u.nombre_completo, 
       COALESCE(u.email, concat(u.username, '@cliente.com')), 
       COALESCE(u.telefono, '999999999'), 
       u.activo
FROM public.usuarios u
JOIN public.roles r ON u.rol_id = r.id
WHERE r.nombre = 'CLIENTE'
ON CONFLICT (id) DO NOTHING;

-- Sincronizar secuencia de clientes
SELECT setval('public.clientes_id_seq', COALESCE((SELECT MAX(id) FROM public.clientes), 1));

-- 4. Migrar Empleados (ADMINISTRADOR y VENDEDOR) desde usuarios
INSERT INTO public.empleados (id, username, password, nombre_completo, email, telefono, rol, activo)
SELECT u.id, u.username, u.password, u.nombre_completo, u.email, u.telefono, r.nombre, u.activo
FROM public.usuarios u
JOIN public.roles r ON u.rol_id = r.id
WHERE r.nombre IN ('ADMINISTRADOR', 'VENDEDOR')
ON CONFLICT (id) DO NOTHING;

-- Sincronizar secuencia de empleados
SELECT setval('public.empleados_id_seq', COALESCE((SELECT MAX(id) FROM public.empleados), 1));

-- 5. Actualizar Foreign Keys en las tablas del sistema

-- A. Pedidos Web (cliente_id -> clientes.id)
ALTER TABLE public.pedidos_web DROP CONSTRAINT IF EXISTS pedidos_web_cliente_id_fkey;
ALTER TABLE public.pedidos_web DROP CONSTRAINT IF EXISTS fk_pedidos_cliente;
ALTER TABLE public.pedidos_web ADD CONSTRAINT fk_pedidos_cliente FOREIGN KEY (cliente_id) REFERENCES public.clientes(id);

-- B. Ventas (usuario_id -> empleados.id)
ALTER TABLE public.ventas DROP CONSTRAINT IF EXISTS fkco9r9xjcdqtgd4nvnnolsr6ei;
ALTER TABLE public.ventas DROP CONSTRAINT IF EXISTS fk_ventas_usuario;
ALTER TABLE public.ventas DROP CONSTRAINT IF EXISTS fk_ventas_empleado;
ALTER TABLE public.ventas ADD CONSTRAINT fk_ventas_empleado FOREIGN KEY (usuario_id) REFERENCES public.empleados(id);

-- C. Compras (usuario_id -> empleados.id)
ALTER TABLE public.compras DROP CONSTRAINT IF EXISTS fk4lnkw9964v2gf3b2sjbaealqw;
ALTER TABLE public.compras DROP CONSTRAINT IF EXISTS fk_compras_empleado;
ALTER TABLE public.compras ADD CONSTRAINT fk_compras_empleado FOREIGN KEY (usuario_id) REFERENCES public.empleados(id);

-- D. Mermas (usuario_id -> empleados.id)
ALTER TABLE public.mermas DROP CONSTRAINT IF EXISTS fkns8xuyalff7s4yb9pmr59f9sy;
ALTER TABLE public.mermas DROP CONSTRAINT IF EXISTS fk_mermas_empleado;
ALTER TABLE public.mermas ADD CONSTRAINT fk_mermas_empleado FOREIGN KEY (usuario_id) REFERENCES public.empleados(id);

-- E. Movimientos de Inventario (usuario_id -> empleados.id)
ALTER TABLE public.movimientos_inventario DROP CONSTRAINT IF EXISTS fkakkxu577al9vljuur2ahk0lo;
ALTER TABLE public.movimientos_inventario DROP CONSTRAINT IF EXISTS fk_movimientos_empleado;
ALTER TABLE public.movimientos_inventario ADD CONSTRAINT fk_movimientos_empleado FOREIGN KEY (usuario_id) REFERENCES public.empleados(id);

-- F. Eventos de Auditoría de Pedidos (actor_id puede ser cliente o empleado)
ALTER TABLE public.eventos_pedido_web DROP CONSTRAINT IF EXISTS eventos_pedido_web_actor_id_fkey;

COMMIT;
