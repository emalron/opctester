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

    public Node(Integer namespace, String id, String name, int idType) {
        this.namespace = namespace;
        this.id = id;
        this.name = name;
        switch(idType) {
            case 0:
                this.type = "i";
                break;
            case 1:
                this.type = "s";
                break;
            default:
                this.type = "s";
        }
    }  
}
