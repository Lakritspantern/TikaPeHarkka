
package main;

public class Mixer {
    String nimi;
    int id;
    
    public Mixer(int id, String nimi) {
        this.nimi = nimi;
        this.id = id;
    }
    
    @Override
    public String toString() {
        return nimi;
    }

    public String getNimi() {
        return nimi;
    }

    public int getId() {
        return id;
    }
}
