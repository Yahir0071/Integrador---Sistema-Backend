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
import pe.edu.utp.Grupo06.model.Categoria;
import pe.edu.utp.Grupo06.model.Proveedor;
import pe.edu.utp.Grupo06.model.Rol;
import pe.edu.utp.Grupo06.model.Usuario;
import pe.edu.utp.Grupo06.model.enums.RolNombre;
import pe.edu.utp.Grupo06.repository.RolRepository;
import pe.edu.utp.Grupo06.service.ICategoriaService;
import pe.edu.utp.Grupo06.service.IProveedorService;
import pe.edu.utp.Grupo06.service.IUsuarioService;

import java.util.List;

@Component
public class AdminPanelViewController {

    @Autowired
    private IUsuarioService usuarioService;

    @Autowired
    private ICategoriaService categoriaService;

    @Autowired
    private IProveedorService proveedorService;

    @Autowired
    private RolRepository rolRepository;

    // Tabla Usuarios
    @FXML
    private TableView<Usuario> tblUsuarios;
    @FXML
    private TableColumn<Usuario, Long> colUsuId;
    @FXML
    private TableColumn<Usuario, String> colUsuNombre;
    @FXML
    private TableColumn<Usuario, String> colUsuUsername;
    @FXML
    private TableColumn<Usuario, String> colUsuEmail;
    @FXML
    private TableColumn<Usuario, String> colUsuTelefono;
    @FXML
    private TableColumn<Usuario, String> colUsuRol;
    @FXML
    private TableColumn<Usuario, String> colUsuEstado;

    // Tabla Categorias
    @FXML
    private TableView<Categoria> tblCategorias;
    @FXML
    private TableColumn<Categoria, Long> colCatId;
    @FXML
    private TableColumn<Categoria, String> colCatNombre;
    @FXML
    private TableColumn<Categoria, String> colCatDesc;
    @FXML
    private TableColumn<Categoria, String> colCatEstado;

    // Tabla Proveedores
    @FXML
    private TableView<Proveedor> tblProveedores;
    @FXML
    private TableColumn<Proveedor, String> colProvRuc;
    @FXML
    private TableColumn<Proveedor, String> colProvRazon;
    @FXML
    private TableColumn<Proveedor, String> colProvContacto;
    @FXML
    private TableColumn<Proveedor, String> colProvTelefono;
    @FXML
    private TableColumn<Proveedor, String> colProvEmail;
    @FXML
    private TableColumn<Proveedor, String> colProvEstado;

