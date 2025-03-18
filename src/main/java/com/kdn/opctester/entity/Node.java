package com.kdn.opctester.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Node {
    Integer namespace;
    Integer id;

    public Node(Integer namespace, Integer id) {
        this.namespace = namespace;
        this.id = id;
    }
}
