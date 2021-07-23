package com.marklogic.hub.central.controllers;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SinglePageAppController implements ErrorController {

    @RequestMapping(value = {"/"})
    public String index() {
        return "forward:index.html";
    }

}
