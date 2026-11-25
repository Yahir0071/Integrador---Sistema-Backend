-- ============================================================================
-- 05 – Conciliación FK pedidos_web ↔ usuarios
-- ============================================================================
-- Relación existente en el esquema (01-esquema-inicial.sql, línea ~1269):
--
--   ALTER TABLE ONLY public.pedidos_web
--       ADD CONSTRAINT pedidos_web_cliente_id_fkey
--       FOREIGN KEY (cliente_id) REFERENCES public.usuarios(id);
--
-- El modelo JPA (Pedido.java) mapea:
--   @ManyToOne @JoinColumn(name = "cliente_id") private Usuario cliente;
--
-- Los clientes se registran en la tabla `usuarios` con rol_id = 3 (CLIENTE)
-- a través de RegistroClienteService.
--
-- ⚠  La rama `feature/segregacion-clientes-empleados` propone una tabla
--    `clientes` separada. NO se debe fusionar esa rama: la FK apunta
--    correctamente a `usuarios` y el flujo de registro ya funciona así.
--
-- Este script es idempotente: solo verifica que la FK exista; no modifica datos.
-- ============================================================================

BEGIN;

DO $$
BEGIN
    -- Verificar que la FK pedidos_web_cliente_id_fkey existe y apunta a usuarios
    IF NOT EXISTS (
        SELECT 1
        FROM   information_schema.table_constraints  tc
        JOIN   information_schema.constraint_column_usage ccu
               ON ccu.constraint_name = tc.constraint_name
              AND ccu.constraint_schema = tc.constraint_schema
        WHERE  tc.constraint_type = 'FOREIGN KEY'
          AND  tc.table_name      = 'pedidos_web'
          AND  tc.constraint_name = 'pedidos_web_cliente_id_fkey'
          AND  ccu.table_name     = 'usuarios'
    ) THEN
        RAISE EXCEPTION
            'FK pedidos_web_cliente_id_fkey → usuarios NO encontrada. '
            'Revise el esquema antes de continuar.';
    END IF;

    RAISE NOTICE 'OK – pedidos_web.cliente_id → usuarios.id verificado.';
END $$;

COMMIT;
