package com.idi.userlogin.Controllers;

import com.idi.userlogin.Handlers.ConnectionHandler;
import com.idi.userlogin.Handlers.ControllerHandler;
import com.idi.userlogin.Handlers.JsonHandler;
import com.idi.userlogin.JavaBeans.Group;
import com.idi.userlogin.JavaBeans.Item;
import com.idi.userlogin.Main;
import com.idi.userlogin.utils.DBUtils;
import com.idi.userlogin.utils.Utils;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.RecursiveTreeItem;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.util.StringConverter;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.dbutils.DbUtils;
import org.controlsfx.control.SearchableComboBox;
import org.controlsfx.control.textfield.CustomTextField;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.idi.userlogin.JavaBeans.User.COMP_NAME;
import static com.idi.userlogin.Main.fxTrayIcon;

public class ManifestViewController extends BaseEntryController<BaseEntryController.EntryItem> implements Initializable {

    private final EntryItem treeRoot = new EntryItem();
    private final RecursiveTreeItem rootItem = new RecursiveTreeItem<>(treeRoot, RecursiveTreeObject::getChildren);
    private String uid;
    private String groupCol;

    @FXML
    private TreeView<String> itemInfo;

    @FXML
    private SearchableComboBox<EntryItem> itemCombo;

    @Override
    @FXML
    public void insert() throws IOException {
        final EntryItem item = itemCombo.getSelectionModel().getSelectedItem();
        final String type = typeCombo.getSelectionModel().getSelectedItem().getText();
        final List<String> conditions = conditCombo.getCheckModel().getCheckedItems();
        final String comments = commentsField.getText();
        final Group group = groupTree.getSelectionModel().getSelectedItem().getValue();

        if (item != null) {
            final TreeItem newItem = new TreeItem<>(item);
            item.setWorkstation(COMP_NAME);
            item.setStarted_On(LocalDateTime.now().toString());
            item.setGroup(group);
            item.getType().setText(type);
            item.setupType(type);
            item.setComments(comments);
            item.getConditions().addAll(conditions);
            int key = insertHelper(item);
            item.setId(key);
            if (tree.getRoot().getChildren().isEmpty()) {
                initSelGroup(group);
            }
            tree.getRoot().getChildren().add(newItem);

            createFoldersFromStruct(item);
            fxTrayIcon.showInfoMessage("Item: " + item.getName() + " Inserted");

            if (!itemCombo.getItems().isEmpty()) {
                itemCombo.getSelectionModel().selectNext();
            }
            itemCombo.getItems().stream().filter(e -> e.equals(item)).findAny().ifPresent(e -> {
                itemCombo.getItems().remove(e);
            });
            typeCombo.getSelectionModel().selectFirst();
            resetFields();
        }
    }

