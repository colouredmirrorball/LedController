package be.cmbsoft.ledcontrol.output;

public record ArtNetOutput(String ip, int port, int subnet, int universe, int x, int y, int width, int height) {

}
