package pe.edu.utp.Grupo06.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
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
        dialog.setTitle("Registrar Factura / Boleta de Compra");
        dialog.setHeaderText("Ingreso de mercadería al almacén por proveedor");

        ButtonType btnGuardar = new ButtonType("💾 Confirmar Compra", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        VBox root = new VBox(12);
        root.setPrefWidth(680);
        root.setStyle("-fx-padding: 10px;");

        // Fila 1: Comprobante y Proveedor
        HBox row1 = new HBox(15);
        VBox boxComp = new VBox(4, new Label("N° Comprobante:"), new TextField());
        TextField txtComp = (TextField) boxComp.getChildren().get(1);
        txtComp.setPromptText("Ej: F001-000456");
        txtComp.setPrefWidth(220);

        ComboBox<Proveedor> cbProv = new ComboBox<>();
        cbProv.getItems().setAll(proveedorService.listarActivos());
        cbProv.setPrefWidth(350);
        cbProv.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Proveedor p) {
                return p != null ? p.getRazonSocial() + " (RUC: " + p.getRuc() + ")" : "";
            }

            @Override
            public Proveedor fromString(String s) {
                return null;
            }
        });
        if (!cbProv.getItems().isEmpty()) cbProv.setValue(cbProv.getItems().get(0));

        VBox boxProv = new VBox(4, new Label("Proveedor:"), cbProv);
        row1.getChildren().addAll(boxComp, boxProv);

        // Fila 2: Selector de Producto a añadir a la compra
        HBox row2 = new HBox(10);
        row2.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);
        row2.setStyle("-fx-background-color: #f1f5f9; -fx-padding: 10px; -fx-background-radius: 6px;");

        ComboBox<Producto> cbProd = new ComboBox<>();
        cbProd.getItems().setAll(productoService.listarActivos());
        cbProd.setPrefWidth(240);
        cbProd.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Producto p) {
                return p != null ? p.getNombre() : "";
            }

            @Override
            public Producto fromString(String s) {
                return null;
            }
        });
        if (!cbProd.getItems().isEmpty()) cbProd.setValue(cbProd.getItems().get(0));

        Spinner<Integer> spnCant = new Spinner<>(1, 1000, 10);
        spnCant.setEditable(true);
        spnCant.setPrefWidth(85);

        TextField txtPrecio = new TextField();
        txtPrecio.setPrefWidth(95);
        txtPrecio.setPromptText("P. Compra");

        cbProd.setOnAction(e -> {
            if (cbProd.getValue() != null) {
                txtPrecio.setText(cbProd.getValue().getPrecioCompra().setScale(2, java.math.RoundingMode.HALF_UP).toString());
            }
        });
        if (cbProd.getValue() != null) {
            txtPrecio.setText(cbProd.getValue().getPrecioCompra().setScale(2, java.math.RoundingMode.HALF_UP).toString());
        }

        Button btnAdd = new Button("➕ Añadir Ítem");
        btnAdd.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        row2.getChildren().addAll(
                new VBox(3, new Label("Producto:"), cbProd),
                new VBox(3, new Label("Cantidad:"), spnCant),
                new VBox(3, new Label("P. Unit (S/):"), txtPrecio),
                btnAdd
        );

        // Tabla de ítems incluidos en esta compra
        ObservableList<DetalleCompra> itemsCompra = FXCollections.observableArrayList();
        TableView<DetalleCompra> tblItems = new TableView<>(itemsCompra);
        tblItems.setPrefHeight(180);

        TableColumn<DetalleCompra, String> colPName = new TableColumn<>("Producto");
        colPName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProducto().getNombre()));
        colPName.setPrefWidth(280);

        TableColumn<DetalleCompra, Integer> colPCant = new TableColumn<>("Cantidad");
        colPCant.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPCant.setPrefWidth(90);

        TableColumn<DetalleCompra, BigDecimal> colPPrice = new TableColumn<>("P. Unit (S/)");
        colPPrice.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colPPrice.setPrefWidth(100);

        TableColumn<DetalleCompra, BigDecimal> colPSub = new TableColumn<>("Subtotal (S/)");
        colPSub.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        colPSub.setPrefWidth(110);

        tblItems.getColumns().addAll(colPName, colPCant, colPPrice, colPSub);

        Label lblTotal = new Label("Total Compra: S/ 0.00");
        lblTotal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0284c7; -fx-alignment: CENTER_RIGHT;");

        btnAdd.setOnAction(e -> {
            try {
                Producto prodSel = cbProd.getValue();
                int cant = spnCant.getValue();
                BigDecimal pu = new BigDecimal(txtPrecio.getText().trim().replace(",", "."));
                
                DetalleCompra det = new DetalleCompra();
                det.setProducto(prodSel);
                det.setCantidad(cant);
                det.setPrecioUnitario(pu);
                det.setSubtotal(pu.multiply(BigDecimal.valueOf(cant)));
                
                itemsCompra.add(det);
                
                BigDecimal totalSum = itemsCompra.stream()
                        .map(DetalleCompra::getSubtotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                lblTotal.setText("Total Compra: S/ " + totalSum.setScale(2, java.math.RoundingMode.HALF_UP));
            } catch (Exception ex) {
                mostrarAlerta("Datos inválidos", "Verifique el precio unitario ingresado.");
            }
        });

        root.getChildren().addAll(row1, row2, tblItems, lblTotal);
        dialog.getDialogPane().setContent(root);

        dialog.setResultConverter(btn -> {
            if (btn == btnGuardar) {
                if (txtComp.getText().isBlank() || itemsCompra.isEmpty()) {
                    mostrarAlerta("Datos incompletos", "Ingrese el número de comprobante y al menos un producto a comprar.");
                    return null;
                }
                Compra compra = new Compra();
                compra.setNumeroComprobante(txtComp.getText().trim());
                compra.setProveedor(cbProv.getValue());
                compra.setUsuario(LoginViewController.getUsuarioSesion());
                compra.setDetalles(new java.util.ArrayList<>(itemsCompra));
                return compra;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(c -> {
            try {
                compraService.registrarCompra(c);
                cargarCompras();
                mostrarAlerta("Compra Registrada con Éxito", "Se aumentó el inventario de los productos comprados.");
            } catch (Exception ex) {
                mostrarAlerta("Error al registrar compra", ex.getMessage());
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
