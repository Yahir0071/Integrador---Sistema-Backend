-- ============================================================================
-- SCRIPT DE CONCILIACIÓN: CLIENTES PENDIENTES Y CATEGORÍA LÁCTEOS (C01 / L02)
-- Seguro e idempotente: no borra registros ni altera IDs existentes.
-- ============================================================================

BEGIN;

-- 1. Sincronizar cualquier cliente registrado en usuarios que falte en clientes
INSERT INTO public.clientes (id, username, password, nombre_completo, email, telefono, activo)
SELECT u.id, u.username, u.password, u.nombre_completo,
       COALESCE(u.email, concat(u.username, '@cliente.com')),
       COALESCE(u.telefono, '999999999'),
       u.activo
FROM public.usuarios u
JOIN public.roles r ON u.rol_id = r.id
WHERE r.nombre = 'CLIENTE'
ON CONFLICT (id) DO NOTHING;

SELECT setval('public.clientes_id_seq', COALESCE((SELECT MAX(id) FROM public.clientes), 1));

-- 2. Corregir ortografía de categoría Lácteos sin cambiar IDs (L02)
UPDATE public.categorias
SET nombre = 'Lácteos'
WHERE nombre = 'Lacteos';

COMMIT;
