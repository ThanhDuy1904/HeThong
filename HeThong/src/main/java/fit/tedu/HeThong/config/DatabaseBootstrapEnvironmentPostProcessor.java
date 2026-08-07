package fit.tedu.HeThong.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

public class DatabaseBootstrapEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	private static final Logger log = LoggerFactory.getLogger(DatabaseBootstrapEnvironmentPostProcessor.class);
	private static final String DEFAULT_JDBC_URL = "jdbc:mysql://localhost:3306/tedu_data?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true";
	private static final String DEFAULT_USERNAME = "root";
	private static final String DEFAULT_PASSWORD = "";
	private static final String DEFAULT_DATABASE_NAME = "tedu_data";
	private static final String DEFAULT_SCHEMA_FILE = "database/tedu_data.sql";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String jdbcUrl = getOrDefault(environment.getProperty("spring.datasource.url"), DEFAULT_JDBC_URL);
		String username = getOrDefault(environment.getProperty("spring.datasource.username"), DEFAULT_USERNAME);
		String password = getOrDefault(environment.getProperty("spring.datasource.password"), DEFAULT_PASSWORD);

		String databaseName = resolveDatabaseName(jdbcUrl);
		String serverJdbcUrl = resolveServerJdbcUrl(jdbcUrl);

		try (Connection connection = DriverManager.getConnection(serverJdbcUrl, username, password)) {
			createDatabaseIfMissing(connection, databaseName);

			if (!hasRolesTable(connection, databaseName)) {
				executeSchemaScript(connection);
			}
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to bootstrap MySQL database '" + databaseName + "'", ex);
		}
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	private void createDatabaseIfMissing(Connection connection, String databaseName) throws SQLException {
		String sql = "CREATE DATABASE IF NOT EXISTS `" + databaseName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}
	}

	private boolean hasRolesTable(Connection connection, String databaseName) throws SQLException {
		String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = 'roles'";
		try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
			preparedStatement.setString(1, databaseName);
			try (var resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					return resultSet.getInt(1) > 0;
				}
			}
		}
		return false;
	}

	private void executeSchemaScript(Connection connection) throws Exception {
		Path schemaPath = Paths.get(DEFAULT_SCHEMA_FILE);
		if (!Files.exists(schemaPath)) {
			log.warn("Schema file not found at {}. Database was created, but tables will rely on JPA auto-update.", schemaPath.toAbsolutePath());
			return;
		}

		EncodedResource encodedResource = new EncodedResource(new FileSystemResource(schemaPath.toFile()), StandardCharsets.UTF_8);
		ScriptUtils.executeSqlScript(connection, encodedResource);
	}

	private String resolveDatabaseName(String jdbcUrl) {
		String withoutQuery = jdbcUrl.contains("?") ? jdbcUrl.substring(0, jdbcUrl.indexOf('?')) : jdbcUrl;
		int lastSlash = withoutQuery.lastIndexOf('/');
		if (lastSlash < 0 || lastSlash == withoutQuery.length() - 1) {
			return DEFAULT_DATABASE_NAME;
		}
		return withoutQuery.substring(lastSlash + 1);
	}

	private String resolveServerJdbcUrl(String jdbcUrl) {
		String query = jdbcUrl.contains("?") ? jdbcUrl.substring(jdbcUrl.indexOf('?')) : "";
		String withoutQuery = jdbcUrl.contains("?") ? jdbcUrl.substring(0, jdbcUrl.indexOf('?')) : jdbcUrl;
		int lastSlash = withoutQuery.lastIndexOf('/');
		if (lastSlash < 0) {
			return jdbcUrl;
		}
		return withoutQuery.substring(0, lastSlash + 1) + query;
	}

	private String getOrDefault(String value, String defaultValue) {
		return value == null || value.isBlank() ? defaultValue : value;
	}
}