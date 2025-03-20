package com.kdn.opctester.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Node {
    Integer namespace;
    String id;
    String type;
    String name;

    public Node(Integer namespace, String id, String name) {
        this.namespace = namespace;
        this.id = id;
        this.name = name;
    }  
}
