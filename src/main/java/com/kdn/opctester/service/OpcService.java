package com.kdn.opctester.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
import com.kdn.opctester.entity.Node;
import com.kdn.opctester.entity.TreeNode;

import jakarta.annotation.PreDestroy;

@Service
public class OpcService {
    private String endpointUrl;
    private OpcUaClient client;
    private final Object clientLock = new Object();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<NodeId> leaves = new ArrayList<>();

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

    public String browseOneDepth(Integer namespace, Integer identifier) {
		try {
            ensureConnection();

			AddressSpace addressSpace = client.getAddressSpace();

            NodeId startNodeId = new NodeId(0, identifier);			

			QualifiedName browseName = client.getAddressSpace().getNode(startNodeId).getBrowseName();
			String rootName = browseName != null ? browseName.getName() : "Objects";
			long nodeId = Long.valueOf(startNodeId.getIdentifier().toString());
			
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

    private void browseNodes(AddressSpace addressSpace, NodeId nodeId, TreeNode parent) {
        try {
			List<ReferenceDescription> references = addressSpace.browse(nodeId);
			for(ReferenceDescription ref : references) {
				TreeNode childTreeNode = createTreeNode(ref);
				parent.getChildren().add(childTreeNode);

                // NodeId childId = ref.getNodeId().toNodeId(null).get();
				// browseNodes(addressSpace, childId, childTreeNode);
			}
		} catch (UaException e) {
			e.printStackTrace();
		}
    }

    public TreeNode createTreeNode(ReferenceDescription ref) {
		TreeNode treeNode = new TreeNode();

		long nodeId = Long.parseLong(ref.getNodeId().getIdentifier().toString());

		treeNode.setName(ref.getBrowseName().getName());
		treeNode.setNodeClass(ref.getNodeClass().name());
		treeNode.setIdentifier(nodeId);
		treeNode.setNamespaceIndex(ref.getNodeId().getNamespaceIndex().intValue());
		return treeNode;
	}

    private void initializeClient() throws UaException, InterruptedException, ExecutionException {
        synchronized(clientLock) {
            if(client != null) {
                try {
                    client.disconnect();
                } catch(Exception e) {
                    // pass
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

    private void traverseLeaves(AddressSpace addressSpace, NodeId nodeId, List<Node> leaves) {
        try {
			List<ReferenceDescription> references = addressSpace.browse(nodeId);
			for(ReferenceDescription ref : references) {
				NodeId childId = ref.getNodeId().toNodeId(null).get();
                traverseLeaves(addressSpace, childId, leaves);
			}
			
			if(references.size() == 0) {
				Integer id_ = Integer.valueOf(nodeId.getIdentifier().toString());
                Integer namespace_ = nodeId.getNamespaceIndex().intValue();
            	leaves.add(new Node(namespace_, id_));
			}
		} catch (UaException e) {
			e.printStackTrace();
		}
    }

    public Map<Integer, Object> pollInternal() {
        if(leaves.size() > 0) {
            try {
                Map<Integer,Object> values = new HashMap<>();
                System.out.println("=========================");
                System.out.println(leaves.get(0));
                System.out.println("=========================");
                List<DataValue> dataValues = client.readValues(0, TimestampsToReturn.Both, leaves).get();
                
                for(int i=0; i<leaves.size(); i++) {
                    NodeId nodeId_ = leaves.get(i);
                    DataValue dataValue_ = dataValues.get(i);

                    Integer id_ = Integer.valueOf(nodeId_.getIdentifier().toString());
                    Object value_ = dataValue_.getValue().getValue();

                    if(dataValue_ != null) {
                        values.put(id_, value_);
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
        Map<Integer,Object> result = pollInternal();
        try {
            String jsonString = objectMapper.writeValueAsString(result);
            return jsonString;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new String();
    }
}
