package be.cmbsoft.ledcontrol.output;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists and loads the list of {@link LedStrip} objects as a JSON file.
 */
public class LedStripConfig {

    private final File configFile;
    private List<LedStrip> strips;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    public LedStripConfig(File configFile) {
        this.configFile = configFile;
    }

    /**
     * Load strips from disk.  Returns an empty list when the file does not exist.
     */
    private List<LedStrip> load() {
        if (!configFile.exists()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(configFile, new TypeReference<>() {
            });
        } catch (IOException e) {
            System.err.println("Could not read strip config from " + configFile + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public LedStripConfig reload() {
        this.strips = load();
        for (LedStrip strip : strips) {
            strip.updatePixelPositions();
        }
        return this;
    }

    /**
     * Persist the current list of strips to disk.
     */
    public void save() {
        try {
            configFile.getParentFile().mkdirs();
            MAPPER.writeValue(configFile, strips);
        } catch (IOException e) {
            System.err.println("Could not save strip config to " + configFile + ": " + e.getMessage());
        }
    }

    public List<LedStrip> getStrips() {
        return strips;
    }
}

