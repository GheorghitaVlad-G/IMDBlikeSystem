import utils.Genre;

import java.security.PublicKey;
import java.util.*;

public abstract class Production implements Comparable {
    private String title, plot;
    private Double averageRating;
    private List<String> directors = new ArrayList<String>();
    private List<String> actors = new ArrayList<String>();
    private List<Genre> genres = new ArrayList<Genre>();
    private List<Rating> ratings = new ArrayList<Rating>();
    private Map<Regular, Boolean> hasBeenReviewedBy = new TreeMap<>();
    public Production (String title, String plot){
        this.title = title;
        this.plot = plot;
    }
    public Production(String title, String plot, Double averageRating){
        this.title = title;
        this.plot = plot;
        this.averageRating = averageRating;
    }
    public void addDirector(String name){
        this.directors.add(name);
    }
    public void removeDirector(String name){
        this.directors.remove(name);
    }
    public void addActor(String name){
        this.actors.add(name);
    }
    public void addGenre(String genre){
        try {
            this.genres.add(Genre.valueOf(genre));
        }
        catch (IllegalArgumentException e){
            System.out.println("Something went wrong adding the genre!");
        }
    }
    public void removeGenre(String name){
        try {
            this.genres.remove(Genre.valueOf(name));
            System.out.println("Genre removed successfully!");
        }
        catch (IllegalArgumentException e){
            System.out.println("Something went wrong removing the genre!");
        }
    }
    public void calculateAverageRating(){
        int sum = 0;
        if (!ratings.isEmpty()) {
            for (Rating rat : ratings) {
                sum += rat.getRating();
            }
        }
        this.averageRating = (double) sum;
        if (!ratings.isEmpty()) {
            this.averageRating /= (double)ratings.size();
        }
    }
    public void addRating(Rating rating){
        this.ratings.add(rating);
        calculateAverageRating();
    }
    public void removeRating(Rating rating){
        this.ratings.remove(rating);
        calculateAverageRating();
    }
    public void setPlot(String plot){
        this.plot = plot;
    }
    public String getTitle(){
        return this.title;
    }
    public String getPlot(){
        return this.plot;
    }
    public List<Rating> getRatings(){
        return this.ratings;
    }
    public List<Genre> getGenres(){
        return this.genres;
    }
    public List<String> getDirectors(){
        return this.directors;
    }
    public List<String> getActors(){
        return this.actors;
    }
    public Double getAverageRating(){
        return this.averageRating;
    }
    public Map<Regular, Boolean> getHasBeenReviewedBy(){
        return this.hasBeenReviewedBy;
    }
    public void addRegularsToReviews(Regular regular){
        this.hasBeenReviewedBy.put(regular, false);
    }
    private static int getUserExperience(String username){
        User user = null;
        IMDB instance  = IMDB.getInstance();
        for (User user1 : instance.getRegularList()){
            if (user1.getUsername().equals(username)){
                user = user1;
                break;
            }
        }
        for (User user1 : instance.getContributorList()){
            if (user1.getUsername().equals(username)){
                user = user1;
                break;
            }
        }
        for (User user1 : instance.getAdminList()){
            if (user1.getUsername().equals(username)){
                user = user1;
                break;
            }
        }
        return user.getExp();
    }
    public void sortRatings(){
        this.ratings.sort(Comparator.comparingInt(rating -> getUserExperience(rating.getUser())));
        this.ratings = this.ratings.reversed();
    }
    public abstract void displayInfo();
    public void displayRatings(){
        for (Rating rating : this.ratings){
            System.out.println(rating.getUser() + ": " + rating.getRating());
            System.out.println(rating.getComment());
            System.out.println();
        }
    }
    public int compareTo(Object o){
        if (o instanceof Production){
            Production prod = (Production) o;
            return this.title.compareTo(prod.title);
        }
        return -1;
    }
    public String toString(){
        String str = "";
        str += this.title;// + "\n" + this.plot + "\n" + this.averageRating + "\n" + directors.toString() + "\n" + actors.toString() + "\n" + genres.toString() + "\n" + ratings + "\n";
        return str;
    }
}
