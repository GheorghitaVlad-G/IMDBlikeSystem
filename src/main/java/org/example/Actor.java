import utils.Genre;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class Actor implements Comparable{
    private String actorName;
    private Map<String, String> performances = new TreeMap<>();
    private String biography;
    public Actor(){
        this.actorName = "";
        this.biography = "";
    }
    public Actor(String n){
        this.actorName = n;
        this.biography = "No biography set";
    }
    public Actor(String n, String bio){
        this.actorName = n;
        this.biography = bio;
    }
    public String getActorName(){
        return this.actorName;
    }
    public String getBiography(){
        return this.biography;
    }
    public Map<String, String> getPerformances(){
        return this.performances;
    }
    public void setActorName(String newName){
        this.actorName = newName;
    }
    public void setBiography(String newBio){
        this.biography = newBio;
    }
    public void addPerformance(String name, String perf){
        this.performances.put(name, perf);
    }
    public void removePerformance(String name, String perf){
        this.performances.remove(name, perf);
    }
    public void removePerformance(String name){
        this.performances.remove(name);
    }
    public String toString(){
        String str = "";
        str += this.actorName;
        return str;
    }
    public void displayInfo() {
        System.out.println(this.getActorName().toUpperCase());
        System.out.print("Known for: ");

        int entryCount = this.performances.size();
        int currentEntry = 0;

        for (Map.Entry<String, String> entry: this.performances.entrySet()){
            System.out.print("\"" + entry.getKey() + "\"");
            if (++currentEntry < entryCount) {
                System.out.print(", ");
            }
        }
        System.out.println();
        System.out.println("Biography: " + this.biography);
        System.out.println();
    }
    @Override
    public int compareTo(Object o) {
        Actor actor = (Actor) o;
        return this.actorName.compareTo(actor.actorName);
    }
}
