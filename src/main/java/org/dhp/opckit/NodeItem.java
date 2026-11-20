package org.dhp.opckit;

public class NodeItem {
    private String index;

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeClass() {
        return nodeClass;
    }

    public void setNodeClass(String nodeClass) {
        this.nodeClass = nodeClass;
    }

    public String getNamespaceIndex() {
        return namespaceIndex;
    }

    public void setNamespaceIndex(String namespaceIndex) {
        this.namespaceIndex = namespaceIndex;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    private String nodeName;
    private String nodeClass;
    private String namespaceIndex;
    private String identifier;
    private String dataType;

    public NodeItem(String index, String nodeName, String nodeClass, String namespaceIndex, String identifier) {
        this.index = index;
        this.nodeName = nodeName;
        this.nodeClass = nodeClass;
        this.namespaceIndex = namespaceIndex;
        this.identifier = identifier;
    }

    public NodeItem() {

    }

}
