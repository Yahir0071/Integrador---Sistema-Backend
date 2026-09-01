# Análisis Técnico del Backend — SGCIVORP
### Sistema de Gestión y Control de Inventario y Ventas para la Optimización de la Reposición de Productos

Basado en el Project Charter del Word (RF01–RF09, RNF01–RNF08) y en el código actual (`model`, `repository`, `service`, `service/impl`).

---

## 0. Contexto rápido (del Word)

- Es una **bodega** sin sistema previo: control de stock "a ojo", ventas no registradas individualmente (efectivo + Yape sumados al final del día).
- Alcance fase 1: **aplicación de escritorio en Java**, con Categorías, Proveedores, Productos, Compras, Ventas, Movimientos de inventario, Alertas de reposición, Usuarios/Roles.
- RF clave: RF01 productos, RF02 entradas/salidas, RF03 stock automático, RF04 ventas, RF05 alertas por stock mínimo, RF06 identificar productos a reponer, RF07 usuarios/roles/permisos, RF08 reportes (incluye rotación), RF09 historial de movimientos.
- RNF clave: RNF02 autenticación, RNF03 permisos por rol, RNF05 validación de datos, RNF06 integridad/consistencia.

**Conclusión general:** el diseño de dominio está bien pensado — el flujo `Compra/Venta → MovimientoInventario → AlertaReposicion` es exactamente el flujo que pide el Word (RF02→RF03→RF05→RF06 encadenados automáticamente). Los huecos están en: seguridad de contraseñas, validaciones de entrada, concurrencia sobre el stock, y algunas funciones de negocio que quedaron a medias (anular venta, cerrar alertas resueltas).

---

## 1. Capa MODEL

| Entidad | Estado | Observaciones |
|---|---|---|
| `Rol` / `RolNombre` | ✅ Bien | Enum simple, correcto para RF07/RNF03. |
| `Usuario` | ⚠️ Revisar | `password` es un `String` plano en BD — **no hay hash**. Esto rompe RNF02 en cuanto se implemente login real. |
| `Categoria` | ✅ Bien | Soft-delete con `estado`, consistente. |
| `Proveedor` | ✅ Bien | `ruc` único, soft-delete. |
| `Producto` | ✅ Bien | `stockActual`, `stockMinimo`, relación a Categoria/Proveedor, `unidadMedida`. Cubre RF01/RF05/RF06. |
| `Compra` / `DetalleCompra` | ✅ Bien | Alimenta movimientos de tipo ENTRADA. |
| `Venta` / `DetalleVenta` / `Pago` | ⚠️ Revisar | `estado` es un `String` libre (`"EMITIDA"`/`"ANULADA"`) en vez de un enum — inconsistente con el resto del modelo, que sí usa enums (`TipoMovimiento`, `EstadoAlerta`, `MetodoPago`). Además, `"ANULADA"` **nunca se usa** en ningún Service. |
| `MovimientoInventario` | ✅ Muy bien | Guarda `stockAnterior`/`stockPosterior` — excelente para trazabilidad (RF09). Solo usa `motivo` (texto libre) para referenciar el origen (compra/venta) en vez de una FK opcional. |
| `AlertaReposicion` | ✅ Bien | `cantidadSugerida`, `estado` (PENDIENTE/ATENDIDA/DESCARTADA), cubre RF05/RF06. |

### Hallazgos importantes

