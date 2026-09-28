package pe.edu.utp.Grupo06.service.impl;

import org.springframework.stereotype.Service;
import pe.edu.utp.Grupo06.model.Compra;
import pe.edu.utp.Grupo06.model.Merma;
import pe.edu.utp.Grupo06.model.Producto;
import pe.edu.utp.Grupo06.model.Venta;
import pe.edu.utp.Grupo06.service.IExportacionService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class ExportacionServiceImpl implements IExportacionService {

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void exportarVentasCsv(File destino, List<Venta> ventas, LocalDate inicio, LocalDate fin) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destino), StandardCharsets.UTF_8))) {
            writer.write('\ufeff'); // UTF-8 BOM para reconocimiento nativo de tildes y símbolos en Microsoft Excel

            // Encabezado corporativo
            writer.write("BODEGA & MINIMARKET SGCIVORP - REPORTE DE VENTAS\n");
            writer.write("RUC: 20601234567 | Dirección: Av. Principal 123, Lima\n");
            String rangoTxt = (inicio != null && fin != null) ? "Del " + inicio.format(df) + " al " + fin.format(df) : "Historial Completo";
            writer.write("Periodo: " + escape(rangoTxt) + "\n");
            writer.write("Fecha de emisión: " + escape(LocalDateTime.now().format(dtf)) + "\n");
            writer.write("Total registros: " + ventas.size() + "\n\n");

            // Cabeceras de tabla
            writer.write("N° Ticket;Fecha y Hora;Vendedor / Cajero;Op. Gravada (S/);IGV 18% (S/);Total Venta (S/);Estado;Metodos de Pago\n");

            BigDecimal sumSubtotal = BigDecimal.ZERO;
            BigDecimal sumIgv = BigDecimal.ZERO;
            BigDecimal sumTotal = BigDecimal.ZERO;

            for (Venta v : ventas) {
                BigDecimal tot = v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO;
                BigDecimal sub = v.getSubtotal() != null && v.getSubtotal().compareTo(BigDecimal.ZERO) > 0 ?
                        v.getSubtotal() : tot.divide(BigDecimal.valueOf(1.18), 2, RoundingMode.HALF_UP);
                BigDecimal igv = v.getIgv() != null && v.getIgv().compareTo(BigDecimal.ZERO) > 0 ?
                        v.getIgv() : tot.subtract(sub);

                sumSubtotal = sumSubtotal.add(sub);
                sumIgv = sumIgv.add(igv);
                sumTotal = sumTotal.add(tot);

                String pagosStr = "";
                if (v.getPagos() != null && !v.getPagos().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < v.getPagos().size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(v.getPagos().get(i).getMetodoPago()).append(" (S/ ").append(v.getPagos().get(i).getMonto()).append(")");
                    }
                    pagosStr = sb.toString();
                }

                writer.write(String.format(Locale.US, "%s;%s;%s;%.2f;%.2f;%.2f;%s;%s\n",
                        escape(v.getNumeroTicket()),
                        escape(v.getFechaVenta() != null ? v.getFechaVenta().format(dtf) : ""),
                        escape(v.getUsuario() != null ? v.getUsuario().getNombreCompleto() : "N/A"),
                        sub,
                        igv,
                        tot,
                        escape(v.getEstado()),
                        escape(pagosStr)
                ));
            }

            // Fila de totales
            writer.write(String.format(Locale.US, "TOTALES;;;%.2f;%.2f;%.2f;;%d Ventas\n",
                    sumSubtotal, sumIgv, sumTotal, ventas.size()));
        }
    }

    @Override
    public void exportarComprasCsv(File destino, List<Compra> compras, LocalDate inicio, LocalDate fin) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destino), StandardCharsets.UTF_8))) {
            writer.write('\ufeff');

            writer.write("BODEGA & MINIMARKET SGCIVORP - REPORTE DE COMPRAS Y ABASTECIMIENTO\n");
            writer.write("RUC: 20601234567 | Dirección: Av. Principal 123, Lima\n");
            String rangoTxt = (inicio != null && fin != null) ? "Del " + inicio.format(df) + " al " + fin.format(df) : "Historial Completo";
            writer.write("Periodo: " + escape(rangoTxt) + "\n");
            writer.write("Fecha de emisión: " + escape(LocalDateTime.now().format(dtf)) + "\n");
            writer.write("Total facturas / compras: " + compras.size() + "\n\n");

            writer.write("N° Factura;Fecha Compra;Proveedor;RUC Proveedor;Op. Gravada (S/);IGV 18% (S/);Total Factura (S/);Estado;Usuario Registrador\n");

            BigDecimal sumSubtotal = BigDecimal.ZERO;
            BigDecimal sumIgv = BigDecimal.ZERO;
            BigDecimal sumTotal = BigDecimal.ZERO;

            for (Compra c : compras) {
                BigDecimal tot = c.getTotal() != null ? c.getTotal() : BigDecimal.ZERO;
                BigDecimal sub = c.getSubtotal() != null && c.getSubtotal().compareTo(BigDecimal.ZERO) > 0 ?
                        c.getSubtotal() : tot.divide(BigDecimal.valueOf(1.18), 2, RoundingMode.HALF_UP);
                BigDecimal igv = c.getIgv() != null && c.getIgv().compareTo(BigDecimal.ZERO) > 0 ?
                        c.getIgv() : tot.subtract(sub);

                sumSubtotal = sumSubtotal.add(sub);
                sumIgv = sumIgv.add(igv);
                sumTotal = sumTotal.add(tot);

                String provNombre = c.getProveedor() != null ? c.getProveedor().getRazonSocial() : "N/A";
                String provRuc = c.getProveedor() != null ? c.getProveedor().getRuc() : "-";
                String usuNombre = c.getUsuario() != null ? c.getUsuario().getNombreCompleto() : "N/A";

                writer.write(String.format(Locale.US, "%s;%s;%s;%s;%.2f;%.2f;%.2f;%s;%s\n",
                        escape(c.getNumeroComprobante()),
                        escape(c.getFechaCompra() != null ? c.getFechaCompra().format(dtf) : ""),
                        escape(provNombre),
                        escape(provRuc),
                        sub,
                        igv,
                        tot,
                        "REGISTRADA",
                        escape(usuNombre)
                ));
            }

            writer.write(String.format(Locale.US, "TOTALES;;;;%.2f;%.2f;%.2f;;%d Compras\n",
                    sumSubtotal, sumIgv, sumTotal, compras.size()));
        }
    }

    @Override
    public void exportarMermasCsv(File destino, List<Merma> mermas, LocalDate inicio, LocalDate fin) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destino), StandardCharsets.UTF_8))) {
            writer.write('\ufeff');

            writer.write("BODEGA & MINIMARKET SGCIVORP - REPORTE DE MERMAS Y PÉRDIDAS DE INVENTARIO\n");
            writer.write("Acreditación para Auditoría y Desmedro Contable / SUNAT\n");
            String rangoTxt = (inicio != null && fin != null) ? "Del " + inicio.format(df) + " al " + fin.format(df) : "Historial Completo";
            writer.write("Periodo: " + escape(rangoTxt) + "\n");
            writer.write("Fecha de emisión: " + escape(LocalDateTime.now().format(dtf)) + "\n");
            writer.write("Total registros de baja: " + mermas.size() + "\n\n");

            writer.write("N° Baja;Fecha y Hora;Código Producto;Producto;Marca;Categoría;Cantidad;Motivo;P. Compra Unit (S/);Pérdida Total (S/);F. Caducidad;Usuario Auditor;Observación / Justificación\n");

            long sumUnidades = 0;
            BigDecimal sumPerdida = BigDecimal.ZERO;

            for (Merma m : mermas) {
                Producto p = m.getProducto();
                String cod = p != null ? p.getCodigo() : "-";
                String nom = p != null ? p.getNombre() : "-";
                String marca = (p != null && p.getMarca() != null && !p.getMarca().isBlank()) ? p.getMarca() : "Genérico";
                String cat = (p != null && p.getCategoria() != null) ? p.getCategoria().getNombre() : "-";
                String mot = m.getMotivo() != null ? m.getMotivo().getDescripcion() : "Otro";
                String fVenc = m.getFechaVencimiento() != null ? m.getFechaVencimiento().format(df) : "-";
                String usu = m.getUsuario() != null ? m.getUsuario().getNombreCompleto() : "N/A";

                int cant = m.getCantidad() != null ? m.getCantidad() : 0;
                BigDecimal unit = m.getCostoUnitario() != null ? m.getCostoUnitario() : BigDecimal.ZERO;
                BigDecimal tot = m.getCostoTotalPerdida() != null ? m.getCostoTotalPerdida() : BigDecimal.ZERO;

                sumUnidades += cant;
                sumPerdida = sumPerdida.add(tot);

                writer.write(String.format(Locale.US, "%d;%s;%s;%s;%s;%s;%d;%s;%.2f;%.2f;%s;%s;%s\n",
                        m.getId(),
                        escape(m.getFechaMerma() != null ? m.getFechaMerma().format(dtf) : ""),
                        escape(cod),
                        escape(nom),
                        escape(marca),
                        escape(cat),
                        cant,
                        escape(mot),
                        unit,
                        tot,
                        escape(fVenc),
                        escape(usu),
                        escape(m.getObservacion() != null ? m.getObservacion() : "")
                ));
            }

            writer.write(String.format(Locale.US, "TOTALES;;;;;;%d;;;%.2f;;;%d Registros\n",
                    sumUnidades, sumPerdida, mermas.size()));
        }
    }

    @Override
    public void exportarInventarioCsv(File destino, List<Producto> productos) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destino), StandardCharsets.UTF_8))) {
            writer.write('\ufeff');

            writer.write("BODEGA & MINIMARKET SGCIVORP - REPORTE GENERAL DE INVENTARIO Y STOCK\n");
            writer.write("Control de Existencias Físicas y Valorización de Almacén\n");
            writer.write("Fecha de emisión: " + escape(LocalDateTime.now().format(dtf)) + "\n");
            writer.write("Total productos activos: " + productos.size() + "\n\n");

            writer.write("Código;Producto;Marca;Categoría;Unidad Medida;Stock Actual;Stock Mínimo;Estado Stock;P. Compra (S/);P. Venta (S/);Valorización Almacén (S/);Estado\n");

            long sumStock = 0;
            BigDecimal sumValorizacion = BigDecimal.ZERO;

            for (Producto p : productos) {
                String marca = (p.getMarca() != null && !p.getMarca().isBlank()) ? p.getMarca() : "Genérico";
                String cat = p.getCategoria() != null ? p.getCategoria().getNombre() : "-";
                String um = p.getUnidadMedida() != null ? p.getUnidadMedida().name() : "UNIDAD";

                int stock = p.getStockActual() != null ? p.getStockActual() : 0;
                int min = p.getStockMinimo() != null ? p.getStockMinimo() : 0;
                String estadoStock = stock <= min ? "REPONER (BAJO STOCK)" : "ÓPTIMO";

                BigDecimal pc = p.getPrecioCompra() != null ? p.getPrecioCompra() : BigDecimal.ZERO;
                BigDecimal pv = p.getPrecioVenta() != null ? p.getPrecioVenta() : BigDecimal.ZERO;
                BigDecimal valorizacion = pc.multiply(BigDecimal.valueOf(stock));

                sumStock += stock;
                sumValorizacion = sumValorizacion.add(valorizacion);

                writer.write(String.format(Locale.US, "%s;%s;%s;%s;%s;%d;%d;%s;%.2f;%.2f;%.2f;%s\n",
                        escape(p.getCodigo()),
                        escape(p.getNombre()),
                        escape(marca),
                        escape(cat),
                        escape(um),
                        stock,
                        min,
                        escape(estadoStock),
                        pc,
                        pv,
                        valorizacion,
                        Boolean.TRUE.equals(p.getEstado()) ? "ACTIVO" : "INACTIVO"
                ));
            }

            writer.write(String.format(Locale.US, "TOTALES;;;;;%d;;;;;%.2f;%d Productos\n",
                    sumStock, sumValorizacion, productos.size()));
        }
    }

    private String escape(Object val) {
        if (val == null) return "";
        String s = val.toString().replace("\"", "\"\"");
        if (s.contains(";") || s.contains("\n") || s.contains("\r") || s.contains("\"")) {
            return "\"" + s + "\"";
        }
        return s;
    }
}
