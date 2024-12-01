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

    @Override
    public String executeQuery(String query, Current current) {
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

    @Override
    public String makeQuery(String[] ids, Current current) {
        StringJoiner placeholders = new StringJoiner(",");
        for (String id : ids) {
            placeholders.add(id);
        }

        String sql = """
                SELECT
                    ciudadano.documento,
                    departamento.nombre AS departamento_nombre,
                    municipio.nombre AS municipio_nombre,
                    puesto_votacion.nombre AS puesto_votacion_nombre,
                    mesa_votacion.consecutive AS mesa_votacion_consecutive
                FROM ciudadano
                JOIN mesa_votacion ON ciudadano.mesa_id = mesa_votacion.id
                JOIN puesto_votacion ON mesa_votacion.puesto_id = puesto_votacion.id
                JOIN municipio ON puesto_votacion.municipio_id = municipio.id
                JOIN departamento ON municipio.departamento_id = departamento.id
                WHERE ciudadano.documento IN (%s);
                """;

        return sql.formatted(placeholders.toString());
    }

    @Override
    public void receiveMessage(String[] ids, CallbackPrx callback, Current current) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'receiveMessage'");
    }

}
