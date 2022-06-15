package com.idi.userlogin.utils;

import com.idi.userlogin.Handlers.ConnectionHandler;
import com.idi.userlogin.Handlers.ControllerHandler;
import com.idi.userlogin.Handlers.JsonHandler;
import com.idi.userlogin.JavaBeans.Group;
import com.idi.userlogin.Main;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.apache.commons.dbutils.DbUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.logging.Level;

//Tracks the status of a project and how long it takes to scan (Timestamp & Totals)
public abstract class GroupLog {
    public static int scanLogID = 0;
    public static IntegerProperty dailyTotal;

    public static void insert() {
        Connection connection = null;
        ResultSet set = null;
        PreparedStatement ps = null;
        try {
            connection = ConnectionHandler.createDBConnection();
            ps = connection.prepareStatement("INSERT INTO " + DBUtils.DBTable.LOG.getTable() + " (emp_id,proj_id,start) VALUES((SELECT id FROM employees WHERE employees.name='" + ConnectionHandler.user.getName() + "'),(SELECT id FROM projects WHERE projects.job_id='" + JsonHandler.getSelJob().getJob_id() + "'),?)", Statement.RETURN_GENERATED_KEYS);
            Date now = ControllerHandler.formatDateTime(LocalDateTime.now().toString());
            ps.setTimestamp(1, new Timestamp(now.toInstant().toEpochMilli()));
            ps.executeUpdate();
            set = ps.getGeneratedKeys();
        } catch (SQLException | java.text.ParseException e) {
            e.printStackTrace();
            Main.LOGGER.log(Level.SEVERE, "There was an error inserting the scan log!", e);
        } finally {
            try {
                set = ps.getGeneratedKeys();
                if (set.next())
                    scanLogID = set.getInt(1);
            } catch (SQLException e) {
                Main.LOGGER.log(Level.SEVERE, "There was an error trying to generate a key!", e);
            }
            DbUtils.closeQuietly(set);
            DbUtils.closeQuietly(ps);
            DbUtils.closeQuietly(connection);
            dailyTotal = new SimpleIntegerProperty(0);
        }
    }

    public static int update(Group group) {
        int total = 0;
        if (scanLogID != 0) {
            Connection connection = null;
            ResultSet set = null;
            PreparedStatement ps = null;
            try {
                connection = ConnectionHandler.createDBConnection();
                ps = connection.prepareStatement("UPDATE " + DBUtils.DBTable.LOG.getTable() + " SET total=? WHERE id=" + scanLogID);
                ps.setInt(1, group.getTotal());
                ps.executeUpdate();
                ps = connection.prepareStatement("SELECT total FROM `ul_scan` WHERE id=" + scanLogID);
                set = ps.executeQuery();
                if (set.next())
                    total = set.getInt("total");
            } catch (SQLException e) {
                e.printStackTrace();
                Main.LOGGER.log(Level.SEVERE, "There was an error updating the scan log", e);
            } finally {
                DbUtils.closeQuietly(set);
                DbUtils.closeQuietly(ps);
                DbUtils.closeQuietly(connection);
            }
            ControllerHandler.loggedInController.getJob1Total().setText(String.valueOf(total));
        }

        return total;
    }

    public static void end() {
        if (scanLogID != 0) {
            Connection connection = null;
            ResultSet set = null;
            PreparedStatement ps = null;
            try {
                connection = ConnectionHandler.createDBConnection();
                ps = connection.prepareStatement("UPDATE " + DBUtils.DBTable.LOG.getTable() + " SET end=? WHERE id=" + scanLogID, 1);
                Date now = ControllerHandler.formatDateTime(LocalDateTime.now().toString());
                ps.setTimestamp(1, new Timestamp(now.toInstant().toEpochMilli()));
                ps.executeUpdate();
                set = ps.getGeneratedKeys();
            } catch (SQLException | java.text.ParseException e) {
                e.printStackTrace();
                Main.LOGGER.log(Level.SEVERE, "There was an error updating the scan log", e);
            } finally {
                DbUtils.closeQuietly(set);
                DbUtils.closeQuietly(ps);
                DbUtils.closeQuietly(connection);
            }
        }
    }
}
