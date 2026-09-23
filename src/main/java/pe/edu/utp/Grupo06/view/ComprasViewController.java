package pe.edu.utp.Grupo06.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Component
public class ComprasViewController {

    @Autowired
    private ICompraService compraService;

    @Autowired
    private IProveedorService proveedorService;

    @Autowired
    private IProductoService productoService;

    @FXML
    private TextField txtFiltroCompra;

    @FXML
    private DatePicker dpFechaInicio;

    @FXML
    private DatePicker dpFechaFin;

    @FXML
    private Label lblTotalCompras;

    @FXML
    private TableView<Compra> tblCompras;

    @FXML
    private TableColumn<Compra, String> colComprobante;

    @FXML
    private TableColumn<Compra, String> colProveedor;

    @FXML
    private TableColumn<Compra, LocalDateTime> colFecha;

    @FXML
    private TableColumn<Compra, String> colUsuario;

    @FXML
    private TableColumn<Compra, BigDecimal> colTotal;

    @FXML
    private TableColumn<Compra, Void> colAcciones;

    @Autowired
    private pe.edu.utp.Grupo06.repository.DetalleCompraRepository detalleCompraRepository;

    private ObservableList<Compra> listaCompras = FXCollections.observableArrayList();
    private javafx.collections.transformation.FilteredList<Compra> filteredCompras;
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