1. **Password sin encriptar (crítico).** `Usuario.password` se guarda y compara tal cual. Antes de exponer login hay que introducir `BCryptPasswordEncoder` (Spring Security) al registrar y validar.
2. **Sin Bean Validation.** Ninguna entidad usa `@NotBlank`, `@Email`, `@Positive`, etc. Hoy la única validación es la de la base de datos (`nullable`, `unique`, `length`), lo cual no da mensajes de error claros al usuario. Esto es exactamente lo que pide **RNF05** ("El sistema debe validar los datos ingresados") y hoy no se cumple a nivel de aplicación.
3. **`Venta.estado` como `String` libre.** Debería ser un enum `EstadoVenta { EMITIDA, ANULADA }`, igual que se hizo con `TipoMovimiento` o `EstadoAlerta`. Con `String` libre no hay garantía de que no se guarde `"emitida"`, `"Emitida "`, etc.
4. **Sin control de concurrencia en `Producto.stockActual`.** No hay `@Version` (optimistic locking). Si en el futuro hay varias estaciones de venta contra la misma base (Supabase, multiusuario), dos ventas simultáneas del mismo producto pueden generar una "lost update" del stock. Esto afecta directamente **RNF06 (integridad y consistencia)**.
5. **Sin auditoría (`createdAt`/`updatedAt`).** Sería útil en `Usuario`, `Producto`, `Venta`, `Compra` para trazabilidad y para respaldos (RNF07).
6. Buen acierto: **no usan `@Data`/`@EqualsAndHashCode`** en las entidades (solo `@Getter/@Setter/@ToString`), lo cual evita el clásico bug de `equals`/`hashCode` con colecciones lazy de JPA. Esto está bien hecho, consérvenlo.
7. Buen acierto de dominio: `MetodoPago { EFECTIVO, YAPE, PLIN }` refleja exactamente la realidad descrita en el Word (ventas en efectivo + Yape), y el modelo `Pago` permite **pagos mixtos** por venta (una venta con parte efectivo, parte Yape), algo que el Word menciona implícitamente.

---

## 2. Capa REPOSITORY

| Repository | Estado | Observaciones |
|---|---|---|
| `RolRepository` | ✅ | — |
| `ProveedorRepository` | ✅ | `findByRuc`, `findByEstadoTrue`, búsqueda por razón social. |
| `CategoriaRepository` | ✅ | — |
| `ProductoRepository` | ⚠️ Inconsistente | `findByEstadoTrue`, `findByCategoriaIdAndEstadoTrue` y `findProductosConBajoStock` usan `@EntityGraph(categoria, proveedor)`, pero **`findByNombreContainingIgnoreCaseAndEstadoTrue` no lo tiene** → riesgo de N+1 o `LazyInitializationException` al mostrar resultados de búsqueda fuera de una transacción. |
| `UsuarioRepository` | ⚠️ Falta EntityGraph | `rol` es `LAZY` y ningún finder trae el rol junto con el usuario. Mitigado parcialmente por `default_batch_fetch_size=20` en `application.properties`, pero conviene ser explícito. |
| `VentaRepository` | ✅ | `@EntityGraph(usuario)` consistente en los finders. |
| `CompraRepository` | ✅ | Igual que Venta, consistente. |
| `DetalleVentaRepository` | ✅ | Trae `producto` vía EntityGraph + query de mayor rotación (RF08). |
| `DetalleCompraRepository` | ⚠️ Inconsistente | `findByCompraId` **no** tiene `@EntityGraph(producto)`, a diferencia de su equivalente en ventas. |
| `MovimientoInventarioRepository` | ✅ | Todas las consultas traen `producto` + `usuario`. Muy sólido para RF09. |
| `AlertaReposicionRepository` | ✅ | `@EntityGraph(producto)` en ambos finders. |
| `PagoRepository` | ✅ | — |

### Hallazgos importantes

1. **Estandarizar `@EntityGraph`.** Es el mismo patrón en casi todos lados, pero se olvidaron dos casos (`ProductoRepository.findByNombreContainingIgnoreCaseAndEstadoTrue` y `DetalleCompraRepository.findByCompraId`). Fácil de corregir y evita bugs sutiles en producción.
2. **No hay paginación.** Ningún método usa `Pageable`/`Page<T>`. Hoy con datos de prueba no se nota, pero apenas la bodega acumule meses de ventas y movimientos, `listarVentas()`, `listarPorFechas()` o `listarPorProducto()` van a traer listas completas a memoria. Esto choca contra **RNF01** ("tiempos de respuesta adecuados"). Recomendado antes de pasar a producción con Supabase.
3. Falta un finder por `estado` en `VentaRepository` (para cuando se implemente anulación de ventas).

