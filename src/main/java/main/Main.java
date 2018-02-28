
package main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import spark.ModelAndView;
import spark.Spark;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

public class Main {
    
    public static void main(String[] args) {
        if(System.getenv("PORT") != null) {
            Spark.port(Integer.valueOf(System.getenv("PORT")));
        }
        
        Spark.get("/", (req, res) -> {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:drinkkiarkisto.db");
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Drinkki");
            ResultSet rs = stmt.executeQuery();
            ArrayList<Drinkki> drinks = new ArrayList();
            while(rs.next()) {
                drinks.add(new Drinkki(rs.getInt("id"), rs.getString("nimi")));
            }
            conn.close();
            HashMap map = new HashMap();
            map.put("drinkit", drinks);
            
            return new ModelAndView(map, "index");
        }, new ThymeleafTemplateEngine());
        
        Spark.get("/drinkit", (req, res) -> {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:drinkkiarkisto.db");
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Drinkki");
            ResultSet rs = stmt.executeQuery();
            ArrayList<Drinkki> drinks = new ArrayList();
            while(rs.next()) {
                drinks.add(new Drinkki(rs.getInt("id"), rs.getString("nimi")));
            }
            
            stmt = conn.prepareStatement("SELECT * FROM Mixer");
            rs = stmt.executeQuery();
            ArrayList<Mixer> mixerit = new ArrayList();
            while(rs.next()) {
                mixerit.add(new Mixer(rs.getInt("id"), rs.getString("nimi")));
            }
            conn.close();
            HashMap map = new HashMap();
            map.put("drinkit", drinks);
            map.put("mixerit", mixerit);

            
            return new ModelAndView(map, "drinkit");
        }, new ThymeleafTemplateEngine());
        
                
        Spark.get("/drinkit/:id", (req, res) -> {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:drinkkiarkisto.db");
            int drink_id = Integer.parseInt(req.params(":id"));
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM DrinkkiMixer AS dm JOIN mixer AS m ON dm.mixer_id = m.id WHERE dm.drinkki_id="+drink_id+" ORDER BY dm.jarjestys");
            ResultSet rs = stmt.executeQuery();
            ArrayList<String> mixerit = new ArrayList();
            while(rs.next()) {
                if(rs.getString("lisaohje")!= null && !rs.getString("lisaohje").isEmpty()) {
                    mixerit.add(rs.getString("nimi")+", "+rs.getString("maara")+" ("+rs.getString("lisaohje")+")");   
                } else {
                    mixerit.add(rs.getString("nimi")+", "+rs.getString("maara"));   
                }
            }
            
            HashMap map = new HashMap();
            map.put("nimi", conn.prepareStatement("SELECT * FROM Drinkki WHERE id="+drink_id).executeQuery().getString("nimi"));
            map.put("mixerit", mixerit);

            conn.close();
            return new ModelAndView(map, "ohje");
        }, new ThymeleafTemplateEngine());
        
        Spark.get("/drinkit/:id/poista", (req, res) -> {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:drinkkiarkisto.db");
            int drink_id = Integer.parseInt(req.params(":id"));
            
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM DrinkkiMixer WHERE drinkki_id="+drink_id);
            stmt.executeUpdate();
            
            stmt = conn.prepareStatement("DELETE FROM Drinkki WHERE id="+drink_id);
            stmt.executeUpdate();

            conn.close();
            res.redirect("/drinkit");
            return "";
        });
        
        Spark.post("/drinkit", (req, res) -> {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:drinkkiarkisto.db");
            if(req.queryParams("Drinkki") != null && !req.queryParams("Drinkki").isEmpty()) {
                PreparedStatement stmt = conn.prepareStatement("INSERT INTO Drinkki (id, nimi) VALUES (?, ?)");
                stmt.setString(2, req.queryParams("Drinkki"));
                stmt.executeUpdate();
            }
            
            if(req.queryParams("maara") != null && req.queryParams("jarj") != null &&!req.queryParams("jarj").isEmpty() && !req.queryParams("maara").isEmpty()) {
                int mixer_id = conn.prepareStatement("SELECT id FROM Mixer WHERE nimi='"+req.queryParams("sisalto")+"'").executeQuery().getInt("id");
                int drinkki_id = conn.prepareStatement("SELECT id FROM Drinkki WHERE nimi='"+req.queryParams("drinkki")+"'").executeQuery().getInt("id");
                
                ResultSet resSet = conn.prepareStatement("SELECT * FROM DrinkkiMixer WHERE mixer_id="+mixer_id+" AND drinkki_id="+drinkki_id).executeQuery();
                String maara = "", ohje = "";
                int jarj = -1;
                while(resSet.next()) {
                    maara = resSet.getString("maara");
                    jarj = resSet.getInt("jarjestys");
                    ohje = resSet.getString("lisaohje");
                }
                boolean onJoOhjeet = !ohje.isEmpty();
                
                
                if(!maara.isEmpty()) {
                    PreparedStatement stmt = conn.prepareStatement("UPDATE DrinkkiMixer SET maara='"+req.queryParams("maara")+"' WHERE drinkki_id="+drinkki_id+" AND mixer_id="+mixer_id);
                    stmt.executeUpdate();
                }
                
                if(jarj != Integer.parseInt(req.queryParams("jarj"))) {
                    PreparedStatement stmt = conn.prepareStatement("UPDATE DrinkkiMixer SET jarjestys="+req.queryParams("jarj")+" WHERE drinkki_id="+drinkki_id+" AND mixer_id="+mixer_id);
                    stmt.executeUpdate();
                }
                
                if (onJoOhjeet) {
                    ohje = ohje + " & ";
                    PreparedStatement stmt = conn.prepareStatement("UPDATE DrinkkiMixer SET lisaohje='"+ohje+req.queryParams("ohje")+"' WHERE drinkki_id="+drinkki_id+" AND mixer_id="+mixer_id);
                    stmt.executeUpdate();
                }
                
                if(maara.isEmpty() && !onJoOhjeet) {
                    PreparedStatement stmt = conn.prepareStatement("INSERT INTO DrinkkiMixer (drinkki_id, mixer_id, maara, jarjestys, lisaohje) VALUES (?, ?, ?, ?, ?)");
                    stmt.setInt(1, drinkki_id);
                    stmt.setInt(2, mixer_id);
                    stmt.setString(3, req.queryParams("maara"));
                    stmt.setString(4, req.queryParams("jarj"));
                    stmt.setString(5, req.queryParams("ohje"));
                    stmt.executeUpdate();
                }
            }
            conn.close();
            res.redirect("/drinkit");
            return "";
        });
        
        Spark.get("/juomat", (req, res) -> {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:drinkkiarkisto.db");
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Mixer");
            ResultSet rs = stmt.executeQuery();
            ArrayList<Mixer> mixerit = new ArrayList();
            while(rs.next()) {
                mixerit.add(new Mixer(rs.getInt("id"), rs.getString("nimi")));
            }
            conn.close();
            HashMap map = new HashMap();
            map.put("mixerit", mixerit);

            
            return new ModelAndView(map, "juomat");
        }, new ThymeleafTemplateEngine());
        
        Spark.get("/juomat/:id", (req, res) -> {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:drinkkiarkisto.db");
            int mixer_id = Integer.parseInt(req.params(":id"));
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Drinkki AS d JOIN DrinkkiMixer AS dm ON d.id = dm.drinkki_id WHERE dm.mixer_id="+mixer_id);
            ResultSet rs = stmt.executeQuery();
            ArrayList<Drinkki> drinkit = new ArrayList();
            while(rs.next()) {
                drinkit.add(new Drinkki(rs.getInt("id"), rs.getString("nimi")));
            }
            
            HashMap map = new HashMap();
            map.put("nimi", conn.prepareStatement("SELECT * FROM Mixer WHERE id="+mixer_id).executeQuery().getString("nimi"));
            map.put("drinkit", drinkit);

            conn.close();
            return new ModelAndView(map, "mixerlista");
        }, new ThymeleafTemplateEngine());
                
        Spark.get("/juomat/:id/poista", (req, res) -> {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:drinkkiarkisto.db");
            int mixer_id = Integer.parseInt(req.params(":id"));
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM DrinkkiMixer WHERE mixer_id="+mixer_id);
            stmt.executeUpdate();
            
            stmt = conn.prepareStatement("DELETE FROM Mixer WHERE id="+mixer_id);
            stmt.executeUpdate();

            conn.close();
            res.redirect("/juomat");
            return "";
        });
                
        Spark.post("/juomat", (req, res) -> {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:drinkkiarkisto.db");
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO Mixer (id, nimi) VALUES (?, ?)");
            stmt.setString(2, req.queryParams("blandis"));
            stmt.executeUpdate();
            
            conn.close();
            res.redirect("/juomat");
            return "";
        });
    }
}
