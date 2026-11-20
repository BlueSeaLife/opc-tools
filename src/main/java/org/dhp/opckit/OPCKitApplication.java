package org.dhp.opckit;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.BuiltinDataType;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.da.Server;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

@Slf4j
public class OPCKitApplication extends Application {

    private TableView<NodeItem> table;

    private TextField uaAddress;
    private TextField uaPort;
    private TextField daAddress;
    private TextField daClsid;
    private RadioButton rbUa;
    private TextField username;
    private PasswordField password;

    @Override
    public void start(Stage primaryStage) {
        ToggleGroup protocolGroup = new ToggleGroup();
        rbUa = new RadioButton("UA协议");
        RadioButton rbDa = new RadioButton("DA协议");
        rbUa.setToggleGroup(protocolGroup);
        rbDa.setToggleGroup(protocolGroup);
        rbUa.setSelected(true);

        // 协议选择行
        HBox protocolRow = new HBox(10, rbUa, rbDa);
        protocolRow.setStyle("-fx-padding: 5 0 5 0;");

        // 服务器信息动态区域
        uaAddress = new TextField("opc.tcp://127.0.0.1");
        uaPort = new TextField("49320");
        daAddress = new TextField("192.168.31.139");
        daClsid = new TextField("7BC0CC8E-482C-47CA-ABDC-0FE7F9C6E729");

        String[] uaLabels= {"服务地址Url:", "服务端口Port:"};
        String[] daLabels= {"服务器IP:", "服务组件ID:"};
        TextField[] uaFields = {uaAddress, uaPort};
        TextField[] daFields = {daAddress, daClsid};
        HBox uaBox = createServerBox(uaLabels,uaFields);
        uaBox.setAlignment(Pos.CENTER_LEFT);
        HBox daBox = createServerBox(daLabels,daFields);
        daBox.setAlignment(Pos.CENTER_LEFT);
        StackPane switchableContainer = new StackPane();
        switchableContainer.getChildren().addAll(uaBox, daBox);

        VBox serverContainer = new VBox(5);
        serverContainer.getChildren().addAll(protocolRow, switchableContainer);
        uaBox.visibleProperty().bind(rbUa.selectedProperty());
        daBox.visibleProperty().bind(rbDa.selectedProperty());
        // 添加对齐设置确保位置一致
        StackPane.setAlignment(uaBox, Pos.TOP_LEFT);
        StackPane.setAlignment(daBox, Pos.TOP_LEFT);

        // 登录类型行
        RadioButton rbAnonymous = new RadioButton("匿名登录");
        RadioButton rbUserPass = new RadioButton("用户名密码登录");
        ToggleGroup loginGroup = new ToggleGroup();
        rbAnonymous.setToggleGroup(loginGroup);
        rbUserPass.setToggleGroup(loginGroup);
        rbUserPass.setSelected(true);
        username = new TextField("ua_user");
        password = new PasswordField();
        password.setText("123456");
        HBox loginRow = new HBox(10, rbAnonymous, rbUserPass);

        // 用户信息行
        HBox authBox = new HBox(10, new Label("用户名:"), username, new Label("密码:"), password);
        authBox.setAlignment(Pos.CENTER_LEFT);
        authBox.visibleProperty().bind(rbUserPass.selectedProperty());
        authBox.managedProperty().bind(rbUserPass.selectedProperty());

        //按钮行
        Button testConnection = new Button("测试连接");
        Button updateNodes = new Button("更新节点");
        Button exportBtn = new Button("导出");
        Button importBtn = new Button("导入");
        testConnection.setOnAction(e -> testConnection());
        updateNodes.setOnAction(e -> updateNodes());
        exportBtn.setOnAction(e -> exportData());
        importBtn.setOnAction(e -> importData());
        importBtn.setDisable(true);
        HBox buttonRow = new HBox(10, testConnection, updateNodes, exportBtn, importBtn);

        // 节点列表
        table = new TableView<>();
        List<TableColumn<NodeItem, ?>> columns = new ArrayList<>();
        columns.add(createColumn("序号", "index", 50));
        columns.add(createColumn("nodeName", "nodeName", 200));
        columns.add(createColumn("nodeClass", "nodeClass", 100));
        columns.add(createColumn("namespaceIndex", "namespaceIndex", 200));
        columns.add(createColumn("identifier", "identifier", 200));
        table.getColumns().addAll(columns);

        // 组合布局
        VBox root = new VBox(10,
                protocolRow,
                serverContainer,
                loginRow,
                authBox,
                buttonRow,
                table
        );
        root.setStyle("-fx-padding: 10;");
        // 加载图标文件
        Image iconImage = new Image(Objects.requireNonNull(getClass().getResource("/icons/ceu-avatar.png")).toExternalForm());
        // 设置窗口图标
        primaryStage.getIcons().add(iconImage);
        Scene scene = new Scene(root, 1000, 600);
        // 加载CSS文件
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("OPC UA/DA 测试工具");
        primaryStage.show();
    }

