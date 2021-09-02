package com.controller.collectors;

import com.controller.util.JSONUtil;
import com.controller.util.json.JSONException;
import com.controller.util.json.JSONObject;
import com.controller.util.json.JSONStringer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

public class DB2Collector extends DBCollector {
    private static final Logger LOG = Logger.getLogger(DB2Collector.class);

    // "DB2 v11.5.6.0"
    private static final String VERSION_SQL = "SELECT SERVICE_LEVEL FROM TABLE(ENV_GET_INST_INFO());";

    // private static final String PARAMETERS_SQL = "SHOW ALL;";
    private final Map<String, String> db2Params = new HashMap<String, String>() {{
        put("DB_MEM_THRESH", "100");
        put("LOCKLIST", "4096");
        put("MAXLOCKS", "10");
        put("PCKCACHESZ", "320");
        put("SHEAPTHRES_SHR", "5000");
        put("SORTHEAP", "256");
        put("CATALOGCACHE_SZ", "200");
        put("LOGBUFSZ", "256");
        put("UTIL_HEAP_SZ", "5000");
        put("CHNGPGS_THRESH", "60");
        put("MAXFILOP", "256");
    }};

//    private static final String[] DB2_MON_VIEWS = {
//            "MON_GET_PKG_CACHE_STMT",
//            "MON_GET_SERVICE_SUBCLASS_STATS",
//            "MON_GET_DATABASE"
//    };

    private static final String METRICS_SQL =
            "SELECT TOTAL_APP_COMMITS FROM SYSIBMADM.MON_DB_SUMMARY;";

    private HashMap<String, String> db2Metrics = new HashMap<>();

    public DB2Collector(String oriDBUrl, String username, String password) {
        db2Metrics = new HashMap<>();
        try {
            Connection conn = DriverManager.getConnection(oriDBUrl, username, password);

            Statement s = conn.createStatement();

            // Collect DBMS version
            ResultSet out = s.executeQuery(VERSION_SQL);
            if (out.next()) {
                String[] outStr = out.getString(1).split(" ");
                String[] verStr = outStr[1].split("\\.");
                this.version.append(verStr[0].substring(1));
                this.version.append(".");
                this.version.append(verStr[1]);
            }

            // Collect DBMS parameters
            for (Map.Entry<String, String> e : db2Params.entrySet()) {
                dbParameters.put(e.getKey(), e.getValue());
            }

            // Collect DBMS internal metrics
            out = s.executeQuery(METRICS_SQL);
            while (out.next()) {
                LOG.info("total_app_commits: " + out.getString(1));
                db2Metrics.put("total_app_commits", out.getString(1));
            }

            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            LOG.error("Error while collecting DB parameters: " + e.getMessage());
        }
    }

    @Override
    public String collectParameters() {
        JSONStringer stringer = new JSONStringer();
        try {
            stringer.object();
            stringer.key(JSON_GLOBAL_KEY);
            JSONObject jobLocal = new JSONObject();
            JSONObject job = new JSONObject();
            for (String k : dbParameters.keySet()) {
                job.put(k, dbParameters.get(k));
            }
            // "global is a fake view_name (a placeholder)"
            jobLocal.put("global", job);
            stringer.value(jobLocal);
            stringer.key(JSON_LOCAL_KEY);
            stringer.value(null);
            stringer.endObject();
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return JSONUtil.format(stringer.toString());
    }

    @Override
    public boolean hasMetrics() {
        return (db2Metrics.isEmpty() == false);
    }

    @Override
    public String collectMetrics() {
        JSONStringer stringer = new JSONStringer();
        try {
            stringer.object();

            stringer.key(JSON_GLOBAL_KEY);
            JSONObject jobGlobal = new JSONObject();

            JSONObject job = new JSONObject();
            for (Map.Entry<String, String> entry : db2Metrics.entrySet()) {
                job.put(entry.getKey(), entry.getValue());
            }
            jobGlobal.put("sysibmadm", job);
            
            stringer.value(jobGlobal);
            stringer.key(JSON_LOCAL_KEY);
            stringer.value(null);
            stringer.endObject();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return JSONUtil.format(stringer.toString());
    }
}