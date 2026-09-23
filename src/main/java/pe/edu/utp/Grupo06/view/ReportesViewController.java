package pe.edu.utp.Grupo06.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pe.edu.utp.Grupo06.dto.reporte.ResumenInventarioDTO;
import pe.edu.utp.Grupo06.dto.venta.ProductoRotacionDTO;
import pe.edu.utp.Grupo06.model.DetalleVenta;
import pe.edu.utp.Grupo06.model.Pago;
import pe.edu.utp.Grupo06.model.Venta;
import pe.edu.utp.Grupo06.model.enums.MetodoPago;
import pe.edu.utp.Grupo06.repository.DetalleVentaRepository;
import pe.edu.utp.Grupo06.repository.PagoRepository;
import pe.edu.utp.Grupo06.repository.VentaRepository;
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
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    // --- Pestaña 1: Resumen General ---
    @FXML
    private Label lblTotalProductos;
    @FXML
    private Label lblProductosBajoStock;
    @FXML
    private Label lblValorizacion;

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

    private final ObservableList<ProductoRotacionDTO> listaRotacion = FXCollections.observableArrayList();
    private final ObservableList<Venta> listaHistorialVentas = FXCollections.observableArrayList();
    private FilteredList<Venta> filteredHistorial;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML
    public void initialize() {
        configurarTablaRotacion();
        configurarTablaVentas();
        configurarFiltrosHistorial();
        configurarControlesDashboard();

        cargarReportes();
    }

    private void configurarTablaRotacion() {
        colRotCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colRotNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colRotVendidos.setCellValueFactory(new PropertyValueFactory<>("cantidadTotalVendida"));
        colRotRecaudado.setCellValueFactory(new PropertyValueFactory<>("totalRecaudado"));
        tblRotacion.setItems(listaRotacion);
    }

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
                new javafx.beans.property.SimpleStringProperty(c.getValue().getUsuario() != null ? c.getValue().getUsuario().getNombreCompleto() : "N/A"));
        colVenTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colVenEstado.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getEstado() != null ? c.getValue().getEstado().name() : ""));

        colVenAccion.setCellFactory(param -> new TableCell<>() {
            private final Button btnVerTicket = new Button("Ver Ticket");

            {
                btnVerTicket.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-cursor: hand;");
                btnVerTicket.setOnAction(event -> {
                    Venta v = getTableView().getItems().get(getIndex());
                    mostrarTicketHistorico(v);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox pane = new HBox(btnVerTicket);
                    pane.setAlignment(Pos.CENTER);
                    setGraphic(pane);
                }
            }
        });

        filteredHistorial = new FilteredList<>(listaHistorialVentas, p -> true);
        SortedList<Venta> sortedList = new SortedList<>(filteredHistorial);
        sortedList.comparatorProperty().bind(tblHistorialVentas.comparatorProperty());
        tblHistorialVentas.setItems(sortedList);
    }

    private void configurarFiltrosHistorial() {
        txtFiltroHistorial.textProperty().addListener((obs, oldVal, newVal) -> aplicarFiltroHistorial());
    }

    @FXML
    public void handleFiltrarHistorial() {
        aplicarFiltroHistorial();
    }

    @FXML
    public void handleLimpiarFiltroHistorial() {
        txtFiltroHistorial.clear();
        dpFechaInicio.setValue(null);
        dpFechaFin.setValue(null);
        aplicarFiltroHistorial();
    }

    private void aplicarFiltroHistorial() {
        String texto = txtFiltroHistorial.getText() != null ? txtFiltroHistorial.getText().trim().toLowerCase() : "";
        LocalDate fechaIni = dpFechaInicio.getValue();
        LocalDate fechaFin = dpFechaFin.getValue();

        filteredHistorial.setPredicate(v -> {
            if (v == null) return false;

            // Filtro por texto (Ticket o Cajero)
            if (!texto.isEmpty()) {
                boolean matchTicket = v.getNumeroTicket() != null && v.getNumeroTicket().toLowerCase().contains(texto);
                boolean matchCajero = v.getUsuario() != null && v.getUsuario().getNombreCompleto() != null &&
                        v.getUsuario().getNombreCompleto().toLowerCase().contains(texto);
                if (!matchTicket && !matchCajero) return false;
            }

            // Filtro por rango de fecha
            if (v.getFechaVenta() != null) {
                LocalDate fVenta = v.getFechaVenta().toLocalDate();
                if (fechaIni != null && fVenta.isBefore(fechaIni)) return false;
                if (fechaFin != null && fVenta.isAfter(fechaFin)) return false;
            }

            return true;
        });

        // Actualizar total filtrado
        BigDecimal suma = filteredHistorial.stream()
                .map(Venta::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalFiltrado.setText(String.format("Total filtrado: S/ %.2f", suma));
    }

    private void configurarControlesDashboard() {
        cbDashPeriodo.getItems().addAll("Hoy", "Últimos 7 Días", "Este Mes", "Mes Específico");
        cbDashPeriodo.setValue("Hoy");

        String[] meses = {"01 - Enero", "02 - Febrero", "03 - Marzo", "04 - Abril", "05 - Mayo", "06 - Junio",
                "07 - Julio", "08 - Agosto", "09 - Septiembre", "10 - Octubre", "11 - Noviembre", "12 - Diciembre"};
        cbDashMes.getItems().addAll(meses);
        int mesActual = LocalDate.now().getMonthValue();
        cbDashMes.setValue(meses[mesActual - 1]);

        int anioActual = LocalDate.now().getYear();
        cbDashAnio.getItems().addAll(anioActual - 1, anioActual, anioActual + 1);
        cbDashAnio.setValue(anioActual);

        boxDashMesAnio.setVisible(false);
        boxDashMesAnio.setManaged(false);

        cbDashPeriodo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean esMesEspecifico = "Mes Específico".equals(newVal);
            boxDashMesAnio.setVisible(esMesEspecifico);
            boxDashMesAnio.setManaged(esMesEspecifico);
            handleActualizarDashboard();
        });

        cbDashMes.valueProperty().addListener((obs, old, n) -> {
            if ("Mes Específico".equals(cbDashPeriodo.getValue())) handleActualizarDashboard();
        });
        cbDashAnio.valueProperty().addListener((obs, old, n) -> {
            if ("Mes Específico".equals(cbDashPeriodo.getValue())) handleActualizarDashboard();
        });
    }

    @FXML
    public void handleActualizarDashboard() {
        String periodo = cbDashPeriodo.getValue();
        if (periodo == null) periodo = "Hoy";

        LocalDateTime inicio;
        LocalDateTime fin = LocalDateTime.now().with(LocalTime.MAX);
        String labelPeriodo;

        switch (periodo) {
            case "Últimos 7 Días":
                inicio = LocalDate.now().minusDays(6).atStartOfDay();
                labelPeriodo = "Últimos 7 días (" + inicio.toLocalDate() + " al " + fin.toLocalDate() + ")";
                break;
            case "Este Mes":
                YearMonth ym = YearMonth.now();
                inicio = ym.atDay(1).atStartOfDay();
                fin = ym.atEndOfMonth().atTime(LocalTime.MAX);
                labelPeriodo = "Mes actual (" + ym.getMonth().name() + " " + ym.getYear() + ")";
                break;
            case "Mes Específico":
                int numMes = Integer.parseInt(cbDashMes.getValue().substring(0, 2));
                int numAnio = cbDashAnio.getValue();
                YearMonth ymEsp = YearMonth.of(numAnio, numMes);
                inicio = ymEsp.atDay(1).atStartOfDay();
                fin = ymEsp.atEndOfMonth().atTime(LocalTime.MAX);
                labelPeriodo = cbDashMes.getValue() + " " + numAnio;
                break;
            case "Hoy":
            default:
                inicio = LocalDate.now().atStartOfDay();
                labelPeriodo = "Hoy (" + LocalDate.now() + ")";
                break;
        }

        lblPeriodoActivo.setText("Visualizando: " + labelPeriodo);
        generarMetricasDashboard(inicio, fin, periodo);
    }

    private void generarMetricasDashboard(LocalDateTime inicio, LocalDateTime fin, String tipoPeriodo) {
        try {
            // 1. Ventas del periodo para KPIs y Tendencia
            List<Venta> ventas = ventaRepository.findVentasEntreFechas(inicio, fin);

            BigDecimal totalIngresos = BigDecimal.ZERO;
            int totalTickets = ventas.size();

            // Mapa para tendencia cronológica
            Map<String, BigDecimal> tendencia = new LinkedHashMap<>();

            if ("Hoy".equals(tipoPeriodo)) {
                for (int h = 7; h <= 22; h += 2) {
                    tendencia.put(String.format("%02d:00", h), BigDecimal.ZERO);
                }
            }

            for (Venta v : ventas) {
                totalIngresos = totalIngresos.add(v.getTotal());

                String slot;
                if ("Hoy".equals(tipoPeriodo)) {
                    int hora = v.getFechaVenta().getHour();
                    int slotHora = (hora / 2) * 2;
                    slot = String.format("%02d:00", Math.min(Math.max(slotHora, 7), 22));
                } else if ("Últimos 7 Días".equals(tipoPeriodo)) {
                    slot = v.getFechaVenta().format(DateTimeFormatter.ofPattern("E dd"));
                } else {
                    slot = "Día " + v.getFechaVenta().getDayOfMonth();
                }
                tendencia.put(slot, tendencia.getOrDefault(slot, BigDecimal.ZERO).add(v.getTotal()));
            }

            // Actualizar Tarjetas KPI
            lblKpiIngresos.setText(String.format("S/ %.2f", totalIngresos));
            lblKpiVentas.setText(totalTickets + (totalTickets == 1 ? " ticket" : " tickets"));

            BigDecimal ticketPromedio = totalTickets > 0 ?
                    totalIngresos.divide(BigDecimal.valueOf(totalTickets), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            lblKpiTicketPromedio.setText(String.format("S/ %.2f", ticketPromedio));

            // Gráfico 1: Área / Línea de Ingresos
            chartVentasTiempo.getData().clear();
            XYChart.Series<String, Number> seriesTiempo = new XYChart.Series<>();
            seriesTiempo.setName("Ingresos S/");
            for (Map.Entry<String, BigDecimal> e : tendencia.entrySet()) {
                seriesTiempo.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
            }
            chartVentasTiempo.getData().add(seriesTiempo);

            // 2. Gráfico 2: Métodos de Pago agrupados en BD (evita MultipleBagFetchException)
            chartMetodosPago.getData().clear();
            List<Object[]> pagosAgrupados = pagoRepository.findMetodosPagoEntreFechas(inicio, fin);
            for (Object[] fila : pagosAgrupados) {
                MetodoPago metodo = (MetodoPago) fila[0];
                BigDecimal monto = (BigDecimal) fila[1];
                if (monto != null && monto.compareTo(BigDecimal.ZERO) > 0) {
                    chartMetodosPago.getData().add(new PieChart.Data(String.format("%s (S/ %.2f)", metodo.name(), monto), monto.doubleValue()));
                }
            }

            // 3. Gráfico 3 y Producto Estrella: Top Productos agrupados en BD (evita MultipleBagFetchException)
            chartTopProductos.getData().clear();
            XYChart.Series<String, Number> seriesProductos = new XYChart.Series<>();
            seriesProductos.setName("Unidades Vendidas");

            List<Object[]> topProductos = detalleVentaRepository.findTopProductosEntreFechas(inicio, fin);
            String prodTop = "Ninguno";
            int cantTop = 0;

            int count = 0;
            for (Object[] fila : topProductos) {
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

        } catch (Exception e) {
            e.printStackTrace();
        }
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

            List<ProductoRotacionDTO> ranking = reporteService.obtenerProductosMayorRotacion();
            listaRotacion.setAll(ranking);

            List<Venta> historial = ventaService.listarVentas();
            listaHistorialVentas.setAll(historial);
            aplicarFiltroHistorial();

            handleActualizarDashboard();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
