
package main;

public class Drinkki {
    String nimi;
    int id;
    
    public Drinkki(int id, String nimi) {
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
