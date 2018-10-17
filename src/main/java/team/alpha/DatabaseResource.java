package team.alpha;

import clover.com.google.gson.Gson;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.alpha.model.Credentials;
import team.alpha.model.ErrorResponse;
import team.alpha.model.SignupForm;
import team.alpha.model.UserPreferences;

import java.sql.*;
import java.util.Properties;

@RestController
class DatabaseResource {

    private Gson gson = new Gson();
    private Connection db;
    private CallableStatement loginProc;
    private CallableStatement signupProc;


    DatabaseResource() {
        try {
            db = createDBConnection();
            createPreparedStatements();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static Connection createDBConnection() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", "postgres");
        return DriverManager.getConnection(Constants.DB_URL, properties);
    }

    private void createPreparedStatements() throws SQLException {
        loginProc = db.prepareCall("{ call login_user(?,?) }");
        signupProc = db.prepareCall("{ ? = call create_user(?,?,?,?,?,?,?) }");
        signupProc.registerOutParameter(1, Types.INTEGER);
    }

    @CrossOrigin
    @RequestMapping("/login")
    String login(@RequestBody Credentials credentials) {
        int userId = -1;
        UserPreferences userPreferences = new UserPreferences();

        try {
            loginProc.setString(1, credentials.getUsername());
            loginProc.setString(2, credentials.getPassword());

            ResultSet rs = loginProc.executeQuery();

            while (rs.next()) {
                userId = rs.getInt(1);
                if (userId > 0) {
                    userPreferences.setCity(rs.getString(2));
                    userPreferences.setCountry(rs.getString(3));
                    userPreferences.setCompany(rs.getString(4));
                    userPreferences.setSubscribedToNewsAlerts(rs.getBoolean(5));
                    userPreferences.setSubscribedToWeatherAlerts(rs.getBoolean(6));
                }
            }

            if (userId == -1) {
                return getErrorResponse(Constants.MSG_INVALID_CREDENTIAL);
            }

            return gson.toJson(userPreferences);

        } catch (SQLException e) {
            e.printStackTrace();
            return getErrorResponse(Constants.MSG_FAILED_TO_FETCH_USER);
        }
    }

    @CrossOrigin
    @RequestMapping("/signup")
    String signup(@RequestBody SignupForm signupForm) {
        String response = "false";

        try {
            signupProc.setString(2, signupForm.getCredentials().getUsername());
            signupProc.setString(3, signupForm.getCredentials().getPassword());
            UserPreferences userPreferences = signupForm.getUserPreferences();
            signupProc.setString(4, userPreferences.getCity());
            signupProc.setString(5, userPreferences.getCountry());
            signupProc.setString(6, userPreferences.getCompany());
            signupProc.setBoolean(7, userPreferences.isSubscribedToNewsAlerts());
            signupProc.setBoolean(8, userPreferences.isSubscribedToWeatherAlerts());

            signupProc.execute();

            int userId = signupProc.getInt(1);

            if (userId == 0) {
                return getErrorResponse(Constants.MSG_USER_ALREADY_EXISTS);
            }

            return "" + userId;

        } catch (SQLException e) {
            e.printStackTrace();
            return getErrorResponse(Constants.MSG_FAILED_TO_CREATE_USER);
        }
    }

    private String getErrorResponse(String msg) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setCode(Constants.STATUS_OK);
        errorResponse.setMessage(msg);
        return gson.toJson(errorResponse);
    }

}
