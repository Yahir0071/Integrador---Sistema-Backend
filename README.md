# SGCIVORP — Sistema de Gestión Comercial, Inventario, Ventas y Órdenes de Pedido

**Universidad Tecnológica del Perú (UTP)**  
**Proyecto Integrador — Grupo 06**

---

## 1. Resumen Ejecutivo

SGCIVORP es una solución integral de software empresarial orientada a la administración de comercios minoristas, bodegas y minimarkets. El sistema implementa una arquitectura híbrida desacoplada compuesta por:

1. **Panel de Escritorio Corporativo (JavaFX):** Diseñado para la operativa interna del establecimiento (cajeros, administradores y jefes de almacén), centralizando el punto de venta (POS), abastecimiento con proveedores, auditoría de existencias, control de mermas y balance financiero.
2. **Servidor API REST (Spring Boot):** Expone los servicios de consulta de catálogo, gestión transaccional de órdenes de pedido y autenticación para la plataforma de comercio electrónico.

---

## 2. Arquitectura de la Solución y Stack Tecnológico

El núcleo del backend está desarrollado sobre el ecosistema Java empresarial bajo estándares de concurrencia y seguridad:

* **Entorno de Ejecución:** Java Development Kit (JDK) 21 LTS.
* **Framework Principal:** Spring Boot 4.1.1.
  * **Capa de Persistencia:** Spring Data JPA / Hibernate 7 con consultas optimizadas y control transaccional mediante `@EntityGraph` y bloqueos pesimistas (`PESSIMISTIC_WRITE`).
  * **Capa de Seguridad:** Spring Security 7 y Nimbus JOSE JWT para la emisión y verificación de tokens de acceso para clientes web.
  * **Capa Web:** Spring Boot Starter WebMVC (Apache Tomcat embebido).
* **Capa de Presentación de Escritorio:** JavaFX 21 + FXML con integración de BootstrapFX.
* **Base de Datos:** PostgreSQL en la nube (alojado en Supabase) con gestión de pool de conexiones mediante HikariCP.
* **Motor de Reportes:** Apache POI 5.3.0 (Generación de hojas de cálculo nativas Microsoft Excel `.xlsx` con tipografía corporativa, formatos de moneda y cálculo de totales).
* **Motor de Códigos Bidimensionales:** Google ZXing 3.5.4 (Generación y lectura de códigos QR para recojo presencial de órdenes).
* **Herramienta de Construcción:** Apache Maven 3.9 (a través de Maven Wrapper `mvnw`).

---

## 3. Requisitos Previos del Sistema

Antes de iniciar el sistema, compruebe que el equipo cuente con los siguientes componentes instalados:

* **Java JDK 21 o superior:** Verifique en consola con el comando:
  ```bash
  java -version
  ```
* **Conexión a Internet:** Requerida para la sincronización con la base de datos en Supabase y la descarga inicial de dependencias de Maven.
* **IDE Recomendado:** IntelliJ IDEA (Community o Ultimate Edition) o Eclipse IDE / VS Code con extensiones Java.

---

## 4. Guía de Instalación y Puesta en Marcha

Para iniciar el sistema de forma correcta y evitar problemas de comunicación entre procesos, siga el procedimiento ordenado a continuación:

### Paso 1: Configuración de la Base de Datos
El proyecto ya cuenta con los parámetros de conexión predeterminados configurados en el archivo:
`src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require
spring.datasource.username=postgres.qoupwzmfmsxtjasackkf
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=validate
```

> **Nota:** La propiedad `ddl-auto=validate` garantiza la integridad de los datos, validando que el esquema de base de datos coincida con las entidades JPA sin alterar ni eliminar registros preexistentes.

### Paso 2: Ejecución del Servidor API REST
La API proporciona los datos de catálogo e interactúa con la tienda web de clientes.

* **Desde IntelliJ IDEA:**
  1. Abra el proyecto en IntelliJ.
  2. En el explorador lateral navegue a: `src/main/java/pe/edu/utp/Grupo06/Grupo06Application.java`.
  3. Haga clic derecho y seleccione **Run 'Grupo06Application'** (o use el selector de ejecución superior).
  4. Verifique en la consola inferior la confirmación de inicio:
     ```text
     Tomcat started on port 8080 (http) with context path '/'
     Started Grupo06Application in X.X seconds
     ```

### Paso 3: Ejecución del Panel de Escritorio (JavaFX)
El panel gráfico permite la atención en mostrador y la gestión de almacén.

