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

    private ObservableList<ProductoRotacionDTO> listaRotacion = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colRotCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colRotNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colRotVendidos.setCellValueFactory(new PropertyValueFactory<>("cantidadTotalVendida"));
        colRotRecaudado.setCellValueFactory(new PropertyValueFactory<>("totalRecaudado"));

        tblRotacion.setItems(listaRotacion);
        cargarReportes();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
