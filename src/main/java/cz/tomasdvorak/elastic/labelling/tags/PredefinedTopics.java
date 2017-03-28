package cz.tomasdvorak.elastic.labelling.tags;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class PredefinedTopics {

    @Value("classpath:/predefined-topics.txt")
    private Resource inputFile;

    public Collection<String> getTopics() throws IOException {
        final Path path = Paths.get(inputFile.getURI());
        return Files.lines(path)
                .filter(l -> !l.startsWith("#"))
                .collect(Collectors.toList());
    }
}
