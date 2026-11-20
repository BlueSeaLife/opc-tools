module org.dhp.opckit {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires org.eclipse.milo.opcua.sdk.client;
    requires org.openscada.opc.lib;
    requires org.eclipse.milo.opcua.stack.core;
    requires org.slf4j;
    requires static lombok;

    opens org.dhp.opckit to javafx.fxml;
    exports org.dhp.opckit;
}