---

## 3. Capa SERVICE (interfaces)

| Interface | Estado | Observaciones |
|---|---|---|
| `ICategoriaService` | ✅ | CRUD completo. |
| `IProveedorService` | ✅ | CRUD completo. |
| `IProductoService` | ✅ | Incluye `listarConBajoStock` (RF06). |
| `ICompraService` | ✅ | Cubre RF02 vía compras. |
| `IVentaService` | ⚠️ Incompleto | No existe `anularVenta`, aunque el modelo (`Venta.estado`) ya contempla `"ANULADA"`. Es un campo "fantasma": existe en el dato pero ningún caso de uso lo produce. |
| `IAlertaReposicionService` | ✅ | Cubre RF05/RF06. |
| `IMovimientoInventarioService` | ✅ | Cubre RF02/RF03/RF09. |
| `IUsuarioService` | ⚠️ Incompleto | No hay método de autenticación (`login`/`validarCredenciales`). Es esperable si aún no se implementa RNF02, pero hay que planearlo (probablemente como un `IAuthService` aparte que use Spring Security). |

### Falta a nivel de servicio (no de implementación)

- **Reportería:** el Word (RF08) pide "reportes relacionados con las ventas, el inventario y los movimientos de productos". Hoy solo existe `reporteMayorRotacion()` dentro de `IVentaService`. Faltaría algo como un `IReporteService` con, por ejemplo: ventas por período con totales, valorización de inventario (stock × precio), productos sin movimiento reciente, etc.
- **Auditoría/autenticación:** un `IAuthService` (login, posiblemente emitir un token o simplemente validar sesión para la app de escritorio).

---

## 4. Capa SERVICE IMPL

| Impl | Estado | Observaciones |
|---|---|---|
| `CategoriaServiceImpl` | ✅ | Soft-delete correcto. |
| `ProveedorServiceImpl` | ✅ | Soft-delete correcto. |
| `ProductoServiceImpl` | ✅ Muy bien | `registrar()` y `actualizar()` ambos re-disparan `verificarYGenerarAlerta()` — cubre el caso de subir el `stockMinimo` por edición y que eso genere alerta aunque el stock físico no haya cambiado. Buen detalle. |
| `AlertaReposicionServiceImpl` | ⚠️ Revisar | La lógica de generación es sólida (evita duplicar alertas PENDIENTE, calcula `cantidadSugerida` de forma razonable). **Pero falta el camino inverso:** si el stock vuelve a subir por encima del mínimo (por una `REPOSICION` o `AJUSTE`), las alertas `PENDIENTE` existentes **no se cierran automáticamente**. Quedan "vivas" aunque ya no reflejen la realidad, a menos que alguien las atienda manualmente. |
| `MovimientoInventarioServiceImpl` | ⚠️ Revisar | Lógica de ENTRADA/SALIDA/AJUSTE/REPOSICION correcta y valida stock insuficiente en SALIDA. **Riesgo de concurrencia:** lee `stockActual`, lo modifica en memoria y guarda — sin bloqueo (`@Version` u optimistic/pessimistic locking). Con un solo puesto de venta no pasa nada; con Supabase y varias estaciones sí es un riesgo real de "se vendió dos veces el último producto". |
| `CompraServiceImpl` | ✅ | Al estar todo en un único `@Transactional`, si un producto de un detalle no existe, el `RuntimeException` revierte toda la compra (no queda una compra "a medias"). Correcto. Falta validar `cantidad > 0` / `precioUnitario >= 0` antes de procesar (RNF05). |
| `VentaServiceImpl` | ⚠️ Revisar | Buen detalle: valida stock disponible **antes** de descontar, con mensaje amigable incluyendo el nombre del producto. **Falta:** no valida que la suma de `Pago.monto` sea igual a `Venta.total` — se podría guardar una venta con pagos que no cuadran con el total (choca con RNF06, integridad). Tampoco existe `anularVenta()` para revertir stock si se anula una venta ya emitida. |
| `UsuarioServiceImpl` | 🔴 Crítico | `registrar()` valida username único (bien), pero guarda el `password` **sin hashear**. Igual en `actualizar()`. Debe integrarse `BCryptPasswordEncoder` antes de que el sistema quede expuesto a login real. |

