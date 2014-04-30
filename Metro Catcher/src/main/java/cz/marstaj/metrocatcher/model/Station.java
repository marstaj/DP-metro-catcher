package cz.marstaj.metrocatcher.model;

/**
 * Created by mastajner on 05/04/14.
 */
public class Station {

    private int id;
    private String name;
    private String[] poct1string;
    private String[] pa1string;
    private String[] so1string;
    private String[] ne1string;
    private String[] poct2string;
    private String[] pa2string;
    private String[] so2string;
    private String[] ne2string;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getPoct1string() {
        return poct1string;
    }

    public void setPoct1string(String[] poct1string) {
        this.poct1string = poct1string;
    }

    public String[] getPa1string() {
        return pa1string;
    }

    public void setPa1string(String[] pa1string) {
        this.pa1string = pa1string;
    }

    public String[] getSo1string() {
        return so1string;
    }

    public void setSo1string(String[] so1string) {
        this.so1string = so1string;
    }

    public String[] getNe1string() {
        return ne1string;
    }

    public void setNe1string(String[] ne1string) {
        this.ne1string = ne1string;
    }

    public String[] getPoct2string() {
        return poct2string;
    }

    public void setPoct2string(String[] poct2string) {
        this.poct2string = poct2string;
    }

    public String[] getPa2string() {
        return pa2string;
    }

    public void setPa2string(String[] pa2string) {
        this.pa2string = pa2string;
    }

    public String[] getSo2string() {
        return so2string;
    }

    public void setSo2string(String[] so2string) {
        this.so2string = so2string;
    }

    public String[] getNe2string() {
        return ne2string;
    }

    public void setNe2string(String[] ne2string) {
        this.ne2string = ne2string;
    }
}
