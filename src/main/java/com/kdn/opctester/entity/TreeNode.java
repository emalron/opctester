package com.kdn.opctester.entity;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class TreeNode {
    private String name;
    private String nodeClass;
    private int namespaceIndex;
    private long identifier;
    private boolean hasChildren;
    private List<TreeNode> children = new ArrayList<>();
}
