package pe.edu.utp.Grupo06.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pe.edu.utp.Grupo06.dto.reporte.ResumenInventarioDTO;
import pe.edu.utp.Grupo06.dto.venta.ProductoRotacionDTO;
import pe.edu.utp.Grupo06.model.*;
import pe.edu.utp.Grupo06.model.enums.MetodoPago;
import pe.edu.utp.Grupo06.repository.DetalleCompraRepository;
import pe.edu.utp.Grupo06.repository.DetalleVentaRepository;
import pe.edu.utp.Grupo06.repository.PagoRepository;
import pe.edu.utp.Grupo06.repository.VentaRepository;
import pe.edu.utp.Grupo06.service.ICategoriaService;
import pe.edu.utp.Grupo06.service.ICompraService;
import pe.edu.utp.Grupo06.service.IReporteService;
import pe.edu.utp.Grupo06.service.IVentaService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class ReportesViewController {

    @Autowired
    private IReporteService reporteService;

    @Autowired
    private IVentaService ventaService;

    @Autowired
    private ICompraService compraService;

    @Autowired
    private ICategoriaService categoriaService;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private DetalleCompraRepository detalleCompraRepository;

    @Autowired
    private PagoRepository pagoRepository;

    // --- Pestaña 1: Resumen General y Rotación ---
    @FXML
    private Label lblTotalProductos;
    @FXML
    private Label lblProductosBajoStock;
    @FXML
    private Label lblValorizacion;

    @FXML
    private ComboBox<String> cbPeriodoRotacion;
    @FXML
    private ComboBox<Categoria> cbCategoriaRotacion;
    @FXML
    private TextField txtBuscarRotacion;
    @FXML
    private Label lblTotalRotacion;

    @FXML
    private TableView<ProductoRotacionDTO> tblRotacion;
    @FXML
    private TableColumn<ProductoRotacionDTO, String> colRotCodigo;
    @FXML
    private TableColumn<ProductoRotacionDTO, String> colRotNombre;
    @FXML
    private TableColumn<ProductoRotacionDTO, Long> colRotVendidos;
    @FXML
    private TableColumn<ProductoRotacionDTO, BigDecimal> colRotRecaudado;

    // --- Pestaña 2: Historial de Ventas con Filtros ---
    @FXML
    private TextField txtFiltroHistorial;
    @FXML
    private ComboBox<String> cbFiltroVendedor;
    @FXML
    private DatePicker dpFechaInicio;
    @FXML
    private DatePicker dpFechaFin;
    @FXML
    private Label lblTotalFiltrado;

    @FXML
    private TableView<Venta> tblHistorialVentas;
    @FXML
    private TableColumn<Venta, String> colVenTicket;
    @FXML
    private TableColumn<Venta, LocalDateTime> colVenFecha;
    @FXML
    private TableColumn<Venta, String> colVenCajero;
    @FXML
    private TableColumn<Venta, BigDecimal> colVenTotal;
    @FXML
    private TableColumn<Venta, String> colVenEstado;
    @FXML
    private TableColumn<Venta, Void> colVenAccion;

    // --- Pestaña 3: Dashboard & Gráficos ---
    @FXML
    private ComboBox<String> cbDashPeriodo;
    @FXML
    private HBox boxDashMesAnio;
    @FXML
    private ComboBox<String> cbDashMes;
    @FXML
    private ComboBox<Integer> cbDashAnio;
    @FXML
    private Label lblPeriodoActivo;

    @FXML
    private Label lblKpiIngresos;
    @FXML
    private Label lblKpiVentas;
    @FXML
    private Label lblKpiTicketPromedio;
    @FXML
    private Label lblKpiProductoTop;
    @FXML
    private Label lblKpiProductoTopCant;

    @FXML
    private AreaChart<String, Number> chartVentasTiempo;
    @FXML
    private PieChart chartMetodosPago;
    @FXML
    private BarChart<String, Number> chartTopProductos;

    @FXML
    private TableView<VentaVendedorDTO> tblVentasPorVendedor;
    @FXML
    private TableColumn<VentaVendedorDTO, String> colVendNombre;
    @FXML
    private TableColumn<VentaVendedorDTO, Long> colVendTickets;
    @FXML
    private TableColumn<VentaVendedorDTO, BigDecimal> colVendTotal;

    // --- Pestaña 4: Historial de Compras en Reportes ---
    @FXML
    private TextField txtFiltroComprasReporte;
    @FXML
    private DatePicker dpFechaInicioCompReporte;
    @FXML
    private DatePicker dpFechaFinCompReporte;
    @FXML
    private Label lblTotalComprasReporte;

    @FXML
    private TableView<Compra> tblComprasReporte;
    @FXML
    private TableColumn<Compra, String> colRepCompNumero;
    @FXML
    private TableColumn<Compra, String> colRepCompProveedor;
    @FXML
    private TableColumn<Compra, LocalDateTime> colRepCompFecha;
    @FXML
    private TableColumn<Compra, String> colRepCompUsuario;
    @FXML
    private TableColumn<Compra, BigDecimal> colRepCompTotal;
    @FXML
    private TableColumn<Compra, Void> colRepCompAccion;

    // Colecciones observables
    private final ObservableList<ProductoRotacionDTO> listaRotacion = FXCollections.observableArrayList();
    private final ObservableList<Venta> listaHistorialVentas = FXCollections.observableArrayList();
    private FilteredList<Venta> filteredHistorial;

    private final ObservableList<Compra> listaComprasReporte = FXCollections.observableArrayList();
    private FilteredList<Compra> filteredComprasReporte;

    private final ObservableList<VentaVendedorDTO> listaVentasVendedor = FXCollections.observableArrayList();

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML
    public void initialize() {
        configurarTablaRotacion();
        configurarFiltrosRotacion();

        configurarTablaVentas();
        configurarFiltrosHistorial();

        configurarControlesDashboard();
        configurarTablaVentasVendedor();

        configurarTablaComprasReporte();

        cargarReportes();
    }

    // ==================== PESTAÑA 1: RESUMEN Y ROTACIÓN ====================

    private void configurarTablaRotacion() {
        colRotCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colRotNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colRotVendidos.setCellValueFactory(new PropertyValueFactory<>("cantidadTotalVendida"));
        colRotRecaudado.setCellValueFactory(new PropertyValueFactory<>("totalRecaudado"));
        tblRotacion.setItems(listaRotacion);
    }

    private void configurarFiltrosRotacion() {
        if (cbPeriodoRotacion != null) {
            cbPeriodoRotacion.getItems().setAll("Todo el Historial", "Hoy", "Últimos 7 Días", "Este Mes");
            cbPeriodoRotacion.setValue("Todo el Historial");
        }

        if (cbCategoriaRotacion != null) {
            cargarCategoriasFiltro();
        }

        if (txtBuscarRotacion != null) {
            txtBuscarRotacion.textProperty().addListener((obs, oldV, newV) -> handleFiltrarRotacion());
        }
    }

    private void cargarCategoriasFiltro() {
        try {
            Categoria catTodas = new Categoria();
            catTodas.setId(null);
            catTodas.setNombre("Todas las Categorías");

            List<Categoria> cats = new ArrayList<>();
            cats.add(catTodas);
            cats.addAll(categoriaService.listarActivas());

            cbCategoriaRotacion.getItems().setAll(cats);
            cbCategoriaRotacion.setValue(catTodas);
            cbCategoriaRotacion.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(Categoria c) {
                    return c != null ? c.getNombre() : "";
                }
                @Override
                public Categoria fromString(String string) {
                    return null;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleFiltrarRotacion() {
        try {
            String periodo = cbPeriodoRotacion != null && cbPeriodoRotacion.getValue() != null
                    ? cbPeriodoRotacion.getValue() : "Todo el Historial";
            Categoria catSel = cbCategoriaRotacion != null ? cbCategoriaRotacion.getValue() : null;
            Long catId = (catSel != null && catSel.getId() != null) ? catSel.getId() : null;
            String texto = txtBuscarRotacion != null && txtBuscarRotacion.getText() != null
                    ? txtBuscarRotacion.getText().trim().toLowerCase() : "";

            LocalDateTime inicio;
            LocalDateTime fin = LocalDateTime.now();

            switch (periodo) {
                case "Hoy":
                    inicio = LocalDate.now().atStartOfDay();
                    break;
                case "Últimos 7 Días":
                    inicio = LocalDate.now().minusDays(6).atStartOfDay();
                    break;
                case "Este Mes":
                    inicio = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                    break;
                default:
                    inicio = LocalDate.of(2000, 1, 1).atStartOfDay();
                    break;
            }

            List<Object[]> resultados = detalleVentaRepository.findProductosMayorRotacionFiltrado(inicio, fin, catId);
            List<ProductoRotacionDTO> ranking = new ArrayList<>();

            for (Object[] fila : resultados) {
                Number idNum = (Number) fila[0];
                Long id = idNum != null ? idNum.longValue() : null;
                String codigo = (String) fila[1];
                String nombre = (String) fila[2];
                Number cantNum = (Number) fila[3];
                Long cantidadTotalVendida = cantNum != null ? cantNum.longValue() : 0L;
                BigDecimal totalRecaudado = (BigDecimal) fila[4];

                if (!texto.isEmpty()) {
                    boolean matchNom = nombre != null && nombre.toLowerCase().contains(texto);
                    boolean matchCod = codigo != null && codigo.toLowerCase().contains(texto);
                    if (!matchNom && !matchCod) continue;
                }

                ranking.add(new ProductoRotacionDTO(id, nombre, codigo, cantidadTotalVendida, totalRecaudado));
            }

            listaRotacion.setAll(ranking);
            if (lblTotalRotacion != null) {
                lblTotalRotacion.setText("Total Productos: " + ranking.size());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLimpiarRotacion() {
        if (cbPeriodoRotacion != null) cbPeriodoRotacion.setValue("Todo el Historial");
        if (cbCategoriaRotacion != null && !cbCategoriaRotacion.getItems().isEmpty()) {
            cbCategoriaRotacion.setValue(cbCategoriaRotacion.getItems().get(0));
        }
        if (txtBuscarRotacion != null) txtBuscarRotacion.clear();
        handleFiltrarRotacion();
    }

    // ==================== PESTAÑA 2: HISTORIAL DE VENTAS ====================

    private void configurarTablaVentas() {
        colVenTicket.setCellValueFactory(new PropertyValueFactory<>("numeroTicket"));

        // Ordenamiento cronológico real mediante LocalDateTime
        colVenFecha.setCellValueFactory(new PropertyValueFactory<>("fechaVenta"));
        colVenFecha.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(dtf.format(date));
                }
            }
        });

        colVenCajero.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getUsuario() != null ? c.getValue().getUsuario().getNombreCompleto() : "N/A"));

        colVenTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colVenEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        colVenAccion.setCellFactory(col -> new TableCell<>() {
            private final Button btnVer = new Button("👁️ Ver Boleta");
            {
                btnVer.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #0284c7; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4;");
                btnVer.setOnAction(e -> {
                    Venta v = getTableView().getItems().get(getIndex());
                    mostrarTicketHistorico(v);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setGraphic(btnVer);
                }
            }
        });

        filteredHistorial = new FilteredList<>(listaHistorialVentas, p -> true);
        SortedList<Venta> sorted = new SortedList<>(filteredHistorial);
        sorted.comparatorProperty().bind(tblHistorialVentas.comparatorProperty());
        tblHistorialVentas.setItems(sorted);
    }

    private void configurarFiltrosHistorial() {
        if (txtFiltroHistorial != null) {
            txtFiltroHistorial.textProperty().addListener((obs, oldV, newV) -> aplicarFiltroHistorial());
        }
        if (dpFechaInicio != null) {
            dpFechaInicio.valueProperty().addListener((obs, oldV, newV) -> aplicarFiltroHistorial());
        }
        if (dpFechaFin != null) {
            dpFechaFin.valueProperty().addListener((obs, oldV, newV) -> aplicarFiltroHistorial());
        }
        if (cbFiltroVendedor != null) {
            cbFiltroVendedor.valueProperty().addListener((obs, oldV, newV) -> aplicarFiltroHistorial());
        }
    }

    private void actualizarComboVendedores() {
        if (cbFiltroVendedor == null) return;
        String seleccionado = cbFiltroVendedor.getValue();

        Set<String> vendedores = new TreeSet<>();
        for (Venta v : listaHistorialVentas) {
            if (v.getUsuario() != null && v.getUsuario().getNombreCompleto() != null) {
                vendedores.add(v.getUsuario().getNombreCompleto());
            }
        }

        List<String> items = new ArrayList<>();
        items.add("Todos los Cajeros");
        items.addAll(vendedores);

        cbFiltroVendedor.getItems().setAll(items);
        if (seleccionado != null && items.contains(seleccionado)) {
            cbFiltroVendedor.setValue(seleccionado);
        } else {
            cbFiltroVendedor.setValue("Todos los Cajeros");
        }
    }

    @FXML
    public void handleFiltrarHistorial() {
        aplicarFiltroHistorial();
    }

    @FXML
    public void handleLimpiarFiltroHistorial() {
        if (txtFiltroHistorial != null) txtFiltroHistorial.clear();
        if (dpFechaInicio != null) dpFechaInicio.setValue(null);
        if (dpFechaFin != null) dpFechaFin.setValue(null);
        if (cbFiltroVendedor != null) cbFiltroVendedor.setValue("Todos los Cajeros");
        aplicarFiltroHistorial();
    }

    private void aplicarFiltroHistorial() {
        String texto = txtFiltroHistorial != null && txtFiltroHistorial.getText() != null
                ? txtFiltroHistorial.getText().trim().toLowerCase() : "";
        LocalDate fechaInicio = dpFechaInicio != null ? dpFechaInicio.getValue() : null;
        LocalDate fechaFin = dpFechaFin != null ? dpFechaFin.getValue() : null;
        String cajeroSel = cbFiltroVendedor != null ? cbFiltroVendedor.getValue() : null;

        filteredHistorial.setPredicate(v -> {
            if (v == null) return false;

            if (!texto.isEmpty()) {
                boolean matchTicket = v.getNumeroTicket() != null && v.getNumeroTicket().toLowerCase().contains(texto);
                boolean matchCajero = v.getUsuario() != null && v.getUsuario().getNombreCompleto() != null &&
                        v.getUsuario().getNombreCompleto().toLowerCase().contains(texto);
                if (!matchTicket && !matchCajero) return false;
            }

            if (cajeroSel != null && !cajeroSel.equals("Todos los Cajeros")) {
                if (v.getUsuario() == null || !cajeroSel.equals(v.getUsuario().getNombreCompleto())) {
                    return false;
                }
            }

            if (v.getFechaVenta() != null) {
                LocalDate fechaV = v.getFechaVenta().toLocalDate();
                if (fechaInicio != null && fechaV.isBefore(fechaInicio)) return false;
                if (fechaFin != null && fechaV.isAfter(fechaFin)) return false;
            }

            return true;
        });

        BigDecimal totalSum = filteredHistorial.stream()
                .map(Venta::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (lblTotalFiltrado != null) {
            lblTotalFiltrado.setText(String.format("Total filtrado: S/ %.2f (%d tickets)",
                    totalSum.doubleValue(), filteredHistorial.size()));
        }
    }

    // ==================== PESTAÑA 3: DASHBOARD & GRÁFICOS ====================

    private void configurarControlesDashboard() {
        cbDashPeriodo.getItems().setAll("Hoy", "Últimos 7 Días", "Este Mes", "Mes Específico");
        cbDashPeriodo.setValue("Hoy");

        List<String> meses = Arrays.asList(
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Setiembre", "Octubre", "Noviembre", "Diciembre"
        );
        cbDashMes.getItems().setAll(meses);
        cbDashMes.setValue(meses.get(LocalDate.now().getMonthValue() - 1));

        int anioActual = LocalDate.now().getYear();
        cbDashAnio.getItems().setAll(anioActual, anioActual - 1, anioActual - 2);
        cbDashAnio.setValue(anioActual);

        boxDashMesAnio.setVisible(false);
        boxDashMesAnio.setManaged(false);

        cbDashPeriodo.setOnAction(e -> {
            boolean esMesEspecifico = "Mes Específico".equals(cbDashPeriodo.getValue());
            boxDashMesAnio.setVisible(esMesEspecifico);
            boxDashMesAnio.setManaged(esMesEspecifico);
            handleActualizarDashboard();
        });

        cbDashMes.setOnAction(e -> handleActualizarDashboard());
        cbDashAnio.setOnAction(e -> handleActualizarDashboard());
    }

    private void configurarTablaVentasVendedor() {
        if (tblVentasPorVendedor != null) {
            colVendNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
            colVendTickets.setCellValueFactory(new PropertyValueFactory<>("tickets"));
            colVendTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
            tblVentasPorVendedor.setItems(listaVentasVendedor);
        }
    }

    @FXML
    public void handleActualizarDashboard() {
        String opcion = cbDashPeriodo.getValue();
        if (opcion == null) opcion = "Hoy";

        LocalDateTime inicio;
        LocalDateTime fin = LocalDateTime.now();
        String textoPeriodo;

        switch (opcion) {
            case "Hoy":
                inicio = LocalDate.now().atStartOfDay();
                textoPeriodo = "Visualizando: Hoy (" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")";
                break;

            case "Últimos 7 Días":
                inicio = LocalDate.now().minusDays(6).atStartOfDay();
                textoPeriodo = "Visualizando: Últimos 7 Días (" + inicio.format(DateTimeFormatter.ofPattern("dd/MM")) + " al " + fin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")";
                break;

            case "Este Mes":
                inicio = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                textoPeriodo = "Visualizando: Mes Actual (" + YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")) + ")";
                break;

            case "Mes Específico":
                int mesIdx = cbDashMes.getSelectionModel().getSelectedIndex() + 1;
                if (mesIdx <= 0) mesIdx = LocalDate.now().getMonthValue();
                Integer anioSel = cbDashAnio.getValue();
                if (anioSel == null) anioSel = LocalDate.now().getYear();

                YearMonth ym = YearMonth.of(anioSel, mesIdx);
                inicio = ym.atDay(1).atStartOfDay();
                fin = ym.atEndOfMonth().atTime(LocalTime.MAX);
                textoPeriodo = "Visualizando: " + cbDashMes.getValue() + " " + anioSel;
                break;

            default:
                inicio = LocalDate.now().atStartOfDay();
                textoPeriodo = "Visualizando: Hoy";
                break;
        }

        lblPeriodoActivo.setText(textoPeriodo);
        actualizarMetricasPeriodo(inicio, fin, opcion);
    }

    private void actualizarMetricasPeriodo(LocalDateTime inicio, LocalDateTime fin, String modoPeriodo) {
        try {
            // 1. Obtener ventas emitidas del periodo
            List<Venta> ventasPeriodo = ventaRepository.findVentasEntreFechas(inicio, fin);

            BigDecimal totalIngresos = BigDecimal.ZERO;
            for (Venta v : ventasPeriodo) {
                totalIngresos = totalIngresos.add(v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO);
            }

            int numTickets = ventasPeriodo.size();
            BigDecimal ticketPromedio = numTickets > 0
                    ? totalIngresos.divide(BigDecimal.valueOf(numTickets), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            lblKpiIngresos.setText("S/ " + totalIngresos.setScale(2, RoundingMode.HALF_UP));
            lblKpiVentas.setText(String.valueOf(numTickets));
            lblKpiTicketPromedio.setText("S/ " + ticketPromedio);

            // 2. Gráfico 1: Área de Ingresos en el Tiempo
            chartVentasTiempo.getData().clear();
            XYChart.Series<String, Number> seriesTiempo = new XYChart.Series<>();

            if ("Hoy".equals(modoPeriodo)) {
                Map<Integer, BigDecimal> porHora = new TreeMap<>();
                for (int h = 7; h <= 22; h++) porHora.put(h, BigDecimal.ZERO);
                for (Venta v : ventasPeriodo) {
                    int hora = v.getFechaVenta().getHour();
                    porHora.put(hora, porHora.getOrDefault(hora, BigDecimal.ZERO).add(v.getTotal()));
                }
                porHora.forEach((h, tot) -> seriesTiempo.getData().add(new XYChart.Data<>(String.format("%02d:00", h), tot)));
            } else {
                Map<LocalDate, BigDecimal> porDia = new TreeMap<>();
                LocalDate curr = inicio.toLocalDate();
                LocalDate endD = fin.toLocalDate();
                while (!curr.isAfter(endD)) {
                    porDia.put(curr, BigDecimal.ZERO);
                    curr = curr.plusDays(1);
                }
                for (Venta v : ventasPeriodo) {
                    LocalDate d = v.getFechaVenta().toLocalDate();
                    porDia.put(d, porDia.getOrDefault(d, BigDecimal.ZERO).add(v.getTotal()));
                }
                porDia.forEach((dia, tot) -> seriesTiempo.getData().add(new XYChart.Data<>(dia.format(DateTimeFormatter.ofPattern("dd/MM")), tot)));
            }
            chartVentasTiempo.getData().add(seriesTiempo);

            // 3. Gráfico 2: Métodos de Pago
            chartMetodosPago.getData().clear();
            List<Object[]> pagosAgrupados = pagoRepository.findMetodosPagoEntreFechas(inicio, fin);
            for (Object[] fila : pagosAgrupados) {
                MetodoPago mp = (MetodoPago) fila[0];
                BigDecimal suma = (BigDecimal) fila[1];
                if (suma != null && suma.compareTo(BigDecimal.ZERO) > 0) {
                    chartMetodosPago.getData().add(new PieChart.Data(mp.name() + " (S/ " + suma.setScale(2, RoundingMode.HALF_UP) + ")", suma.doubleValue()));
                }
            }

            // 4. Gráfico 3: Top 5 Productos más Vendidos
            chartTopProductos.getData().clear();
            XYChart.Series<String, Number> seriesProductos = new XYChart.Series<>();

            List<Object[]> topProds = detalleVentaRepository.findTopProductosEntreFechas(inicio, fin);
            String prodTop = "Ninguno";
            int cantTop = 0;
            int count = 0;

            for (Object[] fila : topProds) {
                String nom = (String) fila[0];
                Number cantNum = (Number) fila[1];
                int cant = cantNum != null ? cantNum.intValue() : 0;

                if (count == 0) {
                    prodTop = nom;
                    cantTop = cant;
                }

                if (count < 5) {
                    String label = nom;
                    if (label.length() > 18) label = label.substring(0, 16) + "..";
                    seriesProductos.getData().add(new XYChart.Data<>(label, cant));
                }
                count++;
            }

            lblKpiProductoTop.setText(prodTop);
            lblKpiProductoTopCant.setText(cantTop + (cantTop == 1 ? " unidad vendida" : " unidades vendidas"));
            chartTopProductos.getData().add(seriesProductos);

            // 5. Tabla de Ventas por Vendedor / Cajero
            if (tblVentasPorVendedor != null) {
                List<Object[]> ventasPorVendedor = ventaRepository.findVentasPorVendedorEntreFechas(inicio, fin);
                List<VentaVendedorDTO> listaVend = new ArrayList<>();
                for (Object[] fila : ventasPorVendedor) {
                    String vendNom = (String) fila[0];
                    Number numT = (Number) fila[1];
                    BigDecimal tot = (BigDecimal) fila[2];
                    listaVend.add(new VentaVendedorDTO(
                            vendNom != null ? vendNom : "Sin asignar",
                            numT != null ? numT.longValue() : 0L,
                            tot != null ? tot.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO
                    ));
                }
                listaVentasVendedor.setAll(listaVend);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== PESTAÑA 4: HISTORIAL DE COMPRAS ====================

    private void configurarTablaComprasReporte() {
        if (tblComprasReporte == null) return;

        colRepCompNumero.setCellValueFactory(new PropertyValueFactory<>("numeroComprobante"));

        colRepCompProveedor.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getProveedor() != null ? c.getValue().getProveedor().getRazonSocial() : "N/A"));

        colRepCompFecha.setCellValueFactory(new PropertyValueFactory<>("fechaCompra"));
        colRepCompFecha.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(dtf.format(date));
                }
            }
        });

        colRepCompUsuario.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getUsuario() != null ? c.getValue().getUsuario().getNombreCompleto() : "N/A"));

        colRepCompTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        colRepCompAccion.setCellFactory(col -> new TableCell<>() {
            private final Button btnVer = new Button("👁️ Ver Detalle");
            {
                btnVer.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #0284c7; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4;");
                btnVer.setOnAction(e -> {
                    Compra c = getTableView().getItems().get(getIndex());
                    mostrarDetalleCompraReporte(c);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setGraphic(btnVer);
                }
            }
        });

        filteredComprasReporte = new FilteredList<>(listaComprasReporte, p -> true);
        SortedList<Compra> sorted = new SortedList<>(filteredComprasReporte);
        sorted.comparatorProperty().bind(tblComprasReporte.comparatorProperty());
        tblComprasReporte.setItems(sorted);

        if (txtFiltroComprasReporte != null) {
            txtFiltroComprasReporte.textProperty().addListener((obs, oldV, newV) -> aplicarFiltroComprasReporte());
        }
        if (dpFechaInicioCompReporte != null) {
            dpFechaInicioCompReporte.valueProperty().addListener((obs, oldV, newV) -> aplicarFiltroComprasReporte());
        }
        if (dpFechaFinCompReporte != null) {
            dpFechaFinCompReporte.valueProperty().addListener((obs, oldV, newV) -> aplicarFiltroComprasReporte());
        }
    }

    private void cargarComprasReporte() {
        try {
            List<Compra> compras = compraService.listarCompras();
            listaComprasReporte.setAll(compras);
            aplicarFiltroComprasReporte();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleFiltrarComprasReporte() {
        aplicarFiltroComprasReporte();
    }

    @FXML
    public void handleLimpiarComprasReporte() {
        if (txtFiltroComprasReporte != null) txtFiltroComprasReporte.clear();
        if (dpFechaInicioCompReporte != null) dpFechaInicioCompReporte.setValue(null);
        if (dpFechaFinCompReporte != null) dpFechaFinCompReporte.setValue(null);
        aplicarFiltroComprasReporte();
    }

    private void aplicarFiltroComprasReporte() {
        if (filteredComprasReporte == null) return;

        String texto = txtFiltroComprasReporte != null && txtFiltroComprasReporte.getText() != null
                ? txtFiltroComprasReporte.getText().trim().toLowerCase() : "";
        LocalDate fechaInicio = dpFechaInicioCompReporte != null ? dpFechaInicioCompReporte.getValue() : null;
        LocalDate fechaFin = dpFechaFinCompReporte != null ? dpFechaFinCompReporte.getValue() : null;

        filteredComprasReporte.setPredicate(c -> {
            if (c == null) return false;

            if (!texto.isEmpty()) {
                boolean matchComp = c.getNumeroComprobante() != null && c.getNumeroComprobante().toLowerCase().contains(texto);
                boolean matchProv = c.getProveedor() != null && c.getProveedor().getRazonSocial() != null &&
                        c.getProveedor().getRazonSocial().toLowerCase().contains(texto);
                if (!matchComp && !matchProv) return false;
            }

            if (c.getFechaCompra() != null) {
                LocalDate fComp = c.getFechaCompra().toLocalDate();
                if (fechaInicio != null && fComp.isBefore(fechaInicio)) return false;
                if (fechaFin != null && fComp.isAfter(fechaFin)) return false;
            }

            return true;
        });

        BigDecimal totalSum = filteredComprasReporte.stream()
                .map(Compra::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (lblTotalComprasReporte != null) {
            lblTotalComprasReporte.setText(String.format("Total Compras: S/ %.2f (%d facturas)",
                    totalSum.doubleValue(), filteredComprasReporte.size()));
        }
    }

    private void mostrarDetalleCompraReporte(Compra c) {
        Dialog<Void> factDialog = new Dialog<>();
        factDialog.setTitle("Comprobante de Compra — " + c.getNumeroComprobante());

        ButtonType btnCerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.OK_DONE);
        factDialog.getDialogPane().getButtonTypes().add(btnCerrar);

        VBox root = new VBox(10);
        root.setStyle("-fx-padding: 15px; -fx-background-color: #ffffff;");
        root.setPrefWidth(550);

        Label lblHeader = new Label(
                "COMPROBANTE DE COMPRA A PROVEEDOR\n" +
                "N° Comprobante: " + c.getNumeroComprobante() + "\n" +
                "Proveedor: " + (c.getProveedor() != null ? c.getProveedor().getRazonSocial() + " (RUC: " + c.getProveedor().getRuc() + ")" : "N/A") + "\n" +
                "Fecha: " + (c.getFechaCompra() != null ? c.getFechaCompra().format(dtf) : "") + "\n" +
                "Recepcionado por: " + (c.getUsuario() != null ? c.getUsuario().getNombreCompleto() : "")
        );
        lblHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");

        List<DetalleCompra> detalles = detalleCompraRepository.findByCompraId(c.getId());
        TableView<DetalleCompra> tblDet = new TableView<>(FXCollections.observableArrayList(detalles));
        tblDet.setPrefHeight(180);

        TableColumn<DetalleCompra, String> colNom = new TableColumn<>("Producto");
        colNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProducto() != null ? d.getValue().getProducto().getNombre() : ""));
        colNom.setPrefWidth(220);

        TableColumn<DetalleCompra, Integer> colC = new TableColumn<>("Cantidad");
        colC.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colC.setPrefWidth(80);

        TableColumn<DetalleCompra, BigDecimal> colP = new TableColumn<>("P. Unit (S/)");
        colP.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colP.setPrefWidth(100);

        TableColumn<DetalleCompra, BigDecimal> colS = new TableColumn<>("Subtotal (S/)");
        colS.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        colS.setPrefWidth(110);

        tblDet.getColumns().addAll(colNom, colC, colP, colS);

        Label lblTot = new Label("TOTAL FACTURADO: S/ " + (c.getTotal() != null ? c.getTotal().setScale(2, RoundingMode.HALF_UP) : "0.00"));
        lblTot.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #15803d; -fx-alignment: CENTER_RIGHT;");

        root.getChildren().addAll(lblHeader, new Separator(), tblDet, lblTot);
        factDialog.getDialogPane().setContent(root);
        factDialog.showAndWait();
    }

    private void mostrarTicketHistorico(Venta v) {
        Dialog<Void> boletaDialog = new Dialog<>();
        boletaDialog.setTitle("Ticket de Venta — " + v.getNumeroTicket());

        ButtonType btnCerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.OK_DONE);
        boletaDialog.getDialogPane().getButtonTypes().add(btnCerrar);

        VBox root = new VBox(10);
        root.setStyle("-fx-font-family: 'Courier New', monospace; -fx-padding: 15px; -fx-background-color: #ffffff;");
        root.setPrefWidth(380);

        Label lblCabecera = new Label(
                "==========================================\n" +
                "           BODEGA SGCIVORP                \n" +
                "       RUC: 20123456789 - LIMA PERÚ       \n" +
                "==========================================\n" +
                "Ticket N°: " + v.getNumeroTicket() + "\n" +
                "Fecha: " + v.getFechaVenta().format(dtf) + "\n" +
                "Cajero: " + (v.getUsuario() != null ? v.getUsuario().getNombreCompleto() : "N/A") + "\n" +
                "------------------------------------------\n" +
                "CANT  DESCRIPCIÓN             P.U    TOTAL\n" +
                "------------------------------------------"
        );

        List<DetalleVenta> detalles = detalleVentaRepository.findByVentaId(v.getId());
        StringBuilder sbItems = new StringBuilder();
        for (DetalleVenta d : detalles) {
            String nom = d.getProducto().getNombre();
            if (nom.length() > 20) nom = nom.substring(0, 17) + "...";
            sbItems.append(String.format("%-4d  %-22s %5.2f  %6.2f\n",
                    d.getCantidad(), nom, d.getPrecioUnitario(), d.getSubtotal()));
        }

        Label lblItems = new Label(sbItems.toString());

        List<Pago> pagos = pagoRepository.findByVentaId(v.getId());
        StringBuilder sbPagos = new StringBuilder();
        sbPagos.append("------------------------------------------\n");
        sbPagos.append(String.format("TOTAL DE LA VENTA:              S/ %7.2f\n", v.getTotal()));
        sbPagos.append("------------------------------------------\n");
        sbPagos.append("MÉTODOS DE PAGO:\n");
        for (Pago p : pagos) {
            sbPagos.append(String.format(" - %-10s:                    S/ %7.2f\n", p.getMetodoPago(), p.getMonto()));
        }
        sbPagos.append("==========================================\n");
        sbPagos.append("       ESTADO: " + v.getEstado() + "\n");
        sbPagos.append("==========================================");

        Label lblPie = new Label(sbPagos.toString());

        root.getChildren().addAll(lblCabecera, lblItems, lblPie);
        boletaDialog.getDialogPane().setContent(root);
        boletaDialog.showAndWait();
    }

    @FXML
    public void cargarReportes() {
        try {
            ResumenInventarioDTO resumen = reporteService.obtenerResumenInventario();
            lblTotalProductos.setText(resumen.getTotalProductosActivos().toString());
            lblProductosBajoStock.setText(resumen.getProductosBajoStock().toString());
            lblValorizacion.setText("S/ " + resumen.getValorizacionTotalInventario().setScale(2, RoundingMode.HALF_UP).toString());

            handleFiltrarRotacion();

            List<Venta> historial = ventaService.listarVentas();
            listaHistorialVentas.setAll(historial);
            actualizarComboVendedores();
            aplicarFiltroHistorial();

            handleActualizarDashboard();

            cargarComprasReporte();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // DTO interno para tabla de ventas por vendedor
    public static class VentaVendedorDTO {
        private final String nombre;
        private final Long tickets;
        private final BigDecimal total;

        public VentaVendedorDTO(String nombre, Long tickets, BigDecimal total) {
            this.nombre = nombre;
            this.tickets = tickets;
            this.total = total != null ? total : BigDecimal.ZERO;
        }

        public String getNombre() { return nombre; }
        public Long getTickets() { return tickets; }
        public BigDecimal getTotal() { return total; }
    }
}
