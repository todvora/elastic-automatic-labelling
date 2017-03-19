package cz.tomasdvorak.elastic.labelling.controllers;

import cz.tomasdvorak.elastic.labelling.api.Wikipedia;
import cz.tomasdvorak.elastic.labelling.api.dto.WikiPage;
import cz.tomasdvorak.elastic.labelling.elastic.ElasticPrecolateIndex;
import cz.tomasdvorak.elastic.labelling.tags.TedTopics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

@Controller
public class WebController {

    @Autowired
    private ElasticPrecolateIndex elastic;

    @Autowired
    private Wikipedia wikipedia;

    @Autowired
    private TedTopics tedTopics;

    @RequestMapping("/")
    public String index() {
        return "index";
    }

    @RequestMapping(value = "/elastic", method = RequestMethod.POST)
    public String createIndex(@RequestParam(name = "tags") String tags, @RequestParam(name = "drop-create", required = false) boolean dropCreate, Model model) throws IOException {
        if(dropCreate) {
            elastic.dropCreate();
        }
        elastic.addTags(Arrays.asList(tags.split(",")));
        return "redirect:/elastic";
    }

    @RequestMapping(value = "/elastic-ted", method = RequestMethod.POST)
    public String tedTopics() throws IOException {
        final Collection<String> topics = tedTopics.getTopics();
        elastic.dropCreate();
        elastic.addTags(topics);
        return "redirect:/elastic";
    }

    @RequestMapping(value = "/elastic")
    private String elastic(final Model model) throws IOException {
        model.addAttribute("tags", elastic.getTags());
        return "elastic";
    }


    @RequestMapping(value = "/tester", method = RequestMethod.POST)
    public String discoverTags(@RequestParam(name = "text") String text, Model model) throws IOException {
        model.addAttribute("result", elastic.discoverTags(text));
        return "tester";
    }

    @RequestMapping(value = "/tester")
    public String tester() throws IOException {
        return "tester";
    }

    @RequestMapping(value = "/tester-random")
    public String testerRandom(Model model) throws IOException {
        final WikiPage wikiPage = wikipedia.getRandomPage();
        model.addAttribute("result", elastic.discoverTags(wikiPage.getExtract()));
        model.addAttribute("source", wikiPage);
        return "tester";
    }
}