### Patrón transversal que se repite

- Todos los `catch`/`throw` son `RuntimeException` genéricas con mensaje de texto. Funciona hoy porque no hay una capa REST/Controller todavía, pero apenas agreguen `@RestController`, van a necesitar excepciones propias (`RecursoNoEncontradoException`, `StockInsuficienteException`, `CredencialesInvalidasException`, etc.) + un `@ControllerAdvice` para mapear cada una a su código HTTP correcto (404, 409, 401...). Vale la pena introducirlo ahora, antes de que la capa web dependa de mensajes de texto para decidir el status.

---

## 5. Matriz de trazabilidad rápida (RF/RNF → código)

| Requisito | Cubierto por | Estado |
|---|---|---|
| RF01 Productos | `Producto`, `ProductoService(Impl)` | ✅ |
| RF02 Entradas/salidas | `MovimientoInventario`, `MovimientoInventarioServiceImpl` | ✅ |
| RF03 Stock automático | `MovimientoInventarioServiceImpl.registrarMovimiento` | ✅ |
| RF04 Ventas | `Venta`, `VentaServiceImpl` | ✅ (falta anulación) |
| RF05 Alertas stock mínimo | `AlertaReposicionServiceImpl` | ✅ (falta auto-cierre) |
| RF06 Identificar reposición | `ProductoRepository.findProductosConBajoStock` | ✅ |
| RF07 Usuarios/roles/permisos | `Usuario`, `Rol` | ⚠️ Falta autenticación real y enforcement de permisos (Spring Security) |
| RF08 Reportes | `reporteMayorRotacion` únicamente | ⚠️ Parcial |
| RF09 Historial de movimientos | `MovimientoInventarioRepository` | ✅ |
| RNF02 Autenticación | — | 🔴 No implementado |
| RNF03 Permisos por rol | — | 🔴 No implementado (Spring Security no está en el proyecto aún) |
| RNF05 Validación de datos | — | 🔴 No hay Bean Validation |
| RNF06 Integridad/consistencia | Soft-delete, transacciones | ⚠️ Falta locking de stock y validación pagos=total |
| RNF07 Backups | — | Pendiente (a nivel de infraestructura Supabase) |

---

## 6. Prioridades sugeridas (orden recomendado de trabajo)

**Crítico (antes de cualquier demo con datos reales):**
1. Hashear contraseñas (`BCryptPasswordEncoder`) en `UsuarioServiceImpl`.
2. Agregar Bean Validation (`spring-boot-starter-validation`) a las entidades/DTOs de entrada.
3. Validar que `sum(pagos.monto) == venta.total` en `VentaServiceImpl`.

**Importante (antes de multiusuario en Supabase):**
4. `@Version` en `Producto` para evitar carreras sobre `stockActual`.
5. Implementar `anularVenta()` (revertir stock + `estado = ANULADA`).
6. Auto-cerrar `AlertaReposicion` cuando el stock vuelve a estar sobre el mínimo.
7. Estandarizar `@EntityGraph` faltantes.

**Cuando construyan la capa web/API:**
8. Excepciones propias + `@ControllerAdvice`.
9. Paginación (`Pageable`) en listados grandes.
10. Módulo de autenticación/autorización (Spring Security + roles ADMINISTRADOR/VENDEDOR).
11. Ampliar reportería (RF08) más allá de mayor rotación.

