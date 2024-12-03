import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import javax.sql.DataSource;

import com.zeroc.Ice.Current;

import RegistryModule.TaskManagerPrx;

public class PerformQueryImpl implements RegistryModule.PerformQuery {

    private final DataSource dataSource;

    public PerformQueryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Map<String, String> executeQuery(String query, Current current) {
        Map<String, String> resultMap = new HashMap<>();

        System.out.println("Executing query with length: " + query.length());

        try (Connection connection = dataSource.getConnection();
                PreparedStatement stmt = connection.prepareStatement(query);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                System.out.println("Column count: " + columnCount);
                System.out.println("Column name: " + metaData.getColumnName(1));

                String documento = rs.getString("documento");
                System.out.println("Documento: " + documento);
                StringJoiner detallesJoiner = new StringJoiner(", ");
                for (int i = 1; i <= columnCount; i++) {
                    String columnValue = rs.getString(i);
                    detallesJoiner.add(columnValue != null ? columnValue : "");
                }

                resultMap.put(documento, detallesJoiner.toString());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error ejecutando la consulta: " + e.getMessage());
        }

        return resultMap;
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
    public void receiveMessage(String[] ids, TaskManagerPrx taskManager, String taskId, Current current) {

        System.out.println("Received message with ids: " + ids.length);
        String query = makeQuery(ids, current);
        Map<String, String> result = executeQuery(query, current);
        taskManager.addPartialResult(result, taskId);

    }

}
