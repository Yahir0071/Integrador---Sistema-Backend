package pe.edu.utp.Grupo06.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pe.edu.utp.Grupo06.model.DetalleVenta;
import pe.edu.utp.Grupo06.model.Pago;
import pe.edu.utp.Grupo06.model.Producto;
import pe.edu.utp.Grupo06.model.Venta;
import pe.edu.utp.Grupo06.model.enums.MetodoPago;
import pe.edu.utp.Grupo06.service.IProductoService;
import pe.edu.utp.Grupo06.service.IVentaService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class VentasPosViewController {

    @Autowired
    private IProductoService productoService;

    @Autowired
    private IVentaService ventaService;

    @FXML
    private TextField txtBuscarProducto;

    @FXML
    private TableView<Producto> tblCatalogo;

    @FXML
    private TableColumn<Producto, String> colCatCodigo;

    @FXML
    private TableColumn<Producto, String> colCatNombre;

    @FXML
    private TableColumn<Producto, BigDecimal> colCatPrecio;

    @FXML
    private TableColumn<Producto, Integer> colCatStock;

    @FXML
    private TableView<DetalleVenta> tblCarrito;

    @FXML
    private TableColumn<DetalleVenta, String> colCarProducto;

    @FXML
    private TableColumn<DetalleVenta, Integer> colCarCant;

    @FXML
    private TableColumn<DetalleVenta, BigDecimal> colCarPrecio;

    @FXML
    private TableColumn<DetalleVenta, BigDecimal> colCarSubtotal;

    @FXML
    private Label lblTotalVenta;

    @FXML
    private TextField txtMontoEfectivo;

    @FXML
    private TextField txtMontoYape;

    @FXML
    private TextField txtMontoPlin;

    private ObservableList<Producto> listaCatalogo = FXCollections.observableArrayList();
    private ObservableList<DetalleVenta> listaCarrito = FXCollections.observableArrayList();
    private BigDecimal totalVenta = BigDecimal.ZERO;

    @FXML
    public void initialize() {
        configurarTablas();
        cargarCatalogo();

        txtBuscarProducto.textProperty().addListener((obs, oldVal, newVal) -> filtrarCatalogo(newVal));
    }

    private void configurarTablas() {
        // Catálogo
        colCatCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colCatNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCatPrecio.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colCatStock.setCellValueFactory(new PropertyValueFactory<>("stockActual"));

        // Carrito
        colCarProducto.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getProducto().getNombre()));
        colCarCant.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colCarPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colCarSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        tblCarrito.setItems(listaCarrito);
    }

    private void cargarCatalogo() {
        try {
            listaCatalogo.setAll(productoService.listarActivos());
            tblCatalogo.setItems(listaCatalogo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void filtrarCatalogo(String texto) {
        if (texto == null || texto.isBlank()) {
            tblCatalogo.setItems(listaCatalogo);
            return;
        }
        String query = texto.toLowerCase().trim();
        List<Producto> filtrados = listaCatalogo.stream()
                .filter(p -> p.getNombre().toLowerCase().contains(query) || p.getCodigo().toLowerCase().contains(query))
                .toList();
        tblCatalogo.setItems(FXCollections.observableArrayList(filtrados));
    }

    @FXML
    public void handleAnadirAlCarrito() {
        Producto seleccionado = tblCatalogo.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Selección requerida", "Seleccione un producto de la tabla para añadirlo.");
            return;
        }

        if (seleccionado.getStockActual() <= 0) {
            mostrarAlerta(Alert.AlertType.ERROR, "Sin Stock", "El producto seleccionado no tiene unidades disponibles.");
            return;
        }

        // Buscar si ya está en el carrito
        DetalleVenta existente = listaCarrito.stream()
                .filter(d -> d.getProducto().getId().equals(seleccionado.getId()))
                .findFirst()
                .orElse(null);

        if (existente != null) {
            if (existente.getCantidad() + 1 > seleccionado.getStockActual()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Stock Límite", "No puede agregar más unidades que las disponibles en stock (" + seleccionado.getStockActual() + ").");
                return;
            }
            existente.setCantidad(existente.getCantidad() + 1);
            existente.setSubtotal(existente.getPrecioUnitario().multiply(BigDecimal.valueOf(existente.getCantidad())));
            tblCarrito.refresh();
        } else {
            DetalleVenta nuevo = new DetalleVenta();
            nuevo.setProducto(seleccionado);
            nuevo.setCantidad(1);
            nuevo.setPrecioUnitario(seleccionado.getPrecioVenta());
            nuevo.setSubtotal(seleccionado.getPrecioVenta());
            listaCarrito.add(nuevo);
        }

        recalcularTotal();
    }

    private void recalcularTotal() {
        totalVenta = listaCarrito.stream()
                .map(DetalleVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        lblTotalVenta.setText("S/ " + totalVenta.setScale(2, java.math.RoundingMode.HALF_UP).toString());
        txtMontoEfectivo.setText(totalVenta.setScale(2, java.math.RoundingMode.HALF_UP).toString());
        txtMontoYape.setText("0.00");
        txtMontoPlin.setText("0.00");
    }

    private BigDecimal parsearMonto(String texto) {
        if (texto == null || texto.isBlank()) return BigDecimal.ZERO;
        String limpio = texto.trim().replace(",", ".");
        return new BigDecimal(limpio);
    }

    @FXML
    public void handleLimpiarCarrito() {
        listaCarrito.clear();
        recalcularTotal();
    }

    @FXML
    public void handleEmitirVenta() {
        if (listaCarrito.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Carrito Vacío", "Añada al menos un producto para registrar la venta.");
            return;
        }

        try {
            BigDecimal ef = parsearMonto(txtMontoEfectivo.getText());
            BigDecimal yap = parsearMonto(txtMontoYape.getText());
            BigDecimal pli = parsearMonto(txtMontoPlin.getText());

            BigDecimal sumaPagos = ef.add(yap).add(pli);
            if (sumaPagos.compareTo(totalVenta) != 0) {
                mostrarAlerta(Alert.AlertType.ERROR, "Pagos Descuadrados",
                        "La suma de los métodos de pago (S/ " + sumaPagos + ") debe ser exactamente igual al total de la venta (S/ " + totalVenta + ").");
                return;
            }

            Venta venta = new Venta();
            venta.setNumeroTicket("TCK-" + System.currentTimeMillis() % 1000000);
            venta.setUsuario(LoginViewController.getUsuarioSesion());

            List<DetalleVenta> detalles = new ArrayList<>(listaCarrito);
            venta.setDetalles(detalles);

            List<Pago> pagos = new ArrayList<>();
            if (ef.compareTo(BigDecimal.ZERO) > 0) {
                Pago p = new Pago();
                p.setMetodoPago(MetodoPago.EFECTIVO);
                p.setMonto(ef);
                pagos.add(p);
            }
            if (yap.compareTo(BigDecimal.ZERO) > 0) {
                Pago p = new Pago();
                p.setMetodoPago(MetodoPago.YAPE);
                p.setMonto(yap);
                pagos.add(p);
            }
            if (pli.compareTo(BigDecimal.ZERO) > 0) {
                Pago p = new Pago();
                p.setMetodoPago(MetodoPago.PLIN);
                p.setMonto(pli);
                pagos.add(p);
            }
            venta.setPagos(pagos);

            Venta emitida = ventaService.registrarVenta(venta);

            mostrarAlerta(Alert.AlertType.INFORMATION, "¡Venta Exitosa!",
                    "Venta registrada correctamente.\nTicket: " + emitida.getNumeroTicket() + "\nTotal: S/ " + emitida.getTotal());

            handleLimpiarCarrito();
            cargarCatalogo();
        } catch (Exception ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al procesar la venta", ex.getMessage());
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String contenido) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
}