    private HBox createServerBox(String[] labels, TextField[] fields) {
        HBox box = new HBox(5);
        for(int i = 0; i < labels.length; i++){
            box.getChildren().add(new Label(labels[i]));
            TextField field = fields[i];
            field.setPrefWidth(300);
            box.getChildren().add(field);
        }
        return box;
    }

    private void testConnection() {
        if (rbUa.isSelected()) {
            try {
                OpcUaClient client = createClient(uaAddress.getText(), uaPort.getText(), username.getText(), password.getText());
                client.connect().get();
                showAlert("UA连接成功", Alert.AlertType.INFORMATION);
                client.disconnect().get();
            } catch (Exception ex) {
                showAlert("UA连接失败: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            try {
                Server server = createServer();
                server.connect();
                showAlert("DA连接成功", Alert.AlertType.INFORMATION);
                server.disconnect();
            } catch (Exception e) {
                showAlert("DA连接失败: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    /**
     * 创建OpcDa服务对象
     * @return OpcDa服务对象
     */
    public Server createServer() {
        ConnectionInformation ci = new ConnectionInformation();
        ci.setHost(daAddress.getText());
        ci.setUser(username.getText());
        ci.setPassword(password.getText());
        ci.setClsid(daClsid.getText());
        return new Server(ci, Executors.newSingleThreadScheduledExecutor());
    }

    private void updateNodes() {
        if (rbUa.isSelected()) {
            try {
                OpcUaClient client = createClient(uaAddress.getText(), uaPort.getText(), username.getText(), password.getText());
                client.connect().get();

                table.getItems().clear();
                List<NodeItem> NodeItemList = browseChannel(client);
                int index = 1;
                for (NodeItem nodeItem : NodeItemList) {
                    table.getItems().add(new NodeItem(
                            String.valueOf(index++),
                            nodeItem.getNodeName(),
                            nodeItem.getNodeClass(),
                            nodeItem.getNamespaceIndex(),
                            nodeItem.getIdentifier()
                    ));
                }
                client.disconnect().get();
            } catch (Exception ex) {
                showAlert("节点更新失败: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            try {
                Server server = createServer();
                server.connect();

                table.getItems().clear();
                int index = 1;
                for (String itemId : server.getFlatBrowser().browse()) {
                    if(!itemId.contains("_") && !itemId.contains("示例")) {
                        table.getItems().add(new NodeItem(
                                String.valueOf(index++),
                                itemId,
                                "Variable",
                                "2",
                                itemId
                        ));
                    }
                }
                server.disconnect();
            } catch (Exception e) {
                showAlert("DA节点获取失败: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void exportData() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("nodes_export.csv"))) {
            writer.println("index,nodeName,nodeClass,namespaceIndex,identifier");
            for (NodeItem item : table.getItems()) {
                writer.printf("%s,%s,%s,%s,%s",
                        item.getIndex(),
                        item.getNodeName(),
                        item.getNodeClass(),
                        item.getNamespaceIndex(),
                        item.getIdentifier());
            }
            showAlert("导出成功", Alert.AlertType.INFORMATION);
        } catch (IOException e) {
            showAlert("导出失败: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void importData() {
        try (BufferedReader reader = new BufferedReader(new FileReader("nodes_export.csv"))) {
            table.getItems().clear();
            reader.readLine(); // 跳过表头
            String lineContent;
            while ((lineContent = reader.readLine()) != null) {
                String[] parts = lineContent.split(",");
                if (parts.length == 5) {
                    table.getItems().add(new NodeItem(parts[0], parts[1], parts[2], parts[3], parts[4]));
                }
            }
            showAlert("导入成功", Alert.AlertType.INFORMATION);
        } catch (IOException e) {
            showAlert("导入失败: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.showAndWait();
    }

    private TableColumn<NodeItem, String> createColumn(String title, String prop, int width) {
        TableColumn<NodeItem, String> col = new TableColumn<>(title);
        col.setMinWidth(width);
        col.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(prop));
        return col;
    }


    /**
     * 获取通道列表
     * @param client 客户端
     * @return 返回通道列表
     */
    public static List<NodeItem> browseChannel(OpcUaClient client) {
        return browseNodesRecursively(client, Identifiers.RootFolder);
    }

    /**
     * 获取通道列表
     * @param client 客户端
     * @param nodeId 节点ID
     * @return 返回通道列表
     */
    public static List<NodeItem> browseNodesRecursively(OpcUaClient client, NodeId nodeId) {
        List<NodeItem> tagNodes = new ArrayList<>();
        List<? extends UaNode> childNodes;
        try {
            childNodes = client.getAddressSpace().browseNodes(nodeId);
            for (UaNode childNode : childNodes) {
                if(Identifiers.RootFolder.equals(nodeId)){
                    if (Objects.requireNonNull(childNode.getBrowseName().getName()).contains("_")) {
                        System.out.println("**********************节点 " + nodeId + " 系统子节点*********************");
                        continue;
                    }
                }else{
                    if (Objects.requireNonNull(childNode.getBrowseName().getName()).contains("_")||(childNode.getNodeId().getIdentifier().toString()).contains("示例")||childNode.getNodeId().getNamespaceIndex().toString().equals("0")) {
                        continue;
                    }
                }
                System.out.println("nodeId="+childNode.getNodeId().getIdentifier()+";nodeName="+childNode.getBrowseName().getName());
                if(childNode.getNodeClass().equals(NodeClass.Variable)){
                    NodeItem tagNodeId = new NodeItem();
                    NodeId serverNodeId = new NodeId(childNode.getNodeId().getNamespaceIndex(), childNode.getNodeId().getIdentifier().toString());
                    NodeId serverNode = client.getAddressSpace().getVariableNode(serverNodeId).getDataType();
                    BuiltinDataType type = BuiltinDataType.fromNodeId(serverNode);
                    tagNodeId.setDataType(type!=null?type.getBackingClass().getSimpleName():"");
                    getNodeInfo(childNode, tagNodeId);
                    tagNodes.add(tagNodeId);
                }else{
                    List<NodeItem> childTags= browseNodesRecursively(client, childNode.getNodeId());
                    tagNodes.addAll(childTags);  // 关键合并操作
                }
            }
        } catch (UaException e) {
            log.error("browseNodesRecursively error", e);
        }
        return tagNodes;
    }

    /**
     * 获取节点信息
     * @param nd 节点
     * @param nodeId 节点ID
     */
    public static void getNodeInfo(UaNode nd, NodeItem nodeId) {
        nodeId.setNamespaceIndex(nd.getNodeId().getNamespaceIndex().toString());
        nodeId.setIdentifier(nd.getNodeId().getIdentifier().toString());
        nodeId.setNodeName(nd.getBrowseName().getName());
        nodeId.setNodeClass(nd.getNodeClass().toString());
    }


    /**
     * 创建Opcua客户端
     * @return Opcua客户端
     * @throws Exception 异常
     */
    public OpcUaClient createClient(String uaAddress,String uaPort, String username, String password) throws Exception {
        return OpcUaClient.create(uaAddress+":"+uaPort,
                endpoints -> endpoints.stream().filter(
                        e -> e.getSecurityPolicyUri().equals(SecurityPolicy.None.getUri())).findFirst(),
                configBuilder -> configBuilder
                        .setApplicationName(LocalizedText.english("eclipse milo opc-ua client"))
                        .setApplicationUri("urn:eclipse:milo:examples:client")
                        .setIdentityProvider(new AnonymousProvider())
                        .setIdentityProvider(new UsernameProvider(username,password))
                        .setRequestTimeout(UInteger.valueOf(500)).build());
    }

    public static void main(String[] args) {
        launch();
    }
}