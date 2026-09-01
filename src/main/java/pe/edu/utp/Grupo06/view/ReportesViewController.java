package pe.edu.utp.Grupo06.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pe.edu.utp.Grupo06.dto.reporte.ResumenInventarioDTO;
import pe.edu.utp.Grupo06.dto.venta.ProductoRotacionDTO;
import pe.edu.utp.Grupo06.model.Venta;
import pe.edu.utp.Grupo06.service.IReporteService;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ReportesViewController {

    @Autowired
    private IReporteService reporteService;

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

    @Autowired
    private pe.edu.utp.Grupo06.service.IVentaService ventaService;

    @Autowired
    private pe.edu.utp.Grupo06.repository.DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private pe.edu.utp.Grupo06.repository.PagoRepository pagoRepository;

    @FXML
    private TableView<Venta> tblHistorialVentas;

    @FXML
    private TableColumn<Venta, String> colVenTicket;

    @FXML
    private TableColumn<Venta, String> colVenFecha;

    @FXML
    private TableColumn<Venta, String> colVenCajero;

    @FXML
    private TableColumn<Venta, BigDecimal> colVenTotal;

    @FXML
    private TableColumn<Venta, String> colVenEstado;

    @FXML
    private TableColumn<Venta, Void> colVenAccion;

    private ObservableList<ProductoRotacionDTO> listaRotacion = FXCollections.observableArrayList();
    private ObservableList<Venta> listaHistorialVentas = FXCollections.observableArrayList();
    private java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML
    public void initialize() {
        colRotCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colRotNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colRotVendidos.setCellValueFactory(new PropertyValueFactory<>("cantidadTotalVendida"));
        colRotRecaudado.setCellValueFactory(new PropertyValueFactory<>("totalRecaudado"));

        tblRotacion.setItems(listaRotacion);

        configurarTablaVentas();
        cargarReportes();
    }

    private void configurarTablaVentas() {
        colVenTicket.setCellValueFactory(new PropertyValueFactory<>("numeroTicket"));
        colVenFecha.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getFechaVenta() != null ? c.getValue().getFechaVenta().format(dtf) : ""));
        colVenCajero.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getUsuario() != null ? c.getValue().getUsuario().getNombreCompleto() : ""));
        colVenTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colVenEstado.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getEstado() != null ? c.getValue().getEstado().name() : ""));

        colVenAccion.setCellFactory(param -> new javafx.scene.control.TableCell<>() {
            private final javafx.scene.control.Button btnVerTicket = new javafx.scene.control.Button("Ver Ticket");

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
                    javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(btnVerTicket);
                    pane.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(pane);
                }
            }
        });

        tblHistorialVentas.setItems(listaHistorialVentas);
    }

    private void mostrarTicketHistorico(Venta v) {
        javafx.scene.control.Dialog<Void> boletaDialog = new javafx.scene.control.Dialog<>();
        boletaDialog.setTitle("Ticket de Venta — " + v.getNumeroTicket());

        javafx.scene.control.ButtonType btnCerrar = new javafx.scene.control.ButtonType("Cerrar", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        boletaDialog.getDialogPane().getButtonTypes().add(btnCerrar);

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10);
        root.setStyle("-fx-font-family: 'Courier New', monospace; -fx-padding: 15px; -fx-background-color: #ffffff;");
        root.setPrefWidth(380);

        javafx.scene.control.Label lblCabecera = new javafx.scene.control.Label(
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

        List<pe.edu.utp.Grupo06.model.DetalleVenta> detalles = detalleVentaRepository.findByVentaId(v.getId());
        StringBuilder sbItems = new StringBuilder();
        for (pe.edu.utp.Grupo06.model.DetalleVenta d : detalles) {
            String nom = d.getProducto().getNombre();
            if (nom.length() > 20) nom = nom.substring(0, 17) + "...";
            sbItems.append(String.format("%-4d  %-22s %5.2f  %6.2f\n",
                    d.getCantidad(), nom, d.getPrecioUnitario(), d.getSubtotal()));
        }

        javafx.scene.control.Label lblItems = new javafx.scene.control.Label(sbItems.toString());

        List<pe.edu.utp.Grupo06.model.Pago> pagos = pagoRepository.findByVentaId(v.getId());
        StringBuilder sbPagos = new StringBuilder();
        sbPagos.append("------------------------------------------\n");
        sbPagos.append(String.format("TOTAL DE LA VENTA:              S/ %7.2f\n", v.getTotal()));
        sbPagos.append("------------------------------------------\n");
        sbPagos.append("MÉTODOS DE PAGO:\n");
        for (pe.edu.utp.Grupo06.model.Pago p : pagos) {
            sbPagos.append(String.format(" - %-10s:                    S/ %7.2f\n", p.getMetodoPago(), p.getMonto()));
        }
        sbPagos.append("==========================================\n");
        sbPagos.append("       ESTADO: " + v.getEstado() + "\n");
        sbPagos.append("==========================================");

        javafx.scene.control.Label lblPie = new javafx.scene.control.Label(sbPagos.toString());

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
            lblValorizacion.setText("S/ " + resumen.getValorizacionTotalInventario().setScale(2, java.math.RoundingMode.HALF_UP).toString());

            List<ProductoRotacionDTO> ranking = reporteService.obtenerProductosMayorRotacion();
            listaRotacion.setAll(ranking);

            List<Venta> historial = ventaService.listarVentas();
            listaHistorialVentas.setAll(historial);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