    public int getManifest_ID() {
        Connection connection = null;
        ResultSet set = null;
        PreparedStatement ps = null;
        int key = 0;
        try {
            connection = ConnectionHandler.createDBConnection();
            ps = connection.prepareStatement("SELECT id FROM `" + JsonHandler.getSelJob().getJob_id() + "" + DBUtils.DBTable.M.getTable() + "` WHERE `" + uid + "`='" + itemCombo.getSelectionModel().getSelectedItem().getName() + "' LIMIT 1");
            set = ps.executeQuery();
            if (set.next()) {
                key = set.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(set);
            DbUtils.closeQuietly(ps);
            DbUtils.closeQuietly(connection);
        }
        return key;
    }

    @Override
    public int insertHelper(Item<? extends Item> item) {
        Connection connection = null;
        ResultSet set = null;
        PreparedStatement ps = null;
        int key = 0;
        try {
            connection = ConnectionHandler.createDBConnection();
            ps = connection.prepareStatement("INSERT INTO `" + JsonHandler.getSelJob().getJob_id() + "" + DBUtils.DBTable.D.getTable() + "` (manifest_id,type_id,employee_id,started_On,group_id,comments,workstation,location,conditions) VALUES((SELECT id FROM `" + JsonHandler.getSelJob().getJob_id() + "" + DBUtils.DBTable.M.getTable() + "` WHERE `" + uid + "`='" + itemCombo.getSelectionModel().getSelectedItem().getName() + "'),(SELECT id FROM item_types WHERE name='" + item.getType().getText() + "'),?,?,?,?,(SELECT id FROM workstation WHERE name='" + COMP_NAME + "'),1,?)");
            Date now = formatDateTime(item.getStarted_On());
            ps.setInt(1, ConnectionHandler.user.getId());
            ps.setTimestamp(2, new Timestamp(now.toInstant().toEpochMilli()));
            ps.setInt(3, item.getGroup().getID());
            ps.setString(4, item.getComments());
            ps.setString(5, item.getConditions().toString().replaceAll("[\\[|\\]]", ""));
            ps.executeUpdate();
        } catch (SQLException | ParseException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(set);
            DbUtils.closeQuietly(ps);
            DbUtils.closeQuietly(connection);
            updateItemProps(item);
        }
        return getManifest_ID();
    }

    @Override
    public void updateItem(String paramString1, String paramString2, Item paramItem) {

    }

    @Override
    public void updateName(String paramString1, String paramString2, Item paramItem) {

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        maniViewController = this;
        tree.setRoot(rootItem);
        afterInitialize();
    }

    @PostConstruct
    @Override
    public void afterInitialize() {
        super.afterInitialize();
        super.nameColumn.setEditable(false);
        itemInfo.setCellFactory(e -> {
            return new TreeCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null) {
                        //Parent
                        if (!getTreeItem().isLeaf()) {
                            setAlignment(Pos.CENTER_LEFT);
                            setStyle("-fx-font-weight:bold;-fx-padding:8 0 0 2;-fx-font-size: 1.0em;");
                        } else {
                            setAlignment(Pos.CENTER_LEFT);
                            setStyle("-fx-font-weight:none;-fx-padding:8 24 4 0;-fx-font-size: 1.1em;");
                        }
                        Label label = new Label(item);
                        label.setWrapText(true);
                        label.setMaxWidth(1000);
                        label.textOverrunProperty().set(OverrunStyle.CLIP);
                        setGraphic(label);
                        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    } else {
                        setGraphic(null);
                        setText("");
                    }
                }
            };
        });
        itemInfo.setPrefHeight(400);
        itemInfo.setMinHeight(400);

        tree.setEditable(false); //The View shouldn't be editable for Manifest Mode
        itemInfo.setRoot(new TreeItem<String>());
        groupTree.getSelectionModel().selectedItemProperty().addListener((ob, ov, nv) -> {
            if (nv != null) {
                groupSelectTask(nv.getValue(), tree);
            }
        });
        itemCombo.getSelectionModel().selectedItemProperty().addListener((ob, ov, nv) -> {
            if (nv != null) {
                ObservableList<TreeItem<String>> info = Utils.getItemInfo("id=" + nv.getId());
                Platform.runLater(() -> {
                    itemInfo.getRoot().getChildren().setAll(info);
                });
            }
        });
        itemCombo.setCellFactory(e -> {
            ListCell<EntryItem> col = new ListCell<EntryItem>() {
                @Override
                protected void updateItem(EntryItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !item.getName().isEmpty()) {
                        setText(item.getName());
                    } else {
                        setText(null);
                        setGraphic(null);
                    }
                }
            };
            return col;
        });

        itemCombo.setButtonCell(new ListCell<EntryItem>() {
            @Override
            protected void updateItem(EntryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !item.getName().isEmpty()) {
                    setText(item.getName());
                } else {
                    setText("");
                    setGraphic(null);
                }
            }
        });

        itemCombo.setConverter(new StringConverter<EntryItem>() {
            @Override
            public String toString(EntryItem object) {
                return object.getName();
            }

            @Override
            public EntryItem fromString(String string) {
                return itemCombo.getItems().stream().filter(e -> e.getName().equals(string)).findAny().orElse(null);
            }
        });

    }

    @Override
    public void updateTotal() {

    }

    @Override
    public void groupSelectTask(Group nv, JFXTreeTableView<? extends Item> tree) {
        boolean sameVal = false;
        if (nv != null && !nv.getName().isEmpty()) {
            if (!nv.getName().equals(addGroupLabel)) {
                if (ControllerHandler.selGroup != null && nv == ControllerHandler.selGroup) {
                    sameVal = true;
                }
                if (!sameVal) {
                    //Daily Log already initialized
                    ControllerHandler.selGroup = nv;
                    groupCountProp.set(0);
                    tree.getRoot().getChildren().clear();
                    selGroup.setText(nv.getName());
                    tree.setPlaceholder(indicator);
                    tree.getPlaceholder().autosize();
                    try {
                        itemCombo.getItems().clear();
                    } catch (IndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }
                    CompletableFuture.supplyAsync(() -> {
                        ObservableList<?> entryItems = getGroupItems(ControllerHandler.selGroup);
                        return entryItems;
                    }).thenApplyAsync(entryItems -> {
                        ObservableList group = ControllerHandler.selGroup.getItemList();
                        group.setAll(entryItems);
                        Platform.runLater(() -> {
                            itemCombo.getSelectionModel().clearSelection();
                        });
                        List itemList = entryItems.stream().map(TreeItem::new).collect(Collectors.toList());
                        return itemList;
                    }).thenApplyAsync(itemList -> {
                        tree.getRoot().getChildren().addAll(itemList);
                        return itemList;
                    }).thenRunAsync(() -> {
                        Platform.runLater(() -> {
                            groupCountProp.set(countGroupTotal());
                            tree.refresh();
                        });
                    });

                    CompletableFuture.runAsync(() -> {
                        ObservableList<EntryItem> comboItems = getItemsForCombo(nv);
                    });


                }
                insertBtn.setDisable(false);
            }
        }
    }

    public void sortItems(ObservableList<EntryItem> comboItems) {
        FXCollections.sort(comboItems, new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
    }

    public ObservableList<EntryItem> getItemsForCombo(Group group) {
        Connection connection = null;
        ResultSet set = null;
        PreparedStatement ps = null;
        ObservableList<EntryItem> group_items = FXCollections.observableArrayList();
        Map<Integer, String> items = new HashedMap<>();
        try {
            connection = ConnectionHandler.createDBConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement("SELECT m.id, TRIM(m." + uid + ") as item FROM  `" + JsonHandler.getSelJob().getJob_id() + "" + DBUtils.DBTable.M.getTable() + "` m LEFT JOIN `" + JsonHandler.getSelJob().getJob_id() + "" + DBUtils.DBTable.D.getTable() + "` d ON m.id = d.manifest_id WHERE d.employee_id IS NULL AND m.group_id=" + group.getID(), Connection.TRANSACTION_READ_UNCOMMITTED);
            set = ps.executeQuery();
            while (set.next()) {
                System.out.println(set.getInt("m.id") + " " + set.getString("item"));
                items.put(set.getInt("m.id"), set.getString("item"));
            }
            connection.commit();
        } catch (Exception e) {
            e.printStackTrace();
            Main.LOGGER.log(Level.SEVERE, "There was an error getting the groups from the db!", e);

        } finally {
            items.entrySet().parallelStream().forEach((k -> {
                int id = k.getKey();
                String val = k.getValue();
                EntryItem item = new EntryItem(id, group.getCollection(), group, val, 0, 0, "", false, "", "", "", "");
                Platform.runLater(() -> {
                    itemCombo.getItems().add(item);
                });
            }));
            Platform.runLater(() -> {
                if (!itemCombo.getItems().isEmpty()) {
                    sortItems(itemCombo.getItems());
                }
            });

            DbUtils.closeQuietly(set);
            DbUtils.closeQuietly(connection);
            DbUtils.closeQuietly(ps);

        }

        return group_items;
    }

    @Override
    public ObservableList<? extends Item> getGroupItems(Group group) {
        Connection connection = null;
        ResultSet set = null;
        PreparedStatement ps = null;
        ObservableList<EntryItem> group_items = FXCollections.observableArrayList();
        AtomicInteger progress = new AtomicInteger(0);
        try {
            connection = ConnectionHandler.createDBConnection();
            ps = connection.prepareStatement("SELECT w.name,d.id,d.manifest_id,g.id as group_id, g.name as group_name, TRIM(m.`" + uid + "`) as item,d.non_feeder, d.completed, e.name as employee, d.total, t.name as type,d.conditions,d.comments,d.started_On,d.completed_On FROM `" + JsonHandler.getSelJob().getJob_id() + "" + DBUtils.DBTable.D.getTable() + "` d INNER JOIN employees e ON d.employee_id = e.id INNER JOIN `" + JsonHandler.getSelJob().getJob_id() + "" + DBUtils.DBTable.G.getTable() + "` g ON d.group_id = g.id INNER JOIN item_types t ON d.type_id = t.id INNER JOIN `" + JsonHandler.getSelJob().getJob_id() + "" + DBUtils.DBTable.M.getTable() + "` m ON m.id=d.manifest_id INNER JOIN workstation w ON w.id=d.workstation WHERE d.group_id=" + group.getID() + " AND employee_id=" + ConnectionHandler.user.getId());
            set = ps.executeQuery();
            while (set.next()) {
                final EntryItem item = new EntryItem(set.getInt("d.manifest_id"), group.getCollection(), group, set.getString("item"), set.getInt("d.total"), set.getInt("d.non_feeder"), set.getString("type"), set.getInt("d.completed") == 1, set.getString("d.comments"), set.getString("d.started_On"), set.getString("d.completed_On"), set.getString("w.name"));
                final String condition = set.getString("d.conditions");
                if (condition != null && !condition.isEmpty()) {
                    final String[] splitConditions = condition.split(", ");
                    item.getConditions().setAll(Arrays.asList(splitConditions));
                }
                group_items.add(item);
                Platform.runLater(() -> {
                    indicator.setProgress(progress.get());
                    progress.incrementAndGet();
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Main.LOGGER.log(Level.SEVERE, "There was an error getting the groups from the db!", e);

        } finally {
            DbUtils.closeQuietly(set);
            DbUtils.closeQuietly(ps);
            DbUtils.closeQuietly(connection);
        }

        return group_items;
    }

    @Override
    public void resetFields() {
        conditCombo.getCheckModel().clearChecks();
        itemInfo.getRoot().getChildren().clear();
    }

    public void deleteItem(Item item) {
        Connection connection = null;
        ResultSet set = null;
        PreparedStatement ps = null;
        try {
            connection = ConnectionHandler.createDBConnection();
            ps = connection.prepareStatement("DELETE FROM `" + JsonHandler.getSelJob().getJob_id() + "" + DBUtils.DBTable.D.getTable() + "` WHERE manifest_id=?");
            ps.setInt(1, item.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(set);
            DbUtils.closeQuietly(ps);
            DbUtils.closeQuietly(connection);
        }
    }

    @Override
    public void legalTextTest(boolean isLegal, CustomTextField node) {

    }

    public AnchorPane getRoot() {
        return root;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getGroupCol() {
        return groupCol;
    }

    public void setGroupCol(String groupCol) {
        this.groupCol = groupCol;
    }

    public SearchableComboBox<EntryItem> getItemCombo() {
        return itemCombo;
    }

}