    private ObservableList<Usuario> listaUsuarios = FXCollections.observableArrayList();
    private ObservableList<Categoria> listaCategorias = FXCollections.observableArrayList();
    private ObservableList<Proveedor> listaProveedores = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTablaUsuarios();
        configurarTablaCategorias();
        configurarTablaProveedores();
        handleRefrescarTodo();
    }

    private void configurarTablaUsuarios() {
        colUsuId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsuNombre.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colUsuUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUsuEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUsuTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colUsuRol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getRol() != null ? c.getValue().getRol().getNombre().name() : "N/A"));
        colUsuEstado.setCellValueFactory(c ->
                new SimpleStringProperty(Boolean.TRUE.equals(c.getValue().getActivo()) ? "Activo" : "Inactivo"));

        tblUsuarios.setItems(listaUsuarios);
    }

    private void configurarTablaCategorias() {
        colCatId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCatNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCatDesc.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colCatEstado.setCellValueFactory(c ->
                new SimpleStringProperty(Boolean.TRUE.equals(c.getValue().getEstado()) ? "Activo" : "Inactivo"));

        tblCategorias.setItems(listaCategorias);
    }

    private void configurarTablaProveedores() {
        colProvRuc.setCellValueFactory(new PropertyValueFactory<>("ruc"));
        colProvRazon.setCellValueFactory(new PropertyValueFactory<>("razonSocial"));
        colProvContacto.setCellValueFactory(new PropertyValueFactory<>("direccion"));
        colProvTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colProvEmail.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colProvEstado.setCellValueFactory(c ->
                new SimpleStringProperty(Boolean.TRUE.equals(c.getValue().getEstado()) ? "Activo" : "Inactivo"));

        tblProveedores.setItems(listaProveedores);
    }

    @FXML
    public void handleRefrescarTodo() {
        try {
            listaUsuarios.setAll(usuarioService.listarTodos());
            listaCategorias.setAll(categoriaService.listarTodas());
            listaProveedores.setAll(proveedorService.listarTodos());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleNuevoUsuario() {
        Dialog<Usuario> dialog = new Dialog<>();
        dialog.setTitle("Crear Nuevo Usuario / Empleado");
        dialog.setHeaderText("Registro de cuenta de acceso al sistema");

        ButtonType btnGuardar = new ButtonType("Guardar Usuario", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        VBox form = new VBox(8);
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Ej: Juan Pérez");
        TextField txtUser = new TextField();
        txtUser.setPromptText("Ej: jperez");
        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Mínimo 6 caracteres");
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("juan@bodega.com");
        TextField txtTel = new TextField();
        txtTel.setPromptText("999888777");

        ComboBox<RolNombre> cbRol = new ComboBox<>();
        cbRol.getItems().addAll(RolNombre.VENDEDOR, RolNombre.ADMINISTRADOR);
        cbRol.setValue(RolNombre.VENDEDOR);

        form.getChildren().addAll(
                new Label("Nombre Completo:"), txtNombre,
                new Label("Usuario (Username):"), txtUser,
                new Label("Contraseña Inicial:"), txtPass,
                new Label("Correo Electrónico:"), txtEmail,
                new Label("Teléfono:"), txtTel,
                new Label("Rol de Acceso:"), cbRol
        );
        dialog.getDialogPane().setContent(form);

        dialog.setResultConverter(btn -> {
            if (btn == btnGuardar) {
                try {
                    Usuario u = new Usuario();
                    u.setNombreCompleto(txtNombre.getText().trim());
                    u.setUsername(txtUser.getText().trim());
                    u.setPassword(txtPass.getText());
                    u.setEmail(txtEmail.getText().trim());
                    u.setTelefono(txtTel.getText().trim());
                    u.setActivo(true);

                    Rol rol = rolRepository.findByNombre(cbRol.getValue())
                            .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
                    u.setRol(rol);
                    return u;
                } catch (Exception ex) {
                    mostrarAlerta("Datos Inválidos", "Complete todos los campos obligatorios.");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(u -> {
            try {
                usuarioService.registrar(u);
                handleRefrescarTodo();
                mostrarAlerta("Usuario Creado", "La cuenta fue registrada con éxito.");
            } catch (Exception ex) {
                mostrarAlerta("Error al registrar", ex.getMessage());
            }
        });
    }

    @FXML
    public void handleNuevaCategoria() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nueva Categoría");
        dialog.setHeaderText("Registro de categoría de productos");
        dialog.setContentText("Nombre de la categoría:");

        dialog.showAndWait().ifPresent(nom -> {
            if (!nom.isBlank()) {
                try {
                    Categoria c = new Categoria();
                    c.setNombre(nom.trim());
                    c.setDescripcion("Categoría registrada desde el panel de administración");
                    c.setEstado(true);
                    categoriaService.registrar(c);
                    handleRefrescarTodo();
                    mostrarAlerta("Categoría Creada", "Categoría '" + nom + "' registrada con éxito.");
                } catch (Exception ex) {
                    mostrarAlerta("Error al registrar", ex.getMessage());
                }
            }
        });
    }

    @FXML
    public void handleNuevoProveedor() {
        Dialog<Proveedor> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Proveedor");
        dialog.setHeaderText("Registro de empresa proveedora de mercadería");

        ButtonType btnGuardar = new ButtonType("Guardar Proveedor", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        VBox form = new VBox(8);
        TextField txtRuc = new TextField();
        txtRuc.setPromptText("11 dígitos (Ej: 20123456789)");
        TextField txtRazon = new TextField();
        txtRazon.setPromptText("Razón Social (Ej: Gloria S.A.)");
        TextField txtDir = new TextField();
        txtDir.setPromptText("Dirección comercial");
        TextField txtTel = new TextField();
        txtTel.setPromptText("Teléfono");
        TextField txtCorreo = new TextField();
        txtCorreo.setPromptText("ventas@empresa.com");

        form.getChildren().addAll(
                new Label("RUC:"), txtRuc,
                new Label("Razón Social:"), txtRazon,
                new Label("Dirección:"), txtDir,
                new Label("Teléfono:"), txtTel,
                new Label("Correo Electrónico:"), txtCorreo
        );
        dialog.getDialogPane().setContent(form);

        dialog.setResultConverter(btn -> {
            if (btn == btnGuardar) {
                try {
                    Proveedor p = new Proveedor();
                    p.setRuc(txtRuc.getText().trim());
                    p.setRazonSocial(txtRazon.getText().trim());
                    p.setDireccion(txtDir.getText().trim());
                    p.setTelefono(txtTel.getText().trim());
                    p.setCorreo(txtCorreo.getText().trim());
                    p.setEstado(true);
                    return p;
                } catch (Exception ex) {
                    mostrarAlerta("Datos Inválidos", "Complete los campos correctamente.");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(p -> {
            try {
                proveedorService.registrar(p);
                handleRefrescarTodo();
                mostrarAlerta("Proveedor Creado", "Proveedor '" + p.getRazonSocial() + "' registrado.");
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
