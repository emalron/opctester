package com.kdn.opctester.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.milo.opcua.sdk.client.AddressSpace;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdn.opctester.config.CorsConfig;
import com.kdn.opctester.entity.Node;
import com.kdn.opctester.entity.TreeNode;
import jakarta.annotation.PreDestroy;

@Service
public class OpcService {

    private final CorsConfig corsConfig;
    private String endpointUrl;
    private OpcUaClient client;
    private final Object clientLock = new Object();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<NodeId> leaves = new ArrayList<>();
    private Map<String,String> names2 = new HashMap<>();
    private final BlockingQueue<Map<String,Map<String,Object>>> queue = new LinkedBlockingQueue<>(1000);
    private final String prefix = "data_";
    private ScheduledExecutorService executorService;

    OpcService(CorsConfig corsConfig) {
        this.corsConfig = corsConfig;
    }
    @PreDestroy
    public void cleanup() {
        if(client != null) {
            client.disconnect();
        }
    }

    public boolean connect(String url) {
        this.endpointUrl = url;
        try {
            initializeClient();
            leaves = new ArrayList<>();
            // executorService = Executors.newScheduledThreadPool(1);
            // executorService.scheduleAtFixedRate(this::filesave, 0, 500, TimeUnit.MILLISECONDS);
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String isConnected() {
        boolean isConnected = client != null;
        Map<String,Boolean> result = Map.of("isConnected", isConnected);
        String jsonString = null;
        try {
            jsonString = objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return jsonString;
    }

    public String browseOneDepth(Integer namespace, String identifier) {
        try {
            ensureConnection();

            AddressSpace addressSpace = client.getAddressSpace();

            NodeId startNodeId = new NodeId(namespace, identifier);			

            QualifiedName browseName = client.getAddressSpace().getNode(startNodeId).getBrowseName();
            String rootName = browseName != null ? browseName.getName() : "Objects";
            // long nodeId = Long.valueOf(startNodeId.getIdentifier().toString());
            String nodeId = startNodeId.getIdentifier().toString();
            
            TreeNode root = new TreeNode();
            root.setName(rootName);
            root.setNodeClass(NodeClass.Object.toString());
            root.setNamespaceIndex(startNodeId.getNamespaceIndex().intValue());
            root.setIdentifier(nodeId);

            browseNodes(addressSpace, startNodeId, root);

            String jsonString = objectMapper.writeValueAsString(root);

            return jsonString;
        } catch (UaException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String browseOneDepth(Integer namespace, String type, String identifier) {
        try {
            ensureConnection();

            AddressSpace addressSpace = client.getAddressSpace();

            NodeId startNodeId;
            switch(type) {
                case "i":
                    startNodeId = new NodeId(namespace, Integer.valueOf(identifier));
                    break;
                case "s":
                    startNodeId = new NodeId(namespace, identifier);
                    break;
                default:
                    startNodeId = new NodeId(namespace, identifier);
            }

            QualifiedName browseName = client.getAddressSpace().getNode(startNodeId).getBrowseName();
            String rootName = browseName != null ? browseName.getName() : "Objects";
            // long nodeId = Long.valueOf(startNodeId.getIdentifier().toString());
            String nodeId = startNodeId.getIdentifier().toString();
            String idType = startNodeId.getType().getValue() == 0 ? "i" : "s";
            
            TreeNode root = new TreeNode();
            root.setName(rootName);
            root.setNodeClass(NodeClass.Object.toString());
            root.setNamespaceIndex(startNodeId.getNamespaceIndex().intValue());
            root.setIdentifier(nodeId);
            root.setIdType(idType);

            browseNodes(addressSpace, startNodeId, root);

            String jsonString = objectMapper.writeValueAsString(root);

            return jsonString;
        } catch (UaException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void browseNodes(AddressSpace addressSpace, NodeId nodeId, TreeNode parent) {
        try {
            List<ReferenceDescription> references = addressSpace.browse(nodeId);
            parent.setHasChildren(references.size() > 0);
            for(ReferenceDescription ref : references) {
                TreeNode childTreeNode = createTreeNode(ref);
                List<ReferenceDescription> refs_ = addressSpace.browse(ref.getNodeId().toNodeIdOrThrow(null));
                childTreeNode.setHasChildren(refs_.size() > 0);
                parent.getChildren().add(childTreeNode);

                // NodeId childId = ref.getNodeId().toNodeId(null).get();
                // browseNodes(addressSpace, childId, childTreeNode);
            }
        } catch (UaException e) {
            e.printStackTrace();
        } catch (Exception e) {
                    e.printStackTrace();
                }
    }

    public TreeNode createTreeNode(ReferenceDescription ref) {
        TreeNode treeNode = new TreeNode();

        // long nodeId = Long.parseLong(ref.getNodeId().getIdentifier().toString());
        String nodeId = ref.getNodeId().getIdentifier().toString();
        String idType = ref.getNodeId().getType().getValue() == 0 ? "i" : "s";

        treeNode.setName(ref.getBrowseName().getName());
        treeNode.setNodeClass(ref.getNodeClass().name());
        treeNode.setIdentifier(nodeId);
        treeNode.setNamespaceIndex(ref.getNodeId().getNamespaceIndex().intValue());
        treeNode.setIdType(idType);
        return treeNode;
    }

    private void initializeClient() throws UaException, InterruptedException, ExecutionException {
        synchronized(clientLock) {
            if(client != null) {
                try {
                    client.disconnect();
                } catch(Exception e) {
                    // pass
                    e.printStackTrace();
                }
            }

            client = OpcUaClient.create(endpointUrl);
            client.connect().get();

            System.out.println("Connection state: " + client.connect().state());
        }
    }

    private void ensureConnection() throws UaException, InterruptedException, ExecutionException {
        synchronized(clientLock) {
            if(client == null) {
                initializeClient();
                return;
            }

            try {
                NodeId serverStateNodeId = Identifiers.Server_ServerStatus_State;
                DataValue value = client.readValue(0.0, TimestampsToReturn.Both, serverStateNodeId).get();
                System.out.println("===============connection test===============");
                System.out.println("Name: Server_ServerStatus_State");
                System.out.println("Value: " + value.getValue().getValue().toString());
                System.out.println("=============================================");
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String browseLeaves(Integer namespace, Integer identifier) {
        try {
            ensureConnection();

            AddressSpace addressSpace = client.getAddressSpace();

            NodeId startNodeId = Identifiers.ObjectsFolder;
            if(identifier != null && identifier != 0) {
                startNodeId = new NodeId(namespace, identifier);
            }

            List<Node> result = new ArrayList<>();
            traverseLeaves(addressSpace, startNodeId, result);
            
            leaves = result.stream().map(e -> {
                return new NodeId(e.getNamespace(), e.getId());
            }).toList();

            String jsonString = objectMapper.writeValueAsString(leaves);
            return jsonString;
        } catch (UaException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e1) {
            e1.printStackTrace();
        }

        return new String();
    }

    public String browseLeaves(Integer namespace, String type, String identifier) {
        try {
            ensureConnection();

            AddressSpace addressSpace = client.getAddressSpace();

            NodeId startNodeId = Identifiers.ObjectsFolder;
            if(identifier != null && !identifier.isEmpty()) {
                startNodeId = getNodeId(namespace, type, identifier);
            }

            List<Node> result = new ArrayList<>();
            // traverseLeaves(addressSpace, startNodeId, result);
            traverseLeavesOneDepth(addressSpace, startNodeId, result);
            
            leaves = result.stream().map(e -> {
                return getNodeId(e.getNamespace(), e.getType(), e.getId());
            }).toList();

            List<DataValue> dataValues = client.readValues(0, TimestampsToReturn.Both, leaves).get();
            int i = 0;
            for(DataValue dValue : dataValues) {
                System.out.println(dValue);
                System.out.println(leaves.size() + " / " + dataValues.size());
                System.out.println(leaves.get(i++));
                System.out.println("status: " + dValue.getStatusCode().toString());
                if(dValue.getValue().getValue() == null) continue;
                System.out.println("value?: " + dValue.getValue().getValue().toString());
                System.out.println("value?: " + dValue.getValue());
            }

            String jsonString = objectMapper.writeValueAsString(leaves);
            return jsonString;
        } catch (UaException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e1) {
            e1.printStackTrace();
        }

        return new String();
    }

    private void traverseLeaves(AddressSpace addressSpace, NodeId nodeId, List<Node> leaves) {
        try {
            List<ReferenceDescription> references = addressSpace.browse(nodeId);
            for(ReferenceDescription ref : references) {
                NodeId childId = ref.getNodeId().toNodeId(null).get();
                String name = ref.getDisplayName().getText();
                // Integer id = Integer.valueOf(childId.getIdentifier().toString());
                String id_ = childId.getIdentifier().toString();
                names2.put(id_, name);
                traverseLeaves(addressSpace, childId, leaves);
            }
            
            if(references.size() == 0) {
                // Integer id_ = Integer.valueOf(nodeId.getIdentifier().toString());
                Integer namespace_ = nodeId.getNamespaceIndex().intValue();
                String id_ = nodeId.getIdentifier().toString();
                String name_ = names2.getOrDefault(id_, "Unknown");
                int idType = nodeId.getType().getValue();
                leaves.add(new Node(namespace_, id_, name_, idType));
            }
        } catch (UaException e) {
            e.printStackTrace();
        }
    }

    private void traverseLeavesOneDepth(AddressSpace addressSpace, NodeId nodeId, List<Node> tree) {
        try {
            List<ReferenceDescription> references = addressSpace.browse(nodeId);
            
            for(ReferenceDescription ref : references) {
                NodeId childId = ref.getNodeId().toNodeId(null).get();
                String name = ref.getDisplayName().getText();
                Integer namespace_ = childId.getNamespaceIndex().intValue();
                // Integer id = Integer.valueOf(childId.getIdentifier().toString());
                String id_ = childId.getIdentifier().toString();
                int idType = childId.getType().getValue();
                names2.put(id_, name);
                // traverseLeaves(addressSpace, childId, leaves);
                tree.add(new Node(namespace_, id_, name, idType));
            }
        } catch (UaException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Map<String,Object>> pollInternal() {
        if(leaves.size() > 0) {
            try {
                Map<String,Map<String,Object>> values = new HashMap<>();
                List<DataValue> dataValues = client.readValues(0, TimestampsToReturn.Both, leaves).get();
                
                for(int i=0; i<leaves.size(); i++) {
                    NodeId nodeId_ = leaves.get(i);
                    DataValue dataValue_ = dataValues.get(i);

                    // Integer id_ = Integer.valueOf(nodeId_.getIdentifier().toString());
                    String id_ = nodeId_.getIdentifier().toString();
                    Object value_ = dataValue_.getValue().getValue();
                    if(value_ == null) continue;
                    Map<String,Object> val_ = Map.of("value", value_, "name", names2.getOrDefault(id_, "Unknown"));

                    if(dataValue_ != null) {
                        values.put(id_, val_);
                    }
                }

                return values;
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        return new HashMap<>();
    }

    public String pollLeaves() {
        Map<String,Map<String,Object>> result = pollInternal();
        try {
            String jsonString = objectMapper.writeValueAsString(result);
            return jsonString;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new String();
    }

    public void enqueue(Map<String,Map<String,Object>> data) {
        queue.add(data);
    }

    public void filesave() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = now.format(formatter);

        String filename = String.format("%s%s.json", prefix, timestamp);
        String currentDirectory = System.getProperty("user.dir");
        File dir = new File(currentDirectory + File.separator + "data");
        if(!dir.exists()) {
            boolean created = dir.mkdirs();
            if(!created) {
                System.err.println("Failed to create dir: " + dir.getAbsolutePath());
            } else {
                System.out.println("Directory created successfully: " + dir.getAbsolutePath());
            }
        }
        File file = new File(dir, filename);

        try {
            Map<String,Map<String,Object>> data = queue.poll(1, TimeUnit.SECONDS);
            if(data != null) {
                String jsonData = objectMapper.writeValueAsString(data);
                try(FileWriter writer = new FileWriter(file)) {
                    writer.write(jsonData);
                    System.out.println("File saved successfully: " + file.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Couldn't take data in timeout from the queue");
            }
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupt occured while waiting data: " + e.getMessage());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private NodeId getNodeId(int namespace, String type, String identifier) {
        NodeId result = null;
        switch(type) {
            case "i":
                result = new NodeId(namespace, Integer.parseInt(identifier));
                break;
            case "s":
                result = new NodeId(namespace, identifier);
                break;
        }
        return result;
    }
}
