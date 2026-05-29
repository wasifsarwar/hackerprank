package com.hackerprank.editor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/editor/java-lsp")
public class JavaLspController {
    private final JavaLspService javaLspService;

    public JavaLspController(JavaLspService javaLspService) {
        this.javaLspService = javaLspService;
    }

    @GetMapping("/status")
    public JavaCompletionResponse status() {
        return javaLspService.status();
    }

    @PostMapping("/completion")
    public JavaCompletionResponse completion(@RequestBody JavaCompletionRequest request) {
        return javaLspService.complete(request);
    }
}
