import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringJoiner;

import javax.sql.DataSource;

import com.zeroc.Ice.Current;

import RegistryModule.CallbackPrx;

public class PerformQueryImpl implements RegistryModule.PerformQuery {

    private final DataSource dataSource;

    public PerformQueryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private String executeQuery(String query, Current current) {
        StringBuilder resultBuilder = new StringBuilder();

        try (Connection connection = dataSource.getConnection();
                PreparedStatement stmt = connection.prepareStatement(query);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                resultBuilder.append("Documento: ").append(rs.getString("documento"))
                        .append(", Departamento: ").append(rs.getString("departamento_nombre"))
                        .append(", Municipio: ").append(rs.getString("municipio_nombre"))
                        .append(", Puesto de votación: ").append(rs.getString("puesto_votacion_nombre"))
                        .append(", Mesa: ").append(rs.getString("mesa_votacion_consecutive"))
                        .append("\n");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error ejecutando la consulta: " + e.getMessage());
        }

        return resultBuilder.toString();
    }

    private String makeQuery(String[] ids, Current current) {
        StringJoiner placeholders = new StringJoiner(",");
        for (String id : ids) {
            placeholders.add(id);
        }

        String sql = "SELECT\n" +
                "    ciudadano.documento,\n" +
                "    departamento.nombre AS departamento_nombre,\n" +
                "    municipio.nombre AS municipio_nombre,\n" +
                "    puesto_votacion.nombre AS puesto_votacion_nombre,\n" +
                "    mesa_votacion.consecutive AS mesa_votacion_consecutive\n" +
                "FROM ciudadano\n" +
                "JOIN mesa_votacion ON ciudadano.mesa_id = mesa_votacion.id\n" +
                "JOIN puesto_votacion ON mesa_votacion.puesto_id = puesto_votacion.id\n" +
                "JOIN municipio ON puesto_votacion.municipio_id = municipio.id\n" +
                "JOIN departamento ON municipio.departamento_id = departamento.id\n" +
                "WHERE ciudadano.documento IN (%s);";

        return String.format(sql, placeholders.toString());
    }

    @Override
    public void receiveMessage(String[] ids, CallbackPrx callback, Current current) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'receiveMessage'");
    }

}
