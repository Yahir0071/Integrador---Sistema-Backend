package pe.edu.utp.Grupo06.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pe.edu.utp.Grupo06.model.Categoria;
import pe.edu.utp.Grupo06.model.Producto;
import pe.edu.utp.Grupo06.model.enums.UnidadMedida;
import pe.edu.utp.Grupo06.service.ICategoriaService;
import pe.edu.utp.Grupo06.service.IProductoService;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ProductosViewController {

    @Autowired
    private IProductoService productoService;

    @Autowired
    private ICategoriaService categoriaService;

    @FXML
    private TextField txtBuscar;

    @FXML
    private ComboBox<Categoria> cbCategoriaFiltro;

    @FXML
    private CheckBox chkSoloBajoStock;

    @FXML
    private TableView<Producto> tblProductos;

    @FXML
    private TableColumn<Producto, String> colCodigo;

    @FXML
    private TableColumn<Producto, String> colNombre;

    @FXML
    private TableColumn<Producto, String> colCategoria;

    @FXML
    private TableColumn<Producto, BigDecimal> colPrecioCompra;

    @FXML
    private TableColumn<Producto, BigDecimal> colPrecioVenta;

    @FXML
    private TableColumn<Producto, Integer> colStockActual;

    @FXML
    private TableColumn<Producto, Integer> colStockMinimo;

    @FXML
    private TableColumn<Producto, String> colEstadoStock;

    @FXML
    private TableColumn<Producto, Void> colAcciones;

    private ObservableList<Producto> listaProductos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarColumnas();
        cargarCategoriasFiltro();
        cargarProductos();

        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> filtrarProductos());
        cbCategoriaFiltro.valueProperty().addListener((obs, oldVal, newVal) -> filtrarProductos());
    }

    private void configurarColumnas() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCategoria() != null ?
                        cellData.getValue().getCategoria().getNombre() : ""));

        colPrecioCompra.setCellValueFactory(new PropertyValueFactory<>("precioCompra"));
        colPrecioVenta.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colStockActual.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        colStockMinimo.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));

        // Badge para estado de stock
        colEstadoStock.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Producto p = getTableRow().getItem();
                    Label badge = new Label();
                    if (p.getStockActual() <= p.getStockMinimo()) {
                        badge.setText("⚠️ Reponer (" + p.getStockActual() + ")");
                        badge.getStyleClass().add("badge-danger");
                    } else {
                        badge.setText("✅ Óptimo");
                        badge.getStyleClass().add("badge-success");
                    }
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Botón de eliminar / editar en acciones
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEliminar = new Button("🗑️");

            {
                btnEliminar.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-cursor: hand;");
                btnEliminar.setOnAction(event -> {
                    Producto p = getTableView().getItems().get(getIndex());
                    eliminarProducto(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox pane = new HBox(btnEliminar);
                    pane.setAlignment(Pos.CENTER);
                    setGraphic(pane);
                }
            }
        });
    }

    public void cargarProductos() {
        try {
            List<Producto> productos = productoService.listarActivos();
            listaProductos.setAll(productos);
            tblProductos.setItems(listaProductos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarCategoriasFiltro() {
        try {
            List<Categoria> categorias = categoriaService.listarActivas();
            cbCategoriaFiltro.getItems().setAll(categorias);
            
            javafx.util.StringConverter<Categoria> converter = new javafx.util.StringConverter<>() {
                @Override
                public String toString(Categoria c) {
                    return c != null ? c.getNombre() : "";
                }

                @Override
                public Categoria fromString(String string) {
                    return null;
                }
            };
            cbCategoriaFiltro.setConverter(converter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleFiltroBajoStock() {
        if (chkSoloBajoStock.isSelected()) {
            List<Producto> bajoStock = productoService.listarConBajoStock();
            listaProductos.setAll(bajoStock);
            tblProductos.setItems(listaProductos);
        } else {
            cargarProductos();
        }
    }

    private void filtrarProductos() {
        String texto = txtBuscar.getText() != null ? txtBuscar.getText().toLowerCase().trim() : "";
        Categoria categoriaSel = cbCategoriaFiltro.getValue();

        List<Producto> filtrados = productoService.listarActivos().stream()
                .filter(p -> texto.isEmpty() ||
                        p.getNombre().toLowerCase().contains(texto) ||
                        p.getCodigo().toLowerCase().contains(texto))
                .filter(p -> categoriaSel == null ||
                        (p.getCategoria() != null && p.getCategoria().getId().equals(categoriaSel.getId())))
                .toList();

        listaProductos.setAll(filtrados);
        tblProductos.setItems(listaProductos);
    }

    @FXML
    public void handleNuevoProducto() {
        Dialog<Producto> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Producto");
        dialog.setHeaderText("Registrar un nuevo producto en el catálogo");

        ButtonType btnGuardarType = new ButtonType("Guardar Producto", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardarType, ButtonType.CANCEL);

        VBox form = new VBox(10);
        
        // Generar sugerencia de código correlativo automático (RF01)
        long totalProds = productoService.listarTodos().size() + 1;
        String codigoSugerido = String.format("PROD-%04d", totalProds);

        TextField txtCod = new TextField(codigoSugerido);
        TextField txtNom = new TextField();
        txtNom.setPromptText("Nombre del producto");
        TextField txtPCompra = new TextField();
        txtPCompra.setPromptText("0.00");
        TextField txtPVenta = new TextField();
        txtPVenta.setPromptText("0.00");
        TextField txtStockMin = new TextField("5");
        
        ComboBox<Categoria> cbCat = new ComboBox<>();
        cbCat.getItems().setAll(categoriaService.listarActivas());
        cbCat.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Categoria c) {
                return c != null ? c.getNombre() : "";
            }

            @Override
            public Categoria fromString(String string) {
                return null;
            }
        });
        if (!cbCat.getItems().isEmpty()) {
            cbCat.setValue(cbCat.getItems().get(0));
        }

        Button btnNuevaCat = new Button("➕ Crear Categoría");
        btnNuevaCat.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0284c7; -fx-cursor: hand;");
        btnNuevaCat.setOnAction(e -> {
            TextInputDialog catDialog = new TextInputDialog();
            catDialog.setTitle("Nueva Categoría");
            catDialog.setHeaderText("Crear categoría de productos");
            catDialog.setContentText("Nombre:");
            catDialog.showAndWait().ifPresent(nombreCat -> {
                if (!nombreCat.isBlank()) {
                    Categoria nuevaC = new Categoria();
                    nuevaC.setNombre(nombreCat.trim());
                    nuevaC.setDescripcion("Creada desde catálogo");
                    categoriaService.registrar(nuevaC);
                    cbCat.getItems().setAll(categoriaService.listarActivas());
                    cbCat.setValue(nuevaC);
                    cargarCategoriasFiltro();
                }
            });
        });

        HBox catBox = new HBox(8, cbCat, btnNuevaCat);
        catBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<UnidadMedida> cbUnidad = new ComboBox<>();
        cbUnidad.getItems().setAll(UnidadMedida.values());
        cbUnidad.setValue(UnidadMedida.UNIDAD);

        form.getChildren().addAll(
                new Label("Código (Autogenerado / Editable):"), txtCod,
                new Label("Nombre del Producto:"), txtNom,
                new Label("Categoría:"), catBox,
                new Label("Precio Compra (S/):"), txtPCompra,
                new Label("Precio Venta (S/):"), txtPVenta,
                new Label("Stock Mínimo de Seguridad:"), txtStockMin,
                new Label("Unidad de Medida:"), cbUnidad
        );

        dialog.getDialogPane().setContent(form);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardarType) {
                try {
                    Producto p = new Producto();
                    p.setCodigo(txtCod.getText().trim());
                    p.setNombre(txtNom.getText().trim());
                    p.setPrecioCompra(new BigDecimal(txtPCompra.getText().trim()));
                    p.setPrecioVenta(new BigDecimal(txtPVenta.getText().trim()));
                    p.setStockActual(0);
                    p.setStockMinimo(Integer.parseInt(txtStockMin.getText().trim()));
                    p.setCategoria(cbCat.getValue());
                    p.setUnidadMedida(cbUnidad.getValue());
                    p.setEstado(true);
                    return p;
                } catch (Exception e) {
                    mostrarAlertaError("Error de validación", "Verifique que los campos numéricos y obligatorios sean válidos.");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(nuevo -> {
            try {
                productoService.registrar(nuevo);
                cargarProductos();
            } catch (Exception ex) {
                mostrarAlertaError("Error al guardar", ex.getMessage());
            }
        });
    }

    private void eliminarProducto(Producto p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText("¿Seguro que desea dar de baja al producto " + p.getNombre() + "?");
        confirm.setContentText("El producto pasará a estado inactivo (eliminación lógica).");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                productoService.eliminar(p.getId());
                cargarProductos();
            }
        });
    }

    private void mostrarAlertaError(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
}
