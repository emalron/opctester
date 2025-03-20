package com.kdn.opctester;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.client.AddressSpace;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdn.opctester.entity.TreeNode;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
class OpctesterApplicationTests {

	List<TreeNode> nodes = new ArrayList<>();

	void contextLoads() {
		String endpointUrl = "opc.tcp://192.168.0.3:19800/ISP/JISP-Server";
		try {
			OpcUaClient client = OpcUaClient.create(endpointUrl);
			client.connect().get();
			AddressSpace addressSpace = client.getAddressSpace();
			// NodeId startNodeId = Identifiers.ObjectsFolder;
			NodeId startNodeId = new NodeId(3, "3:100017");

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

			ObjectMapper objectMapper = new ObjectMapper();

			String jsonString = objectMapper.writeValueAsString(root);

			System.out.println(jsonString);
		} catch (UaException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	@Test
	void read_data_test() {
		String endpointUrl = "opc.tcp://192.168.0.3:19800/ISP/JISP-Server";
		try {
			OpcUaClient client = OpcUaClient.create(endpointUrl);
			client.connect().get();
			NodeId startNodeId = new NodeId(3, "3:100001");

			DataValue dvalue = client.readValue(0, TimestampsToReturn.Both, startNodeId).get();

			System.out.println(dvalue);
			System.out.println("TIME COUNT");
			System.out.println("status: " + dvalue.getStatusCode().toString());
			System.out.println("value?: " + dvalue.getValue().getValue().toString());
			System.out.println("value?: " + dvalue.getValue());
		} catch (UaException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	public TreeNode createTreeNode(ReferenceDescription ref) {
		TreeNode treeNode = new TreeNode();

		// long nodeId = Long.parseLong(ref.getNodeId().getIdentifier().toString());
		String nodeId = ref.getNodeId().getIdentifier().toString();

		treeNode.setName(ref.getBrowseName().getName());
		treeNode.setNodeClass(ref.getNodeClass().name());
		treeNode.setIdentifier(nodeId);
		treeNode.setNamespaceIndex(ref.getNodeId().getNamespaceIndex().intValue());
		return treeNode;
	}

	public void browseNodes(AddressSpace addressSpace, NodeId nodeId, TreeNode parent) {
		try {
			List<ReferenceDescription> references = addressSpace.browse(nodeId);
			System.out.println("====================");
			System.out.println(references.size());
			System.out.println("====================");
			for(ReferenceDescription ref : references) {
				NodeId childId = ref.getNodeId().toNodeId(null).get();
				TreeNode childTreeNode = createTreeNode(ref);
				parent.getChildren().add(childTreeNode);
				// browseNodes(addressSpace, childId, childTreeNode);
			}
		} catch (UaException e) {
			e.printStackTrace();
		}
	}

	public void leaves_test() {
		String endpointUrl = "opc.tcp://192.168.0.114:4841/freeopcua/server";
		try {
			OpcUaClient client = OpcUaClient.create(endpointUrl);
			client.connect().get();
			AddressSpace addressSpace = client.getAddressSpace();
			// NodeId startNodeId = Identifiers.ObjectsFolder;
			NodeId startNodeId = new NodeId(0, 24087);
			
			List<Integer> children = new ArrayList<>();

			browseLeaves(addressSpace, startNodeId, children);

			ObjectMapper objectMapper = new ObjectMapper();

			String jsonString = objectMapper.writeValueAsString(children);

			System.out.println("=============result=============");
			System.out.println(jsonString);
			System.out.println("================================");
		} catch (UaException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	private void browseLeaves(AddressSpace addressSpace, NodeId nodeId, List<Integer> leaves) {
        try {
			List<ReferenceDescription> references = addressSpace.browse(nodeId);
			for(ReferenceDescription ref : references) {
				NodeId childId = ref.getNodeId().toNodeId(null).get();
                browseLeaves(addressSpace, childId, leaves);
			}
			
			if(references.size() == 0) {
				Integer id_ = Integer.valueOf(nodeId.getIdentifier().toString());
            	leaves.add(id_);
			}
		} catch (UaException e) {
			e.printStackTrace();
		}
    }

}
