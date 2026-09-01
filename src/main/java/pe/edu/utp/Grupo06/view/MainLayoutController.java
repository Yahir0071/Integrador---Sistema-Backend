package pe.edu.utp.Grupo06.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Component;
import pe.edu.utp.Grupo06.model.Usuario;

@Component
public class MainLayoutController {

    @FXML
    private Label lblUsuarioNombre;

    @FXML
    private Label lblUsuarioRol;

    @FXML
    private StackPane contentArea;

    @FXML
    private Button btnPos;

    @FXML
    private Button btnProductos;

    @FXML
    private Button btnAlertas;

    @FXML
    private Button btnCompras;

    @FXML
    private Button btnReportes;

    @FXML
    public void initialize() {
        Usuario usuario = LoginViewController.getUsuarioSesion();
        if (usuario != null) {
            lblUsuarioNombre.setText(usuario.getNombreCompleto());
            lblUsuarioRol.setText("Rol: " + (usuario.getRol() != null ? usuario.getRol().getNombre().name() : "N/A"));

            // Restricciones por Rol (RNF03):
            // Si es VENDEDOR, solo accede a Punto de Venta e Inventario;
            // Ocultamos Compras y Reportes gerenciales (solo para ADMINISTRADOR).
            boolean esAdmin = usuario.getRol() != null && usuario.getRol().getNombre() == pe.edu.utp.Grupo06.model.enums.RolNombre.ADMINISTRADOR;
            
            btnCompras.setVisible(esAdmin);
            btnCompras.setManaged(esAdmin);
            
            btnReportes.setVisible(esAdmin);
            btnReportes.setManaged(esAdmin);
        }

        // Cargar vista inicial
        mostrarPos();
    }

    @FXML
    public void mostrarPos() {
        activarBoton(btnPos);
        cargarVista("/fxml/ventas_pos.fxml");
    }

    @FXML
    public void mostrarProductos() {
        activarBoton(btnProductos);
        cargarVista("/fxml/productos.fxml");
    }

    @FXML
    public void mostrarAlertas() {
        activarBoton(btnAlertas);
        cargarVista("/fxml/alertas.fxml");
    }

    @FXML
    public void mostrarCompras() {
        activarBoton(btnCompras);
        cargarVista("/fxml/compras.fxml");
    }

    @FXML
    public void mostrarReportes() {
        activarBoton(btnReportes);
        cargarVista("/fxml/reportes.fxml");
    }

    @FXML
    public void handleCerrarSesion() {
        LoginViewController.cerrarSesion();
    }

    private void activarBoton(Button botonActivo) {
        Button[] botones = {btnPos, btnProductos, btnAlertas, btnCompras, btnReportes};
        for (Button b : botones) {
            if (b != null) {
                b.getStyleClass().remove("sidebar-btn-active");
                if (!b.getStyleClass().contains("sidebar-btn")) {
                    b.getStyleClass().add("sidebar-btn");
                }
            }
        }
        if (botonActivo != null) {
            botonActivo.getStyleClass().remove("sidebar-btn");
            botonActivo.getStyleClass().add("sidebar-btn-active");
        }
    }

    private void cargarVista(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(JavaFxApplication.getContext()::getBean);
            Node vista = loader.load();
            contentArea.getChildren().setAll(vista);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
