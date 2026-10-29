-- ============================================================================
-- SCRIPT: HABILITAR RLS (ROW LEVEL SECURITY) EN TABLAS SEGREGADAS
-- Proyecto: Sistema Comercial Nova (Grupo06 / Supabase)
-- Motivo: Bloquear la exposición pública de PostgREST y resolver alertas de Supabase
-- ============================================================================

ALTER TABLE public.clientes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.empleados ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mermas ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.presentacion_producto ENABLE ROW LEVEL SECURITY;
