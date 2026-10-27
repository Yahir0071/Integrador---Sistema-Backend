# SGCIVORP — Sistema de Gestión Comercial, Inventario y Ventas (Backend & Desktop)

Sistema integral de gestión empresarial para bodegas y minimarkets desarrollado para el **Proyecto Integrador** en la **Universidad Tecnológica del Perú (UTP)**.

El proyecto combina un **panel de escritorio corporativo en JavaFX** para el personal de tienda y una **API REST robusta en Spring Boot** que alimenta la plataforma de comercio electrónico para clientes.

---

## 🚀 Tecnologías y Arquitectura

* **Lenguaje:** Java 21 (LTS)
* **Framework Backend:** Spring Boot 4.1.1
  * Spring Data JPA / Hibernate (ORM con consultas optimizadas y carga transaccional)
  * Spring Security & JWT (Tokens de acceso seguro para la tienda web)
  * Validación Jakarta Validation
* **Interfaz de Escritorio:** JavaFX 21 + FXML con estilos corporativos BootstrapFX
* **Base de Datos:** PostgreSQL en la nube (Supabase) con pool de conexiones HikariCP
* **Reportes y Exportación:** Apache POI 5.3.0 (Generación de libros nativos `.xlsx` de Microsoft Excel con estilos corporativos)
* **Manejo de Códigos:** Google ZXing (Generación y verificación de códigos QR para recojo de pedidos)
* **Gestor de Construcción:** Maven Wrapper (`./mvnw.cmd`)

---

## 📦 Módulos Principales del Sistema

### 1. Punto de Venta (POS)
* Registro rápido de ventas presenciales con lector de código de barras o búsqueda manual.
* Desglose automático de importes: Subtotal, IGV (18%) y Total.
* Soporte para múltiples métodos de pago (Efectivo, Yape, Tarjeta).
* Emisión e impresión de tickets de venta con formato térmico.
* Validación inteligente de **Stock Disponible** (`stockActual - stockReservado`), evitando vender mercadería apartada por clientes web.

### 2. Control de Inventario y Stock
* Catálogo maestro de productos con código comercial, marcas, categorías y unidad de medida.
* Alertas preventivas automáticas de bajo stock en tiempo real.
* Soporte para presentaciones comerciales (unidades, six-packs, paquetes y cajas) con factores de conversión.

### 3. Gestión de Compras y Abastecimiento
* Registro y auditoría de comprobantes y facturas de compras a proveedores.
* Control de recepción de mercadería: seguimiento de unidades esperadas, recibidas, rechazadas e ingresadas al almacén.
* **Anulación segura de compras:** Reversión automática de inventario garantizando la consistencia del stock.

### 4. Control de Mermas y Bajas
* Auditoría de pérdidas por descomposición, vencimiento o deterioro físico.
* Registro del responsable, motivo y cálculo del impacto económico para balance contable.

### 5. Reportes Financieros y Exportación a Excel (.xlsx)
* Dashboard analítico de ventas por período (Hoy, Últimos 7 Días, Mes Actual o Mes Específico).
* Historial de ventas con desglose de métodos de pago y cajero auditor.
* Ranking de rotación de productos con cálculo de demanda y recaudación.
* Exportación nativa a **Microsoft Excel (.xlsx)** con diseño corporativo, cabeceras estilizadas, celdas formateadas a moneda y autoajuste de columnas.

### 6. Gestión de Pedidos Web
* Módulo interactivo en JavaFX para gestionar las órdenes entrantes desde la tienda web.
* Visualización directa de comprobantes de pago Yape subidos por los clientes.
* Aprobación de pagos, cambio de estados (Pendiente, En Preparación, Listo para Recoger).
* Registro de entrega presencial escaneando el código QR o ingresando el código de recojo.

---

## 🛠️ Cómo Ejecutar el Proyecto (IntelliJ IDEA)

El backend soporta ejecución dual directamente desde IntelliJ IDEA sin necesidad de comandos de consola:

```text
IntelliJ IDEA
 ├── ▶ AppLauncher           --> Abre el Panel de Escritorio (JavaFX)
 └── ▶ Grupo06Application    --> Abre el Servidor API REST (:8080)
```

### Paso 1: Ejecutar la Aplicación de Escritorio
1. Abre el proyecto `Grupo06` en **IntelliJ IDEA**.
2. En la barra superior derecha, selecciona **`AppLauncher`** y presiona el botón verde ▶ (**Play**).
3. *(Alternativamente: abre `src/main/java/pe/edu/utp/Grupo06/AppLauncher.java`, clic derecho y selecciona **Run 'AppLauncher.main()'**).*
4. Se abrirá la ventana gráfica con la pantalla de inicio de sesión de JavaFX.

### Paso 2: Ejecutar el Servidor API REST (para la tienda web)
1. En el mismo selector superior derecho, selecciona **`Grupo06Application`** y presiona ▶ (**Play**).
2. En la consola inferior verás que Spring Boot inicia en el puerto `8080`:
   ```text
   Tomcat started on port 8080 (http) with context path '/'
   Started Grupo06Application in 7.8 seconds
   ```
3. Ambos procesos pueden ejecutarse simultáneamente en pestañas separadas de IntelliJ.

---

## 🗄️ Base de Datos y Configuración

* La conexión a la base de datos se encuentra centralizada en:  
  📁 `src/main/resources/application.properties`
* La validación de esquema está configurada en `ddl-auto=validate`, asegurando que Hibernate conserve intactos todos los datos existentes.
* Los scripts oficiales de inicialización y actualización de la base de datos se encuentran en la carpeta:  
  📁 `database/`
  * `01-esquema-inicial.sql`: Creación de tablas, secuencias y restricciones.
  * `03-actualizar-supabase-20261026.sql`: Migración transaccional para pedidos web y rol cliente.

---

## 👥 Equipo de Desarrollo

* **Universidad:** Universidad Tecnológica del Perú (UTP)
* **Curso:** Proyecto Integrador
* **Grupo:** Grupo 06