---

## 7. Base de datos: Supabase

El modelo ya es compatible con esto sin cambios grandes: Supabase es Postgres administrado, así que solo necesitan:

- Cambiar el `driver`/`url` en `application.properties` (o mejor, `application.yml` con perfiles `dev`/`prod`) a la cadena de conexión de Supabase (host, puerto 5432 o el *pooler* 6543, `sslmode=require`).
- Usar el **connection pooler de Supabase (PgBouncer, puerto 6543)** en modo *transaction* si van a tener varias instancias de la app de escritorio conectadas a la vez — esto es justamente lo que hace más urgente el punto de concurrencia del stock (`@Version`) mencionado arriba.
- Revisar `ddl-auto` de Hibernate: para producción normalmente se prefiere `validate` y manejar el esquema con migraciones (Flyway/Liquibase) en vez de dejar que Hibernate genere las tablas automáticamente.

---

## 8. Recomendación para el apartado visual (app de escritorio)

Dado que el Word especifica explícitamente **"aplicación de escritorio en Java"**, la opción natural — y la que mejor combina con este backend Spring — es:

### Opción recomendada: JavaFX + FXML + Scene Builder
- **JavaFX** es el framework de UI de escritorio moderno para Java (Swing está en mantenimiento, no en desarrollo activo).
- **Scene Builder** (herramienta gratuita de Gluon) permite diseñar las pantallas de forma visual (arrastrar y soltar), generando archivos `.fxml` que luego se conectan a "Controllers" Java — parecido a cómo diseñarían una interfaz en Figma pero ya en el lenguaje de la app.
- Se integra bien con Spring: existe el patrón "Spring Boot + JavaFX" donde el `ApplicationContext` de Spring inyecta los `@Service` (los que ya tienen) directamente en los controladores de JavaFX — **sin necesidad de exponer una API REST**, si el sistema va a correr como una sola aplicación de escritorio conectada directo a Supabase.
- Si en cambio van a tener **varias computadoras** (caja, almacén, administración) accediendo al mismo tiempo — que es lo que describe el Word (personal de almacén, personal de ventas, área administrativa) — conviene separar en dos partes:
  - Este backend Spring Boot expuesto como **API REST** (agregar `@RestController` sobre los `Service` que ya tienen).
  - Un cliente JavaFX en cada estación que consuma esa API vía HTTP (con `RestTemplate` o `WebClient`).
  - Esto también resuelve de forma más limpia el problema de concurrencia de stock, porque todo pasa por un único punto (el servidor), y ahí sí conviene el `@Version`/locking mencionado antes.

**Para que no se vea "Java de los 2000":** hay librerías que dan estilo moderno a JavaFX sin mucho esfuerzo — **AtlantaFX** o **JFoenix** (componentes estilo Material Design), o simplemente CSS personalizado (JavaFX se estiliza con hojas de estilo tipo CSS).

**Para empaquetar** la app final como un instalador de escritorio (.exe/.dmg/.deb) sin pedirle al usuario que instale Java aparte: **`jpackage`** (viene con el JDK desde Java 14+).

### Alternativas (por si prefieren comparar)
- **Swing:** más simple/antiguo, curva de aprendizaje menor si ya lo conocen del curso, pero visualmente más limitado y sin desarrollo activo — no lo recomendaría para un proyecto nuevo en 2026.
- Si en una fase posterior quisieran mover esto a algo accesible desde cualquier navegador (fuera del alcance actual, que es explícitamente escritorio), Spring Boot ya está listo para servir un frontend web (Thymeleaf, o una API consumida por React/Angular) sin cambiar la capa de servicios.

¿Quieren que arme un ejemplo concreto (estructura de carpetas + una pantalla FXML de ejemplo, por ejemplo el listado de productos con alertas de bajo stock) para que vean cómo se conecta JavaFX con el `ProductoServiceImpl` que ya tienen?
