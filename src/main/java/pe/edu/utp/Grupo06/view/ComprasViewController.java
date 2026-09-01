package pe.edu.utp.Grupo06.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pe.edu.utp.Grupo06.model.Compra;
import pe.edu.utp.Grupo06.model.DetalleCompra;
import pe.edu.utp.Grupo06.model.Producto;
import pe.edu.utp.Grupo06.model.Proveedor;
import pe.edu.utp.Grupo06.service.ICompraService;
import pe.edu.utp.Grupo06.service.IProductoService;
import pe.edu.utp.Grupo06.service.IProveedorService;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ComprasViewController {

    @Autowired
    private ICompraService compraService;

    @Autowired
    private IProveedorService proveedorService;

    @Autowired
    private IProductoService productoService;

    @FXML
    private TableView<Compra> tblCompras;

    @FXML
    private TableColumn<Compra, String> colComprobante;

    @FXML
    private TableColumn<Compra, String> colProveedor;

    @FXML
    private TableColumn<Compra, String> colFecha;

    @FXML
    private TableColumn<Compra, String> colUsuario;

    @FXML
    private TableColumn<Compra, BigDecimal> colTotal;

    private ObservableList<Compra> listaCompras = FXCollections.observableArrayList();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        configurarColumnas();
        cargarCompras();
    }

    private void configurarColumnas() {
        colComprobante.setCellValueFactory(new PropertyValueFactory<>("numeroComprobante"));
        colProveedor.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getProveedor() != null ?
                        cellData.getValue().getProveedor().getRazonSocial() : ""));

        colFecha.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFechaCompra() != null ?
                        cellData.getValue().getFechaCompra().format(formatter) : ""));

        colUsuario.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getUsuario() != null ?
                        cellData.getValue().getUsuario().getNombreCompleto() : ""));

        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        tblCompras.setItems(listaCompras);
    }

    public void cargarCompras() {
        try {
            List<Compra> compras = compraService.listarCompras();
            listaCompras.setAll(compras);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleNuevaCompra() {
        Dialog<Compra> dialog = new Dialog<>();
        dialog.setTitle("Nueva Compra a Proveedor");
        dialog.setHeaderText("Ingreso de mercadería (aumentará el stock del producto automáticamente)");

        ButtonType btnGuardar = new ButtonType("Registrar Compra", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        VBox form = new VBox(10);
        TextField txtComp = new TextField();
        txtComp.setPromptText("Ej: F001-000456");

        ComboBox<Proveedor> cbProv = new ComboBox<>();
        cbProv.getItems().setAll(proveedorService.listarActivos());

        ComboBox<Producto> cbProd = new ComboBox<>();
        cbProd.getItems().setAll(productoService.listarActivos());

        TextField txtCantidad = new TextField("10");
        TextField txtPrecioUnit = new TextField();
        txtPrecioUnit.setPromptText("Precio unitario de compra");

        cbProd.setOnAction(e -> {
            if (cbProd.getValue() != null) {
                txtPrecioUnit.setText(cbProd.getValue().getPrecioCompra().toString());
            }
        });

        form.getChildren().addAll(
                new Label("N° Comprobante / Factura:"), txtComp,
                new Label("Proveedor:"), cbProv,
                new Label("Producto a Abastecer:"), cbProd,
                new Label("Cantidad:"), txtCantidad,
                new Label("Precio Unitario (S/):"), txtPrecioUnit
        );

        dialog.getDialogPane().setContent(form);

        dialog.setResultConverter(btn -> {
            if (btn == btnGuardar) {
                try {
                    Compra compra = new Compra();
                    compra.setNumeroComprobante(txtComp.getText().trim());
                    compra.setProveedor(cbProv.getValue());
                    compra.setUsuario(LoginViewController.getUsuarioSesion());

                    DetalleCompra detalle = new DetalleCompra();
                    detalle.setProducto(cbProd.getValue());
                    detalle.setCantidad(Integer.parseInt(txtCantidad.getText().trim()));
                    detalle.setPrecioUnitario(new BigDecimal(txtPrecioUnit.getText().trim()));
                    compra.setDetalles(List.of(detalle));

                    return compra;
                } catch (Exception ex) {
                    mostrarAlerta("Error de datos", "Complete todos los campos correctamente.");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(c -> {
            try {
                compraService.registrarCompra(c);
                cargarCompras();
                mostrarAlerta("Compra Registrada", "Stock del producto actualizado con éxito.");
            } catch (Exception ex) {
                mostrarAlerta("Error al registrar", ex.getMessage());
            }
        });
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
}
