package com.kdn.opctester.web;

import org.springframework.web.bind.annotation.RestController;

import com.kdn.opctester.dto.UrlDto;
import com.kdn.opctester.service.OpcService;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class OpcController {

    @Autowired
    private OpcService opcService;

    @GetMapping({"/browse", "/browse/"})
    public String browse() {
        String result = opcService.browseOneDepth(0, "85");
        return result;
    }

    @GetMapping("/browse/{id}")
    public String browseById(@PathVariable("id") String id) {
        String result = opcService.browseOneDepth(0, id);
        return result;
    }
    @GetMapping("/browse/{namespace}/{id}")
    public String browseByNamespaceAndId(@PathVariable("namespace") Integer namespace, @PathVariable("id") String id) {
        String result = opcService.browseOneDepth(namespace, id);
        return result;
    }

    @GetMapping("/browse/{namespace}/{type}/{id}")
    public String browseByNamespaceAndId(@PathVariable("namespace") Integer namespace, @PathVariable("type") String type, @PathVariable("id") String id) {
        String result = opcService.browseOneDepth(namespace, type, id);
        return result;
    }

    // 웹소켓을 열어서 polling 대상이 있는 경우 polling 값을 전달함
    @GetMapping("/traverse/{id}")
    public String traverseById(@PathVariable("id") String id) {
        String result = opcService.browseLeaves(0, "i", id);

        return result;
    }

    @GetMapping("/traverse/{namespace}/{type}/{id}")
    public String traverse(@PathVariable("namespace") Integer namespace, @PathVariable("type") String type, @PathVariable("id") String id) {
        String result = opcService.browseLeaves(namespace, type, id);

        return result;
    }

    @GetMapping("/poll")
    public String poll() {
        String result = opcService.pollLeaves();

        return result;
    }

    @GetMapping("/connected")
    public String isConnected() {
        return opcService.isConnected();
    }

    @PostMapping("/init")
    public Map<String,Boolean> postMethodName(@RequestBody UrlDto url) {
        String host = url.getUrl();
        boolean result = opcService.connect(host);
        Map<String,Boolean> msg = Map.of("result", result);
        return msg;
    }

    @GetMapping("/travel")
    public Map<String,Map<String,Object>> travel() {
        Map<String,Map<String,Object>> result = null;
        return result;
    }
}
