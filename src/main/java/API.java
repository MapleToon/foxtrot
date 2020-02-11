import static spark.Spark.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;


import java.util.Base64;

public class API {
    private static final Logger log = Logger.getLogger(API.class.getName());
    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static Handler errorHandler  = null;
    private static Handler configHandler  = null;
    private static Connection connection = null;

    //MACROS
    private static String DATABASE_URL = "jdbc:sqlserver://yourserver.database.windows.net:1433;"
            + "database=AdventureWorks;"
            + "user=yourusername@yourserver;"
            + "password=yourpassword;"
            + "encrypt=true;"
            + "trustServerCertificate=false;"
            + "loginTimeout=30;";
    private static String DATABASE_NAME = "PestsAndDisease";


    private static boolean inUK(float lat,float lang){
        if(lat <59.5 && lat > 49.5 && lang > -8.95 && lang <1.75){
            return true;
        }
        return false;
    }

    private static void connectDB(){
        try {
            connection = DriverManager.getConnection(DATABASE_URL);
        }
        catch (SQLException e){
        log.severe(dtf.format(LocalDateTime.now())+":Failed to connect to database with error: " + e.getMessage());
        }
    }

    private static void setUpLogs(){
        try{
            //Creating consoleHandler and fileHandler
            errorHandler  = new FileHandler("./error.log",true);
            configHandler  = new FileHandler("./config.log",true);
            //Assigning handlers to LOGGER object
            log.addHandler(errorHandler);
            log.addHandler(configHandler);
            //Setting levels to handlers and LOGGER
            errorHandler.setLevel(Level.WARNING);
            configHandler.setLevel(Level.CONFIG);
            log.setLevel(Level.ALL);
            log.config(dtf.format(LocalDateTime.now())+": Log set up");
        }
        catch (IOException f){
            System.err.println(dtf.format(LocalDateTime.now())+":Log file failed"+ f.getMessage());
        }
    }


    public static void main(String[] args) {
        connectDB();
        setUpLogs();

        before((request, response) -> response.type("JSON"));

        post("/api/new",(request, response) -> {
            JSONObject reqBody = new JSONObject(request.body());
            JSONObject res = new JSONObject();
            try {
                if (inUK( reqBody.getFloat("latitude"),  reqBody.getFloat("longitude"))) {

                    String sql = "insert into "+DATABASE_NAME
                            + "(report_id, category, date, latitude, longitude, name, description, image,solved)"
                            + "VALUES (?,?,?,?,?,?,?,?,?)";

                    PreparedStatement p = connection.prepareStatement(sql);
                    p.setString(1, reqBody.getString("report_id"));
                    p.setString(2, reqBody.optString("category"));
                    p.setString(3, reqBody.optString("date",dtf.format(LocalDateTime.now())));
                    p.setString(4, reqBody.optString("latitude"));
                    p.setString(5, reqBody.optString("longitude"));
                    p.setString(6, reqBody.optString("name"));
                    p.setString(7, reqBody.optString("description"));
                    p.setString(8, reqBody.optString("image"));
                    p.setString(9, reqBody.optString("solved"));

                    p.execute(sql);

                    res.append("complete",true);
                }
                else{
                    res.append("complete",false);
                    res.append("error","Outside UK");
                }
            }
            catch(JSONException e){
                log.warning(dtf.format(LocalDateTime.now())+":Error in parsing POST request to /api/new");
                res.append("error","Request parsing error");
                res.append("complete",false);
                response.body(res.toString());
            }
            return response;
        });

        get("/api/map/*", (request, response) -> {
            JSONObject reqBody = new JSONObject( request.body());
            JSONObject res = new JSONObject();
            try {
                if (inUK( reqBody.getFloat("longitude"),  reqBody.getFloat("longitude"))) {
                    //DB request for info on body.get("pest"); in range
                }
            }
            catch(JSONException e){
                log.warning(dtf.format(LocalDateTime.now())+":Error in parsing GET request to /api/map/*");
                res.append("error","Request parsing error");
                response.body(res.toString());
            }

            return response;
        });
    }
}