* **Desde IntelliJ IDEA:**
  1. En el explorador lateral navegue a: `src/main/java/pe/edu/utp/Grupo06/AppLauncher.java`.
  2. Haga clic derecho y seleccione **Run 'AppLauncher.main()'**.
  3. Se desplegará la interfaz gráfica en una ventana independiente, solicitando credenciales de usuario.

> **Importante:** Ambos procesos (`Grupo06Application` y `AppLauncher`) pueden mantenerse en ejecución simultánea dentro de IntelliJ IDEA mediante pestañas de ejecución independientes.

---

## 5. Módulos Funcionales del Sistema

### Módulo 1: Punto de Venta (POS)
* Facturación rápida de mostrador con cálculo de Subtotal, IGV (18%) y Total.
* Métodos de cobro integrados: Efectivo, Yape y Tarjetas de Crédito/Débito.
* Emisión e impresión de tickets de venta con desglose detallado.
* Control estricto de **Stock Disponible** (`stockActual - stockReservado`), impidiendo que el cajero venda productos apartados temporalmente por clientes en la plataforma web.

### Módulo 2: Catálogo e Inventario
* Mantenimiento de productos: código comercial, descripción, marca, categoría y unidad de medida base.
* Configuración de presentaciones de empaque (unidades, six-packs, paquetes y cajas) con factor de conversión.
* Notificación visual preventiva de productos por debajo del umbral de stock mínimo.

### Módulo 3: Gestión de Abastecimiento y Compras
* Registro de compras a proveedores por factura o comprobante.
* Verificación detallada de recepción: unidades esperadas, recibidas, rechazadas e ingresadas a almacén.
* Proceso de anulación de compra con reversión automática y validada del inventario.

### Módulo 4: Control de Mermas y Bajas
* Registro formal de bajas operativas por vencimiento, rotura o merma física.
* Registro de auditoría con usuario responsable, motivo clasificado y valorización de la pérdida económica.

### Módulo 5: Reportes Financieros y Exportación a Excel (.xlsx)
* Paneles analíticos con filtros temporales: Hoy, Últimos 7 Días, Mes Actual o Selección Mensual Específica.
* Métricas clave de rendimiento (KPIs): Total de ingresos, comprobantes emitidos, ticket promedio e inversión en mercadería.
* Exportación nativa a archivos **Microsoft Excel (.xlsx)** con formato corporativo: celdas formateadas a moneda nacional (S/), cabeceras coloreadas y autoajuste de ancho de columnas.

### Módulo 6: Gestión de Pedidos Web
* Bandeja interactiva que recibe en tiempo real las órdenes generadas por los clientes desde la tienda en línea.
* Inspección visual del comprobante de transferencia Yape cargado por el cliente.
* Transición de estados operativos: *Pendiente de Verificación -> Aprobado / Pagado -> En Preparación -> Listo para Recoger*.
* Validación y cierre de entrega presencial mediante escaneo de código QR o digitación del código de recojo (`NV-...`).

---

## 6. Estructura del Repositorio

```text
Grupo06/
├── pom.xml                                  # Archivo descriptor de dependencias Maven
├── mvnw / mvnw.cmd                          # Ejecutables binarios de Maven Wrapper
├── database/                                # Scripts SQL de definición y migración
│   ├── 01-esquema-inicial.sql               # Esquema de tablas y secuencias
│   └── 03-actualizar-supabase-20261026.sql  # Script de actualización para pedidos web
└── src/
    ├── main/
    │   ├── java/pe/edu/utp/Grupo06/
    │   │   ├── AppLauncher.java             # Punto de entrada para el entorno JavaFX
    │   │   ├── Grupo06Application.java      # Punto de entrada para el servidor Spring Boot
    │   │   ├── config/                      # Seguridad, CORS y configuración de beans
    │   │   ├── controller/                  # Controladores REST públicos y privados
    │   │   ├── dto/                         # Objetos de transferencia de datos
    │   │   ├── model/                       # Entidades del modelo relacional JPA
    │   │   ├── repository/                  # Interfaces Spring Data JPA
    │   │   ├── service/                     # Lógica de negocio e interfaces de servicio
    │   │   ├── util/                        # Clases de utilidad y formateo
    │   │   └── view/                        # Controladores de vista JavaFX
    │   └── resources/
    │       ├── application.properties       # Configuración central de la aplicación
    │       ├── css/                         # Hojas de estilo de la interfaz gráfica
    │       └── fxml/                        # Plantillas declarativas de ventanas JavaFX
    └── test/                                # Pruebas unitarias y de integración
```

---

## 7. Información Académica

* **Institución:** Universidad Tecnológica del Perú (UTP)
* **Carrera:** Ingeniería de Sistemas e Informática
* **Asignatura:** Proyecto Integrador
* **Equipo de Trabajo:** Grupo 06