        // Ordenamiento cronológico real
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaCompra"));
        colFecha.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(formatter));
                }
            }
        });

        colUsuario.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getUsuario() != null ?
                        cellData.getValue().getUsuario().getNombreCompleto() : ""));

        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnVer = new Button("Ver Detalle");

            {
                btnVer.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0284c7; -fx-font-weight: bold; -fx-cursor: hand;");
                btnVer.setOnAction(event -> {
                    Compra c = getTableView().getItems().get(getIndex());
                    mostrarDetalleFactura(c);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox pane = new HBox(btnVer);
                    pane.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(pane);
                }
            }
        });

        filteredCompras = new javafx.collections.transformation.FilteredList<>(listaCompras, p -> true);
        javafx.collections.transformation.SortedList<Compra> sorted = new javafx.collections.transformation.SortedList<>(filteredCompras);
        sorted.comparatorProperty().bind(tblCompras.comparatorProperty());
        tblCompras.setItems(sorted);

        txtFiltroCompra.textProperty().addListener((obs, old, n) -> aplicarFiltroCompras());
    }

    @FXML
    public void handleFiltrarCompras() {
        aplicarFiltroCompras();
    }

    @FXML
    public void handleLimpiarFiltroCompras() {
        txtFiltroCompra.clear();
        dpFechaInicio.setValue(null);
        dpFechaFin.setValue(null);
        aplicarFiltroCompras();
    }

    private void aplicarFiltroCompras() {
        String texto = txtFiltroCompra.getText() != null ? txtFiltroCompra.getText().trim().toLowerCase() : "";
        LocalDate fIni = dpFechaInicio.getValue();
        LocalDate fFin = dpFechaFin.getValue();

        filteredCompras.setPredicate(c -> {
            if (c == null) return false;

            if (!texto.isEmpty()) {
                boolean matchComp = c.getNumeroComprobante() != null && c.getNumeroComprobante().toLowerCase().contains(texto);
                boolean matchProv = c.getProveedor() != null && c.getProveedor().getRazonSocial() != null &&
                        c.getProveedor().getRazonSocial().toLowerCase().contains(texto);
                if (!matchComp && !matchProv) return false;
            }

            if (c.getFechaCompra() != null) {
                LocalDate fComp = c.getFechaCompra().toLocalDate();
                if (fIni != null && fComp.isBefore(fIni)) return false;
                if (fFin != null && fComp.isAfter(fFin)) return false;
            }

            return true;
        });

        BigDecimal total = filteredCompras.stream()
                .map(Compra::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalCompras.setText(String.format("Total Compras: S/ %.2f", total));
    }

    private void mostrarDetalleFactura(Compra c) {
        Dialog<Void> factDialog = new Dialog<>();
        factDialog.setTitle("Comprobante de Compra — " + c.getNumeroComprobante());
        factDialog.setHeaderText(null);

        ButtonType btnOk = new ButtonType("Cerrar", ButtonBar.ButtonData.OK_DONE);
        factDialog.getDialogPane().getButtonTypes().add(btnOk);

        VBox root = new VBox(10);
        root.setStyle("-fx-padding: 15px; -fx-background-color: #ffffff;");
        root.setPrefWidth(550);

        Label lblHeader = new Label(
                "COMPROBANTE DE COMPRA A PROVEEDOR\n" +
                "N° Comprobante: " + c.getNumeroComprobante() + "\n" +
                "Proveedor: " + (c.getProveedor() != null ? c.getProveedor().getRazonSocial() + " (RUC: " + c.getProveedor().getRuc() + ")" : "N/A") + "\n" +
                "Fecha: " + (c.getFechaCompra() != null ? c.getFechaCompra().format(formatter) : "") + "\n" +
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

        Label lblTot = new Label("TOTAL FACTURADO: S/ " + (c.getTotal() != null ? c.getTotal().setScale(2, java.math.RoundingMode.HALF_UP) : "0.00"));
        lblTot.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #15803d; -fx-alignment: CENTER_RIGHT;");

        root.getChildren().addAll(lblHeader, new Separator(), tblDet, lblTot);
        factDialog.getDialogPane().setContent(root);
        factDialog.showAndWait();
    }

    public void cargarCompras() {
        try {
            List<Compra> compras = compraService.listarCompras();
            listaCompras.setAll(compras);
            aplicarFiltroCompras();
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
        tblItems.setPrefHeight(190);

        TableColumn<DetalleCompra, String> colPName = new TableColumn<>("Producto");
        colPName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProducto().getNombre()));
        colPName.setPrefWidth(240);

        TableColumn<DetalleCompra, Integer> colPCant = new TableColumn<>("Cantidad");
        colPCant.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPCant.setPrefWidth(80);

        TableColumn<DetalleCompra, BigDecimal> colPPrice = new TableColumn<>("P. Unit (S/)");
        colPPrice.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colPPrice.setPrefWidth(95);

        TableColumn<DetalleCompra, BigDecimal> colPSub = new TableColumn<>("Subtotal (S/)");
        colPSub.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        colPSub.setPrefWidth(100);

        Label lblTotal = new Label("Total Compra: S/ 0.00");
        lblTotal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0284c7;");

        Runnable actualizarTotal = () -> {
            BigDecimal totalSum = itemsCompra.stream()
                    .map(DetalleCompra::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            lblTotal.setText("Total Compra: S/ " + totalSum.setScale(2, java.math.RoundingMode.HALF_UP));
        };

        TableColumn<DetalleCompra, Void> colPQuitar = new TableColumn<>("Acción");
        colPQuitar.setPrefWidth(85);
        colPQuitar.setStyle("-fx-alignment: CENTER;");
        colPQuitar.setCellFactory(col -> new TableCell<>() {
            private final Button btnQuitar = new Button("❌ Quitar");
            {
                btnQuitar.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4;");
                btnQuitar.setOnAction(e -> {
                    DetalleCompra d = getTableView().getItems().get(getIndex());
                    itemsCompra.remove(d);
                    actualizarTotal.run();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setGraphic(btnQuitar);
                }
            }
        });

        tblItems.getColumns().addAll(colPName, colPCant, colPPrice, colPSub, colPQuitar);

        btnAdd.setOnAction(e -> {
            try {
                Producto prodSel = cbProd.getValue();
                if (prodSel == null) {
                    mostrarAlerta("Seleccione producto", "Debe seleccionar un producto de la lista.");
                    return;
                }
                int cant = spnCant.getValue();
                BigDecimal pu = new BigDecimal(txtPrecio.getText().trim().replace(",", "."));
                if (pu.compareTo(BigDecimal.ZERO) < 0) {
                    mostrarAlerta("Precio inválido", "El precio unitario no puede ser negativo.");
                    return;
                }

                DetalleCompra existente = itemsCompra.stream()
                        .filter(d -> d.getProducto().getId().equals(prodSel.getId()))
                        .findFirst().orElse(null);

                if (existente != null) {
                    existente.setCantidad(existente.getCantidad() + cant);
                    existente.setPrecioUnitario(pu);
                    existente.setSubtotal(pu.multiply(BigDecimal.valueOf(existente.getCantidad())));
                    tblItems.refresh();
                } else {
                    DetalleCompra det = new DetalleCompra();
                    det.setProducto(prodSel);
                    det.setCantidad(cant);
                    det.setPrecioUnitario(pu);
                    det.setSubtotal(pu.multiply(BigDecimal.valueOf(cant)));
                    itemsCompra.add(det);
                }

                actualizarTotal.run();
            } catch (Exception ex) {
                mostrarAlerta("Datos inválidos", "Verifique el precio unitario ingresado (use números decimales válidos).");
            }
        });

        Button btnVaciar = new Button("🗑️ Vaciar Lista");
        btnVaciar.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 4;");
        btnVaciar.setOnAction(e -> {
            itemsCompra.clear();
            actualizarTotal.run();
        });

        HBox botRow = new HBox(15, btnVaciar, new Region(), lblTotal);
        HBox.setHgrow(botRow.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);
        botRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        root.getChildren().addAll(row1, row2, tblItems, botRow);
        dialog.getDialogPane().setContent(root);

        // Prevenir que la ventana se cierre si faltan datos
        Button btnConfirmar = (Button) dialog.getDialogPane().lookupButton(btnGuardar);
        btnConfirmar.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (txtComp.getText().isBlank()) {
                mostrarAlerta("Dato Requerido", "Ingrese el número de comprobante (factura/boleta de compra).");
                event.consume();
                return;
            }
            if (cbProv.getValue() == null) {
                mostrarAlerta("Dato Requerido", "Seleccione la empresa proveedora.");
                event.consume();
                return;
            }
            if (itemsCompra.isEmpty()) {
                mostrarAlerta("Lista Vacía", "Debe añadir al menos un producto a la compra antes de guardar.");
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == btnGuardar) {
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
                mostrarAlerta("Compra Registrada con Éxito", "Se aumentó el inventario de los productos comprados correctamente.");
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
