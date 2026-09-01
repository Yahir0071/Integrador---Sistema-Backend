package pe.edu.utp.Grupo06.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pe.edu.utp.Grupo06.model.AlertaReposicion;
import pe.edu.utp.Grupo06.service.IAlertaReposicionService;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class AlertasViewController {

    @Autowired
    private IAlertaReposicionService alertaService;

    @FXML
    private TableView<AlertaReposicion> tblAlertas;

    @FXML
    private TableColumn<AlertaReposicion, Long> colAlertaId;

    @FXML
    private TableColumn<AlertaReposicion, String> colProducto;

    @FXML
    private TableColumn<AlertaReposicion, Integer> colStockReg;

    @FXML
    private TableColumn<AlertaReposicion, Integer> colStockMin;

    @FXML
    private TableColumn<AlertaReposicion, Integer> colSugerida;

    @FXML
    private TableColumn<AlertaReposicion, String> colFecha;

    @FXML
    private TableColumn<AlertaReposicion, String> colEstado;

    @FXML
    private TableColumn<AlertaReposicion, Void> colAcciones;

    private ObservableList<AlertaReposicion> listaAlertas = FXCollections.observableArrayList();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        configurarColumnas();
        cargarAlertas();
    }

    private void configurarColumnas() {
        colAlertaId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProducto.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getProducto() != null ?
                        cellData.getValue().getProducto().getNombre() + " (" + cellData.getValue().getProducto().getCodigo() + ")" : ""));

        colStockReg.setCellValueFactory(new PropertyValueFactory<>("stockRegistrado"));
        colStockMin.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));
        colSugerida.setCellValueFactory(new PropertyValueFactory<>("cantidadSugerida"));

        colFecha.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFechaGeneracion() != null ?
                        cellData.getValue().getFechaGeneracion().format(formatter) : ""));

        colEstado.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getEstado() != null ?
                        cellData.getValue().getEstado().name() : ""));

        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnAtender = new Button("✔️ Atender");

            {
                btnAtender.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-weight: bold; -fx-cursor: hand;");
                btnAtender.setOnAction(event -> {
                    AlertaReposicion alerta = getTableView().getItems().get(getIndex());
                    atenderAlerta(alerta);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox pane = new HBox(btnAtender);
                    pane.setAlignment(Pos.CENTER);
                    setGraphic(pane);
                }
            }
        });

        tblAlertas.setItems(listaAlertas);
    }

    @FXML
    public void cargarAlertas() {
        try {
            List<AlertaReposicion> alertas = alertaService.listarPendientes();
            listaAlertas.setAll(alertas);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void atenderAlerta(AlertaReposicion alerta) {
        TextInputDialog dialog = new TextInputDialog("Compra de reposición realizada");
        dialog.setTitle("Atender Alerta");
        dialog.setHeaderText("Registrar atención de reposición para:\n" + alerta.getProducto().getNombre());
        dialog.setContentText("Observación:");

        dialog.showAndWait().ifPresent(obs -> {
            try {
                alertaService.atenderAlerta(alerta.getId(), obs);
                cargarAlertas();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
